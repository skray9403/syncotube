/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import com.metrolist.shared.deviceconnect.DcLocalPlaybackState
import com.metrolist.shared.deviceconnect.DcPlaybackAction
import com.metrolist.shared.deviceconnect.DcPlaybackCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

private data class RemoteControllerEndpoint(
    val deviceId: String,
    val name: String,
    val host: String,
    val port: Int,
)

class NectarCastController(
    private val scope: CoroutineScope,
    private val identity: NectarDeviceIdentity,
    private val transport: NectarTransportBridge,
) {
    private val transferSeq = AtomicLong(1L)
    private val stateVersion = AtomicLong(1L)

    private val _sessionState = MutableStateFlow(NectarSessionUiState())
    val sessionState: StateFlow<NectarSessionUiState> = _sessionState.asStateFlow()

    // Controller role state (Phone A controlling Phone B)
    private var remoteShadowState: DcLocalPlaybackState? = null
    private var lastShadowSyncUnixMs: Long = 0L

    // Receiver role state (Phone B playing for Phone A)
    private var activeController: RemoteControllerEndpoint? = null
    private var lastAppliedControllerVersion: Long = 0L
    private var stateSyncJob: Job? = null

    init {
        transport.setIncomingHandler(::handleIncoming)
        NectarCastRegistry.registerController(this)
    }

    fun onPlaybackPortBound() {
        transport.startServer()
    }

    fun onPlaybackPortUnbound() {
        stopStateSyncLoop()
        activeController = null
        transport.stopServer()
    }

    fun refreshAdvertising() {
        transport.refreshAdvertising()
    }

    fun handoffTo(peer: NectarPeer) {
        val port = NectarCastRegistry.port ?: run {
            _sessionState.update { it.copy(lastError = "Playback service not ready") }
            return
        }
        scope.launch {
            _sessionState.update { it.copy(transferringDeviceId = peer.deviceId, lastError = null) }
            val snap = runCatching { port.snapshot() }.getOrElse { err ->
                _sessionState.update { it.copy(transferringDeviceId = null, lastError = err.message) }
                return@launch
            }
            val currentSong = snap.queue.displayedItems.getOrNull(snap.queue.currentIndex)
            if (currentSong == null || currentSong.isLocal || currentSong.id.startsWith("local_")) {
                _sessionState.update {
                    it.copy(transferringDeviceId = null, lastError = "Cannot hand off local or empty track")
                }
                return@launch
            }

            val tracks = NectarSnapshotBridge.extractQueueTracks(snap)
            val mappedIndex = tracks.indexOfFirst { it.videoId == currentSong.id }.coerceAtLeast(0)
            val version = stateVersion.incrementAndGet()

            val payload = NectarHandoffPayload(
                transferId = transferSeq.getAndIncrement(),
                stateVersion = version,
                sentAtUnixMs = System.currentTimeMillis(),
                controllerDeviceId = identity.deviceId,
                controllerName = identity.deviceName,
                controllerPort = transport.localListenPort ?: 0,
                activeVideoId = currentSong.id,
                positionMs = snap.positionMs,
                isPlaying = snap.playing,
                volume = snap.volume,
                repeatMode = NectarSnapshotBridge.repeatModeToInt(snap),
                queueTitle = snap.title,
                queue = tracks,
                queueIndex = mappedIndex,
            )

            transport.sendHandoff(peer, payload)
                .onSuccess {
                    stopStateSyncLoop()
                    activeController = null
                    remoteShadowState = snap
                    lastShadowSyncUnixMs = System.currentTimeMillis()
                    port.setPlaying(false)
                    port.setAudioOutputEnabled(false)
                    port.setExternalReportingEnabled(false, null)
                    port.setControllerPlayback(snap, provisional = false)
                    _sessionState.value = NectarSessionUiState(activePeer = peer)
                }
                .onFailure { err ->
                    _sessionState.update {
                        it.copy(transferringDeviceId = null, lastError = err.message ?: "Handoff failed")
                    }
                }
        }
    }

    fun returnToThisPhone() {
        val active = _sessionState.value.activePeer ?: return
        val port = NectarCastRegistry.port
        scope.launch {
            val ack = transport.sendRelease(active, identity.deviceId).getOrNull()
            val shadow = remoteShadowState
            val finalSync = ack?.state
            remoteShadowState = null
            _sessionState.value = NectarSessionUiState()

            if (port == null) return@launch

            val targetPos = when {
                finalSync != null -> {
                    val elapsed = if (finalSync.isPlaying) {
                        (System.currentTimeMillis() - finalSync.updatedAtUnixMs).coerceAtLeast(0L)
                    } else 0L
                    finalSync.positionMs + elapsed
                }
                shadow != null -> {
                    val elapsed = if (shadow.playing) {
                        (System.currentTimeMillis() - lastShadowSyncUnixMs).coerceAtLeast(0L)
                    } else 0L
                    shadow.positionMs + elapsed
                }
                else -> 0L
            }
            val shouldPlay = finalSync?.isPlaying ?: shadow?.playing ?: false

            port.setControllerPlayback(null, provisional = false)
            port.setAudioOutputEnabled(true)
            port.setExternalReportingEnabled(true, null)

            if (shadow != null && finalSync != null) {
                val updated = NectarSnapshotBridge.applyRemoteStateToShadow(shadow, finalSync)
                val trackChanged = updated.queue.currentIndex != shadow.queue.currentIndex
                if (trackChanged) {
                    val returnPayload = NectarHandoffPayload(
                        transferId = transferSeq.getAndIncrement(),
                        stateVersion = stateVersion.incrementAndGet(),
                        sentAtUnixMs = System.currentTimeMillis(),
                        controllerDeviceId = identity.deviceId,
                        controllerName = identity.deviceName,
                        controllerPort = 0,
                        activeVideoId = finalSync.activeVideoId
                            ?: updated.queue.displayedItems.getOrNull(updated.queue.currentIndex)?.id.orEmpty(),
                        positionMs = targetPos,
                        isPlaying = shouldPlay,
                        volume = updated.volume,
                        repeatMode = NectarSnapshotBridge.repeatModeToInt(updated),
                        queueTitle = updated.title,
                        queue = NectarSnapshotBridge.extractQueueTracks(updated),
                        queueIndex = updated.queue.currentIndex,
                    )
                    runCatching {
                        port.apply(
                            NectarSnapshotBridge.buildIncomingSnapshot(returnPayload),
                            targetPos,
                        ) { true }
                    }
                    return@launch
                }
            }

            port.seekTo(targetPos)
            port.setPlaying(shouldPlay)
        }
    }

    fun interceptPlaybackCommand(command: DcPlaybackCommand): Boolean {
        val active = _sessionState.value.activePeer ?: return false
        val wire = NectarSnapshotBridge.mapOutboundCommand(command)
        val version = stateVersion.incrementAndGet()

        scope.launch {
            val ack = transport.sendCommand(active, identity.deviceId, version, wire).getOrNull()
            val port = NectarCastRegistry.port ?: return@launch
            val current = remoteShadowState ?: return@launch

            val updated = when {
                ack?.state != null -> NectarSnapshotBridge.applyRemoteStateToShadow(current, ack.state)
                command.action == DcPlaybackAction.PLAY -> current.copy(playing = true)
                command.action == DcPlaybackAction.PAUSE -> current.copy(playing = false)
                wire.positionMs != null -> current.copy(positionMs = wire.positionMs)
                wire.volume != null -> current.copy(volume = wire.volume)
                else -> current
            }

            remoteShadowState = updated
            lastShadowSyncUnixMs = System.currentTimeMillis()
            port.setControllerPlayback(updated, provisional = false)
        }
        return true
    }

    private suspend fun handleIncoming(
        remoteIp: String,
        message: IncomingNectarMessage,
    ): NectarAck = when (message) {
        is IncomingNectarMessage.Handoff -> handleIncomingHandoff(remoteIp, message.payload)
        is IncomingNectarMessage.Command -> handleIncomingCommand(message)
        is IncomingNectarMessage.StateSync -> handleIncomingStateSync(message.sync)
        is IncomingNectarMessage.Release -> handleIncomingRelease(message.senderDeviceId)
    }

    private fun handleIncomingHandoff(
        remoteIp: String,
        payload: NectarHandoffPayload,
    ): NectarAck {
        val port = NectarCastRegistry.port
            ?: return NectarAck(accepted = false, error = "Receiver playback service not bound")

        if (activeController?.deviceId == payload.controllerDeviceId &&
            payload.stateVersion < lastAppliedControllerVersion
        ) {
            return NectarAck(accepted = false, error = "Stale handoff version")
        }

        // Exit controller mode if this device was previously controlling another peer
        if (_sessionState.value.activePeer != null) {
            remoteShadowState = null
        }

        lastAppliedControllerVersion = payload.stateVersion
        activeController = if (remoteIp.isNotBlank() && payload.controllerPort > 0) {
            RemoteControllerEndpoint(
                deviceId = payload.controllerDeviceId,
                name = payload.controllerName,
                host = remoteIp,
                port = payload.controllerPort,
            )
        } else {
            null
        }

        _sessionState.value = NectarSessionUiState(remoteControllingUs = payload.controllerName)

        // Resolve & start playback asynchronously so TCP ACK returns immediately without stalling sender UI
        scope.launch {
            port.setControllerPlayback(null, provisional = false)
            port.setAudioOutputEnabled(true)
            val snapshot = NectarSnapshotBridge.buildIncomingSnapshot(payload)
            val elapsed = if (payload.isPlaying) {
                (System.currentTimeMillis() - payload.sentAtUnixMs).coerceAtLeast(0L)
            } else 0L
            val targetPos = (payload.positionMs + elapsed).coerceAtLeast(0L)

            val applyResult = runCatching {
                port.apply(snapshot, targetPos) { true }
            }

            if (applyResult.isSuccess) {
                pushCurrentStateToController()
                startStateSyncLoop()
            } else {
                val errMsg = applyResult.exceptionOrNull()?.message ?: "Failed to resolve track"
                activeController?.let { ctrl ->
                    transport.pushStateSync(
                        ctrl.host,
                        ctrl.port,
                        NectarStateSync(
                            stateVersion = stateVersion.incrementAndGet(),
                            ownerDeviceId = identity.deviceId,
                            updatedAtUnixMs = System.currentTimeMillis(),
                            activeVideoId = payload.activeVideoId,
                            positionMs = payload.positionMs,
                            durationMs = 0L,
                            isPlaying = false,
                            volume = payload.volume,
                            queueIndex = payload.queueIndex,
                            error = errMsg,
                        ),
                    )
                }
            }
        }

        return NectarAck(accepted = true)
    }

    private suspend fun handleIncomingCommand(cmd: IncomingNectarMessage.Command): NectarAck {
        val port = NectarCastRegistry.port
            ?: return NectarAck(accepted = false, error = "Playback service not bound")
        if (activeController?.deviceId == cmd.senderDeviceId &&
            cmd.stateVersion < lastAppliedControllerVersion
        ) {
            return NectarAck(accepted = false, error = "Stale command")
        }
        lastAppliedControllerVersion = cmd.stateVersion

        runCatching {
            NectarSnapshotBridge.applyIncomingCommand(port, cmd.wire, cmd.stateVersion)
        }.onFailure { err ->
            return NectarAck(accepted = false, error = err.message ?: "Command failed")
        }

        val fresh = runCatching { port.snapshot() }.getOrNull()?.let { snap ->
            NectarSnapshotBridge.toStateSync(identity.deviceId, stateVersion.incrementAndGet(), snap)
        }
        return NectarAck(accepted = true, state = fresh)
    }

    private suspend fun handleIncomingRelease(senderDeviceId: String): NectarAck {
        val port = NectarCastRegistry.port
        val finalSync = port?.let { p ->
            runCatching { p.snapshot() }.getOrNull()?.let { snap ->
                NectarSnapshotBridge.toStateSync(identity.deviceId, stateVersion.incrementAndGet(), snap)
            }
        }
        if (activeController == null || activeController?.deviceId == senderDeviceId) {
            stopStateSyncLoop()
            activeController = null
            _sessionState.update { it.copy(remoteControllingUs = null) }
            runCatching { port?.setPlaying(false) }
        }
        return NectarAck(accepted = true, state = finalSync)
    }

    private suspend fun handleIncomingStateSync(sync: NectarStateSync): NectarAck {
        val active = _sessionState.value.activePeer
        if (active == null || active.deviceId != sync.ownerDeviceId) {
            return NectarAck(accepted = true)
        }
        if (!sync.error.isNullOrBlank()) {
            _sessionState.update { it.copy(lastError = sync.error) }
        }
        val port = NectarCastRegistry.port ?: return NectarAck(accepted = true)
        val current = remoteShadowState ?: return NectarAck(accepted = true)
        val updated = NectarSnapshotBridge.applyRemoteStateToShadow(current, sync)
        remoteShadowState = updated
        lastShadowSyncUnixMs = System.currentTimeMillis()
        port.setControllerPlayback(updated, provisional = false)
        return NectarAck(accepted = true)
    }

    private fun startStateSyncLoop() {
        stateSyncJob?.cancel()
        stateSyncJob = scope.launch {
            var consecutiveFailures = 0
            while (isActive && activeController != null) {
                delay(3_000L)
                val ok = pushCurrentStateToController()
                if (ok) {
                    consecutiveFailures = 0
                } else if (++consecutiveFailures >= 3) {
                    activeController = null
                    _sessionState.update { it.copy(remoteControllingUs = null) }
                    break
                }
            }
        }
    }

    private fun stopStateSyncLoop() {
        stateSyncJob?.cancel()
        stateSyncJob = null
    }

    private suspend fun pushCurrentStateToController(): Boolean {
        val ctrl = activeController ?: return false
        val port = NectarCastRegistry.port ?: return false
        val snap = runCatching { port.snapshot() }.getOrNull() ?: return false
        val sync = NectarSnapshotBridge.toStateSync(
            ownerDeviceId = identity.deviceId,
            stateVersion = stateVersion.incrementAndGet(),
            state = snap,
        )
        return transport.pushStateSync(ctrl.host, ctrl.port, sync).isSuccess
    }
}
