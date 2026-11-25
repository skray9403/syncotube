/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.ext.SdkExtensions
import com.metrolist.shared.deviceconnect.DcLocalPlaybackState
import com.metrolist.shared.deviceconnect.DcPlaybackAction
import com.metrolist.shared.deviceconnect.DcPlaybackCommand
import com.metrolist.shared.deviceconnect.DcPlaybackSnapshot
import com.metrolist.shared.deviceconnect.DcRepeatMode
import com.metrolist.shared.deviceconnect.DeviceConnectPlaybackPort
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.playback.MusicQueueSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class NectarProtocol(
    val priority: Int,
    val label: String,
) {
    WIFI_QUIC(1, "Wi-Fi · QUIC"),
    BLE_HINT_QUIC(2, "Bluetooth → Wi-Fi"),
    NECTARLINK_QUIC(3, "NectarLink · QUIC"),
    BLE_WAKE(4, "Bluetooth"),
}

enum class NectarPlatform { ANDROID, WINDOWS, LINUX, TIZEN, UNKNOWN }

data class NectarRoute(
    val protocol: NectarProtocol,
    val host: String? = null,
    val port: Int? = null,
    val bleAddress: String? = null,
    val rttMs: Int? = null,
)

data class NectarPeer(
    val deviceId: String, // 32-char hex of 16-byte BLAKE3(Ed25519 pubkey)
    val displayName: String,
    val platform: NectarPlatform,
    val paired: Boolean,
    val routes: List<NectarRoute>,
) {
    val primaryRoute: NectarRoute get() = routes.first()
}

data class RawNectarEvent(
    val deviceId: String,
    val displayName: String,
    val platform: NectarPlatform,
    val paired: Boolean,
    val route: NectarRoute,
)

data class NectarHandoffPayload(
    val transferId: Long,
    val stateVersion: Long,
    val sentAtUnixMs: Long,
    val activeVideoId: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val volume: Float,
    val queueVideoIds: List<String>,
    val queueIndex: Int,
    val titleHint: String,
    val artistHint: String,
)

data class NectarSessionUiState(
    val activePeer: NectarPeer? = null,
    val transferringDeviceId: String? = null,
    val lastError: String? = null,
)

/** Replace LanNectarTransportBridge in AppModule once Rust cast-ffi (UniFFI) is built. */
interface NectarTransportBridge {
    val incoming: Flow<IncomingNectarMessage> get() = emptyFlow()

    fun startServer() = Unit

    fun stopServer() = Unit

    fun refreshAdvertising() = Unit

    fun observeNectarLinkPeers(): Flow<RawNectarEvent>

    suspend fun sendHandoff(
        peer: NectarPeer,
        payload: NectarHandoffPayload,
    ): Result<Unit>

    suspend fun sendCommand(
        peer: NectarPeer,
        command: DcPlaybackCommand,
    ): Result<Unit>

    suspend fun releaseRemote(peer: NectarPeer): Result<Unit>
}

class StubNectarTransportBridge : NectarTransportBridge {
    override fun observeNectarLinkPeers(): Flow<RawNectarEvent> = emptyFlow()

    override suspend fun sendHandoff(
        peer: NectarPeer,
        payload: NectarHandoffPayload,
    ): Result<Unit> =
        Result.failure(UnsupportedOperationException("Rust cast-ffi transport not linked yet"))

    override suspend fun sendCommand(
        peer: NectarPeer,
        command: DcPlaybackCommand,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun releaseRemote(peer: NectarPeer): Result<Unit> = Result.success(Unit)
}

/**
 * Lightweight static hook so AndroidDeviceConnect and AndroidDeviceConnectPlayback
 * stay decoupled from Koin and MusicService.kt needs zero edits.
 */
object NectarCastRegistry {
    @Volatile var port: DeviceConnectPlaybackPort? = null
        private set

    @Volatile var controller: NectarCastController? = null

    fun bindPort(target: DeviceConnectPlaybackPort) {
        port = target
        controller?.onPortBound()
    }

    fun unbindPort(target: DeviceConnectPlaybackPort) {
        if (port === target) {
            port = null
            controller?.onPortUnbound()
        }
    }

    fun intercept(command: DcPlaybackCommand): Boolean =
        controller?.interceptPlaybackCommand(command) == true
}

class NectarCastDiscovery(
    private val context: Context,
    private val localDeviceId: String,
    private val transport: NectarTransportBridge,
) {
    companion object {
        const val MDNS_SERVICE_TYPE = "_nectarcast._udp."
        val BLE_SERVICE_UUID: ParcelUuid =
            ParcelUuid(UUID.fromString("7e4a0001-9c2d-4f6b-a31e-5d8f0c2b9100"))
    }

    @OptIn(FlowPreview::class)
    fun scanAll(
        canUseLocalNetwork: Boolean,
        canScanBle: Boolean,
    ): Flow<List<NectarPeer>> {
        val peers = ConcurrentHashMap<String, NectarPeer>()
        val sources =
            buildList {
                if (canUseLocalNetwork) add(scanMdnsLan())
                if (canScanBle) add(scanBle())
                add(transport.observeNectarLinkPeers())
            }
        if (sources.isEmpty()) return flowOf(emptyList())

        return merge(*sources.toTypedArray())
            .filterNot { it.deviceId == localDeviceId }
            .map { ev ->
                peers.compute(ev.deviceId) { _, prev ->
                    if (prev == null) {
                        NectarPeer(
                            deviceId = ev.deviceId,
                            displayName = ev.displayName,
                            platform = ev.platform,
                            paired = ev.paired,
                            routes = listOf(ev.route),
                        )
                    } else {
                        val updatedRoutes =
                            (
                                prev.routes.filterNot {
                                    it.protocol == ev.route.protocol
                                } + ev.route
                            ).sortedBy { it.protocol.priority }

                        prev.copy(
                            displayName = ev.displayName.ifBlank { prev.displayName },
                            platform = if (prev.platform == NectarPlatform.UNKNOWN) ev.platform else prev.platform,
                            paired = prev.paired || ev.paired,
                            routes = updatedRoutes,
                        )
                    }
                }
                peers.values.sortedWith(
                    compareBy<NectarPeer> { !it.paired }
                        .thenBy { it.primaryRoute.protocol.priority }
                        .thenBy { it.displayName.lowercase() },
                )
            }.debounce(200L)
    }

    private fun scanMdnsLan(): Flow<RawNectarEvent> =
        callbackFlow {
            val nsd = context.getSystemService(NsdManager::class.java)
            if (nsd == null) {
                close()
                return@callbackFlow
            }
            val mainHandler = Handler(Looper.getMainLooper())
            val multicastLock =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) < 7
                ) {
                    context.applicationContext
                        .getSystemService(WifiManager::class.java)
                        ?.createMulticastLock("nectarcast-mdns")
                        ?.apply {
                            setReferenceCounted(false)
                            runCatching { acquire() }
                        }
                } else {
                    null
                }

            val pending = ArrayDeque<NsdServiceInfo>()
            var resolving = false

            @Suppress("DEPRECATION")
            fun resolveNext() {
                if (resolving || pending.isEmpty()) return
                resolving = true
                val next = pending.removeFirst()
                runCatching {
                    nsd.resolveService(
                        next,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(
                                info: NsdServiceInfo,
                                errorCode: Int,
                            ) {
                                mainHandler.post {
                                    resolving = false
                                    resolveNext()
                                }
                            }

                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val attrs = info.attributes
                                val id = attrs["id"]?.decodeToString()
                                val host =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        info.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress
                                            ?: info.hostAddresses.firstOrNull()?.hostAddress
                                            ?: info.host?.hostAddress
                                    } else {
                                        info.host?.hostAddress
                                    }
                                if (!id.isNullOrBlank() && !host.isNullOrBlank()) {
                                    val name =
                                        attrs["name"]?.decodeToString()?.takeIf { it.isNotBlank() }
                                            ?: info.serviceName
                                    val platform =
                                        attrs["plat"]
                                            ?.decodeToString()
                                            ?.toIntOrNull()
                                            ?.let { NectarPlatform.entries.getOrNull(it) }
                                            ?: NectarPlatform.UNKNOWN
                                    trySend(
                                        RawNectarEvent(
                                            deviceId = id,
                                            displayName = name,
                                            platform = platform,
                                            paired = true,
                                            route =
                                                NectarRoute(
                                                    protocol = NectarProtocol.WIFI_QUIC,
                                                    host = host,
                                                    port = info.port,
                                                ),
                                        ),
                                    )
                                }
                                mainHandler.post {
                                    resolving = false
                                    resolveNext()
                                }
                            }
                        },
                    )
                }.onFailure {
                    resolving = false
                    resolveNext()
                }
            }

            val listener =
                object : NsdManager.DiscoveryListener {
                    override fun onDiscoveryStarted(serviceType: String) = Unit

                    override fun onDiscoveryStopped(serviceType: String) = Unit

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

                    override fun onStopDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) = Unit

                    override fun onStartDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) {
                        close()
                    }

                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        mainHandler.post {
                            pending.addLast(serviceInfo)
                            resolveNext()
                        }
                    }
                }

            runCatching {
                nsd.discoverServices(MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            }.onFailure { close(it) }

            awaitClose {
                runCatching { nsd.stopServiceDiscovery(listener) }
                if (multicastLock?.isHeld == true) multicastLock.release()
            }
        }

    @SuppressLint("MissingPermission")
    private fun scanBle(): Flow<RawNectarEvent> =
        callbackFlow {
            val scanner =
                (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
                    ?.adapter
                    ?.bluetoothLeScanner
            if (scanner == null) {
                close()
                return@callbackFlow
            }

            val callback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: ScanResult,
                    ) {
                        val data =
                            result.scanRecord?.getManufacturerSpecificData(
                                NectarCastAdvertiser.BLE_MANUFACTURER_ID,
                            ) ?: return
                        if (data.size < 20) return // [ver:1][dev_id:16][platform:1][port:2]

                        val deviceId = data.sliceArray(1..16).joinToString("") { "%02x".format(it) }
                        val platform =
                            NectarPlatform.entries.getOrNull(data[17].toInt() and 0xFF)
                                ?: NectarPlatform.UNKNOWN
                        val port = ((data[18].toInt() and 0xFF) shl 8) or (data[19].toInt() and 0xFF)
                        val name =
                            result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
                                ?: runCatching { result.device?.name }.getOrNull()?.takeIf { it.isNotBlank() }
                                ?: "Nearby Daemon"
                        trySend(
                            RawNectarEvent(
                                deviceId = deviceId,
                                displayName = name,
                                platform = platform,
                                paired = false,
                                route =
                                    NectarRoute(
                                        protocol = NectarProtocol.BLE_WAKE,
                                        port = port.takeIf { it > 0 },
                                        bleAddress = result.device?.address,
                                    ),
                            ),
                        )
                    }
                }

            val filter = ScanFilter.Builder().setServiceUuid(BLE_SERVICE_UUID).build()
            val settings =
                ScanSettings
                    .Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

            runCatching { scanner.startScan(listOf(filter), settings, callback) }
                .onFailure { close(it) }

            awaitClose { runCatching { scanner.stopScan(callback) } }
        }
}

class NectarCastController(
    private val scope: CoroutineScope,
    private val transport: NectarTransportBridge,
    private val ensurePort: suspend () -> DeviceConnectPlaybackPort? = { NectarCastRegistry.port },
) {
    private val transferSeq = AtomicLong(1L)
    private val stateVersion = AtomicLong(1L)

    private val _sessionState = MutableStateFlow(NectarSessionUiState())
    val sessionState: StateFlow<NectarSessionUiState> = _sessionState.asStateFlow()

    private var remoteShadowState: DcLocalPlaybackState? = null

    init {
        NectarCastRegistry.controller = this
        transport.startServer()
        scope.launch {
            transport.incoming.collect { msg ->
                handleIncoming(msg)
            }
        }
    }

    fun onPortBound() {
        transport.startServer()
    }

    fun onPortUnbound() = Unit

    fun refreshAdvertising() {
        transport.refreshAdvertising()
    }

    fun handoffTo(peer: NectarPeer) {
        val port =
            NectarCastRegistry.port ?: run {
                _sessionState.update { it.copy(lastError = "Playback service not bound yet") }
                return
            }
        scope.launch {
            _sessionState.update { it.copy(transferringDeviceId = peer.deviceId, lastError = null) }
            val snap =
                runCatching { port.snapshot() }.getOrElse { err ->
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

            val payload =
                NectarHandoffPayload(
                    transferId = transferSeq.getAndIncrement(),
                    stateVersion = stateVersion.incrementAndGet(),
                    sentAtUnixMs = System.currentTimeMillis(),
                    activeVideoId = currentSong.id,
                    positionMs = snap.positionMs,
                    isPlaying = snap.playing,
                    volume = snap.volume,
                    queueVideoIds = snap.queue.displayedItems.map { it.id },
                    queueIndex = snap.queue.currentIndex,
                    titleHint = currentSong.title,
                    artistHint = currentSong.artists.joinToString(", ") { it.name },
                )

            transport
                .sendHandoff(peer, payload)
                .onSuccess {
                    remoteShadowState = snap
                    // Put Metrolist into remote-controller mode using its own port
                    port.setPlaying(false)
                    port.setAudioOutputEnabled(false)
                    port.setExternalReportingEnabled(false, null)
                    port.setControllerPlayback(snap, provisional = false)
                    _sessionState.value = NectarSessionUiState(activePeer = peer)
                }.onFailure { err ->
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
            transport.releaseRemote(active)
            val shadow = remoteShadowState
            remoteShadowState = null
            _sessionState.value = NectarSessionUiState()
            if (port != null) {
                port.setControllerPlayback(null, provisional = false)
                port.setAudioOutputEnabled(true)
                port.setExternalReportingEnabled(true, null)
                if (shadow != null) {
                    val seq = stateVersion.incrementAndGet()
                    val snap =
                        DcPlaybackSnapshot(
                            sessionId = "nectar-return-$seq",
                            revision = seq,
                            queueRevision = seq,
                            trackRevision = seq,
                            playback = shadow,
                        )
                    runCatching { port.apply(snap, shadow.positionMs) { true } }
                }
            }
        }
    }

    fun interceptPlaybackCommand(command: DcPlaybackCommand): Boolean {
        val active = _sessionState.value.activePeer ?: return false
        scope.launch {
            transport.sendCommand(active, command)
            val current = remoteShadowState ?: return@launch
            val updated =
                when (command.action) {
                    DcPlaybackAction.PLAY -> current.copy(playing = true)
                    DcPlaybackAction.PAUSE -> current.copy(playing = false)
                    DcPlaybackAction.SEEK -> current.copy(positionMs = command.positionMs.coerceAtLeast(0L))
                    DcPlaybackAction.VOLUME -> command.volume?.let { current.copy(volume = it.coerceIn(0f, 1f)) } ?: current
                    DcPlaybackAction.SPEED -> current.copy(speed = command.speed)
                    DcPlaybackAction.NEXT -> {
                        val items = current.queue.displayedItems
                        if (items.isEmpty()) {
                            current
                        } else {
                            val nextIndex = (current.queue.currentIndex + 1).coerceAtMost(items.lastIndex)
                            current.copy(queue = current.queue.copy(currentIndex = nextIndex), positionMs = 0L)
                        }
                    }

                    DcPlaybackAction.PREVIOUS -> {
                        val nextIndex = (current.queue.currentIndex - 1).coerceAtLeast(0)
                        current.copy(queue = current.queue.copy(currentIndex = nextIndex), positionMs = 0L)
                    }

                    DcPlaybackAction.SELECT -> {
                        if (command.index in current.queue.displayedItems.indices) {
                            current.copy(queue = current.queue.copy(currentIndex = command.index), positionMs = 0L)
                        } else {
                            current
                        }
                    }

                    else -> current
                }
            if (updated != current) {
                remoteShadowState = updated
                NectarCastRegistry.port?.setControllerPlayback(updated, provisional = false)
            }
        }
        return true
    }

    private suspend fun handleIncoming(msg: IncomingNectarMessage) {
        when (msg) {
            is IncomingNectarMessage.Handoff -> handleIncomingHandoff(msg.payload)
            is IncomingNectarMessage.Command -> handleIncomingCommand(msg)
            IncomingNectarMessage.Release -> {
                NectarCastRegistry.port?.let { port ->
                    runCatching { port.setPlaying(false) }
                }
            }
        }
    }

    private suspend fun handleIncomingHandoff(payload: NectarHandoffPayload) {
        remoteShadowState = null
        _sessionState.value = NectarSessionUiState()

        val port = NectarCastRegistry.port ?: ensurePort() ?: return
        port.setControllerPlayback(null, provisional = false)
        port.setAudioOutputEnabled(true)
        port.setExternalReportingEnabled(true, null)

        val videoIds = payload.queueVideoIds.ifEmpty { listOf(payload.activeVideoId) }
        val songs =
            videoIds.map { id ->
                val isTarget = id == payload.activeVideoId
                SongItem(
                    id = id,
                    title = if (isTarget && payload.titleHint.isNotBlank()) payload.titleHint else id,
                    thumbnail = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                    artists =
                        if (isTarget && payload.artistHint.isNotBlank()) {
                            listOf(ArtistTiny(id = null, name = payload.artistHint))
                        } else {
                            emptyList()
                        },
                    album = null,
                )
            }
        val resolvedIndex =
            payload.queueIndex
                .takeIf { it in songs.indices }
                ?: songs.indexOfFirst { it.id == payload.activeVideoId }.takeIf { it >= 0 }
                ?: 0

        val queueSnapshot =
            MusicQueueSnapshot(
                displayedItems = songs,
                originalItems = songs,
                currentIndex = resolvedIndex,
                isShuffled = false,
                originalIndices = songs.indices.toList(),
            )
        val localState =
            DcLocalPlaybackState(
                queue = queueSnapshot,
                title = null,
                repeatMode = DcRepeatMode.OFF,
                playing = payload.isPlaying,
                positionMs = payload.positionMs,
                volume = payload.volume,
                finite = true,
                explicitRadioQueue = false,
            )
        val snapshot =
            DcPlaybackSnapshot(
                sessionId = "nectar-${payload.transferId}",
                revision = payload.stateVersion,
                queueRevision = payload.stateVersion,
                trackRevision = payload.transferId,
                playback = localState,
                anchorTimeMs = payload.sentAtUnixMs,
            )
        val elapsed =
            if (payload.isPlaying) {
                (System.currentTimeMillis() - payload.sentAtUnixMs).coerceIn(0L, 10_000L)
            } else {
                0L
            }
        val targetPos = (payload.positionMs + elapsed).coerceAtLeast(0L)

        runCatching {
            port.apply(snapshot, targetPos) { true }
            port.setVolume(payload.volume.coerceIn(0f, 1f))
        }.onFailure { err ->
            _sessionState.update {
                it.copy(lastError = err.message ?: "Failed to apply incoming handoff")
            }
        }
    }

    private suspend fun handleIncomingCommand(cmd: IncomingNectarMessage.Command) {
        val port = NectarCastRegistry.port ?: return
        val action = runCatching { DcPlaybackAction.valueOf(cmd.actionName) }.getOrNull() ?: return
        when (action) {
            DcPlaybackAction.PLAY -> port.setPlaying(true)
            DcPlaybackAction.PAUSE -> port.setPlaying(false)
            DcPlaybackAction.SEEK -> cmd.positionMs?.let { port.seekTo(it.coerceAtLeast(0L)) }
            DcPlaybackAction.VOLUME -> cmd.volume?.let { port.setVolume(it.coerceIn(0f, 1f)) }
            DcPlaybackAction.SPEED -> cmd.speed?.let { port.setSpeed(it) }
            DcPlaybackAction.NEXT,
            DcPlaybackAction.PREVIOUS,
            DcPlaybackAction.SELECT -> {
                val current = runCatching { port.snapshot() }.getOrNull() ?: return
                val items = current.queue.displayedItems
                if (items.isEmpty()) return
                val nextIndex =
                    when (action) {
                        DcPlaybackAction.NEXT -> (current.queue.currentIndex + 1).coerceAtMost(items.lastIndex)
                        DcPlaybackAction.PREVIOUS -> (current.queue.currentIndex - 1).coerceAtLeast(0)
                        DcPlaybackAction.SELECT -> cmd.index?.takeIf { it in items.indices } ?: return
                        else -> return
                    }
                if (nextIndex == current.queue.currentIndex) {
                    if (action == DcPlaybackAction.PREVIOUS) port.seekTo(0L)
                    return
                }
                val nextState =
                    current.copy(
                        queue = current.queue.copy(currentIndex = nextIndex),
                        positionMs = 0L,
                    )
                val seq = stateVersion.incrementAndGet()
                val snap =
                    DcPlaybackSnapshot(
                        sessionId = "nectar-cmd-$seq",
                        revision = seq,
                        queueRevision = seq,
                        trackRevision = seq,
                        playback = nextState,
                    )
                runCatching { port.apply(snap, 0L) { true } }
            }

            else -> Unit
        }
    }
}
