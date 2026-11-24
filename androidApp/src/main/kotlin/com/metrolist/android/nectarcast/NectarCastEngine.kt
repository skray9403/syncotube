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
import com.metrolist.shared.deviceconnect.DeviceConnectPlaybackPort
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class NectarProtocol(val priority: Int, val label: String) {
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

/** Replace StubNectarTransportBridge in AppModule once Rust cast-ffi (UniFFI) is built. */
interface NectarTransportBridge {
    fun observeNectarLinkPeers(): Flow<RawNectarEvent>
    suspend fun sendHandoff(peer: NectarPeer, payload: NectarHandoffPayload): Result<Unit>
    suspend fun sendCommand(peer: NectarPeer, command: DcPlaybackCommand): Result<Unit>
    suspend fun releaseRemote(peer: NectarPeer): Result<Unit>
}

class StubNectarTransportBridge : NectarTransportBridge {
    override fun observeNectarLinkPeers(): Flow<RawNectarEvent> = emptyFlow()
    override suspend fun sendHandoff(peer: NectarPeer, payload: NectarHandoffPayload): Result<Unit> =
        Result.failure(UnsupportedOperationException("Rust cast-ffi transport not linked yet"))
    override suspend fun sendCommand(peer: NectarPeer, command: DcPlaybackCommand): Result<Unit> =
        Result.success(Unit)
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
    }

    fun unbindPort(target: DeviceConnectPlaybackPort) {
        if (port === target) port = null
    }

    fun intercept(command: DcPlaybackCommand): Boolean =
        controller?.interceptPlaybackCommand(command) == true
}

class NectarCastDiscovery(
    private val context: Context,
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
        val sources = buildList {
            if (canUseLocalNetwork) add(scanMdnsLan())
            if (canScanBle) add(scanBle())
            add(transport.observeNectarLinkPeers())
        }
        if (sources.isEmpty()) return flowOf(emptyList())

        return merge(*sources.toTypedArray())
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
                        val updatedRoutes = (prev.routes.filterNot {
                            it.protocol == ev.route.protocol
                        } + ev.route).sortedBy { it.protocol.priority }

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
            }
            .debounce(200L)
    }

    private fun scanMdnsLan(): Flow<RawNectarEvent> = callbackFlow {
        val nsd = context.getSystemService(NsdManager::class.java)
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
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            mainHandler.post {
                                resolving = false
                                resolveNext()
                            }
                        }

                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val attrs = info.attributes
                            val id = attrs["id"]?.decodeToString()
                            val host = info.host?.hostAddress
                            if (!id.isNullOrBlank() && !host.isNullOrBlank()) {
                                val name = attrs["name"]?.decodeToString()?.takeIf { it.isNotBlank() }
                                    ?: info.serviceName
                                val platform = attrs["plat"]?.decodeToString()?.toIntOrNull()
                                    ?.let { NectarPlatform.entries.getOrNull(it) }
                                    ?: NectarPlatform.UNKNOWN
                                trySend(
                                    RawNectarEvent(
                                        deviceId = id,
                                        displayName = name,
                                        platform = platform,
                                        paired = true,
                                        route = NectarRoute(
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

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
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
    private fun scanBle(): Flow<RawNectarEvent> = callbackFlow {
        val scanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            close()
            return@callbackFlow
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getServiceData(BLE_SERVICE_UUID) ?: return
                if (data.size < 20) return // [ver:1][dev_id:16][caps:1][hint:2]

                val deviceId = data.sliceArray(1..16).joinToString("") { "%02x".format(it) }
                val name = result.scanRecord?.deviceName ?: result.device?.name ?: "Nearby Daemon"
                trySend(
                    RawNectarEvent(
                        deviceId = deviceId,
                        displayName = name,
                        platform = NectarPlatform.UNKNOWN,
                        paired = false,
                        route = NectarRoute(
                            protocol = NectarProtocol.BLE_WAKE,
                            bleAddress = result.device?.address,
                        ),
                    ),
                )
            }
        }

        val filter = ScanFilter.Builder().setServiceUuid(BLE_SERVICE_UUID).build()
        val settings = ScanSettings.Builder()
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
) {
    private val transferSeq = AtomicLong(1L)
    private val stateVersion = AtomicLong(1L)

    private val _sessionState = MutableStateFlow(NectarSessionUiState())
    val sessionState: StateFlow<NectarSessionUiState> = _sessionState.asStateFlow()

    private var remoteShadowState: DcLocalPlaybackState? = null

    init {
        NectarCastRegistry.controller = this
    }

    fun handoffTo(peer: NectarPeer) {
        val port = NectarCastRegistry.port ?: run {
            _sessionState.update { it.copy(lastError = "Playback service not bound yet") }
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

            val payload = NectarHandoffPayload(
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

            transport.sendHandoff(peer, payload)
                .onSuccess {
                    remoteShadowState = snap
                    // Put Metrolist into remote-controller mode using its own port
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
            transport.releaseRemote(active)
            val shadow = remoteShadowState
            remoteShadowState = null
            _sessionState.value = NectarSessionUiState()
            if (port != null) {
                port.setControllerPlayback(null, provisional = false)
                port.setAudioOutputEnabled(true)
                port.setExternalReportingEnabled(true, null)
                if (shadow != null) {
                    port.seekTo(shadow.positionMs)
                    port.setPlaying(shadow.playing)
                }
            }
        }
    }

    fun interceptPlaybackCommand(command: DcPlaybackCommand): Boolean {
        val active = _sessionState.value.activePeer ?: return false
        scope.launch {
            transport.sendCommand(active, command)
            val current = remoteShadowState ?: return@launch
            val updated = when (command.action) {
                DcPlaybackAction.PLAY -> current.copy(playing = true)
                DcPlaybackAction.PAUSE -> current.copy(playing = false)
                else -> current
            }
            if (updated != current) {
                remoteShadowState = updated
                NectarCastRegistry.port?.setControllerPlayback(updated, provisional = false)
            }
        }
        return true
    }
}
