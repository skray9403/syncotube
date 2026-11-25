/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import com.metrolist.shared.deviceconnect.DcPlaybackCommand
import com.metrolist.shared.deviceconnect.DeviceConnectPlaybackPort
import kotlinx.coroutines.flow.Flow

enum class NectarProtocol(val priority: Int, val label: String) {
    WIFI_TCP(1, "Wi-Fi · LAN"),
    WIFI_QUIC(1, "Wi-Fi · QUIC"),
    BLE_HINT_LAN(2, "Bluetooth → Wi-Fi"),
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
    val deviceId: String,
    val displayName: String,
    val platform: NectarPlatform,
    val paired: Boolean,
    val routes: List<NectarRoute>,
) {
    val primaryRoute: NectarRoute get() = routes.first()
}

sealed interface RawDiscoveryDelta {
    data class Upsert(
        val deviceId: String,
        val displayName: String,
        val platform: NectarPlatform,
        val paired: Boolean,
        val route: NectarRoute,
    ) : RawDiscoveryDelta

    data class RemoveRoute(
        val deviceId: String,
        val protocol: NectarProtocol,
    ) : RawDiscoveryDelta
}

data class NectarQueueTrack(
    val videoId: String,
    val title: String,
    val artists: List<String>,
    val durationSec: Int,
    val thumbnailUrl: String?,
)

data class NectarHandoffPayload(
    val transferId: Long,
    val stateVersion: Long,
    val sentAtUnixMs: Long,
    val controllerDeviceId: String,
    val controllerName: String,
    val controllerPort: Int,
    val activeVideoId: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val volume: Float,
    val repeatMode: Int,
    val queueTitle: String?,
    val queue: List<NectarQueueTrack>,
    val queueIndex: Int,
)

data class NectarStateSync(
    val stateVersion: Long,
    val ownerDeviceId: String,
    val updatedAtUnixMs: Long,
    val activeVideoId: String?,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val volume: Float,
    val queueIndex: Int,
    val title: String? = null,
    val artist: String? = null,
    val error: String? = null,
)

data class OutboundCommandWire(
    val action: String,
    val positionMs: Long? = null,
    val volume: Float? = null,
    val queueIndex: Int? = null,
)

data class NectarAck(
    val accepted: Boolean,
    val error: String? = null,
    val state: NectarStateSync? = null,
)

sealed interface IncomingNectarMessage {
    data class Handoff(val payload: NectarHandoffPayload) : IncomingNectarMessage
    data class Command(
        val senderDeviceId: String,
        val stateVersion: Long,
        val wire: OutboundCommandWire,
    ) : IncomingNectarMessage

    data class StateSync(val sync: NectarStateSync) : IncomingNectarMessage
    data class Release(val senderDeviceId: String) : IncomingNectarMessage
}

data class NectarSessionUiState(
    val activePeer: NectarPeer? = null,
    val remoteControllingUs: String? = null,
    val transferringDeviceId: String? = null,
    val lastError: String? = null,
)

interface NectarTransportBridge {
    val localListenPort: Int?
    fun setIncomingHandler(handler: suspend (remoteIp: String, message: IncomingNectarMessage) -> NectarAck)
    fun startServer()
    fun stopServer()
    fun refreshAdvertising()
    fun observeNectarLinkPeers(): Flow<RawDiscoveryDelta>
    suspend fun sendHandoff(peer: NectarPeer, payload: NectarHandoffPayload): Result<NectarAck>
    suspend fun sendCommand(
        peer: NectarPeer,
        senderDeviceId: String,
        stateVersion: Long,
        wire: OutboundCommandWire,
    ): Result<NectarAck>
    suspend fun sendRelease(peer: NectarPeer, senderDeviceId: String): Result<NectarAck>
    suspend fun pushStateSync(host: String, port: Int, sync: NectarStateSync): Result<Unit>
}

object NectarCastRegistry {
    @Volatile var port: DeviceConnectPlaybackPort? = null
        private set

    @Volatile var controller: NectarCastController? = null
        private set

    fun registerController(instance: NectarCastController) {
        controller = instance
        if (port != null) {
            instance.onPlaybackPortBound()
        }
    }

    fun bindPort(target: DeviceConnectPlaybackPort) {
        port = target
        controller?.onPlaybackPortBound()
    }

    fun unbindPort(target: DeviceConnectPlaybackPort) {
        if (port === target) {
            port = null
            controller?.onPlaybackPortUnbound()
        }
    }

    fun intercept(command: DcPlaybackCommand): Boolean =
        controller?.interceptPlaybackCommand(command) == true
}
