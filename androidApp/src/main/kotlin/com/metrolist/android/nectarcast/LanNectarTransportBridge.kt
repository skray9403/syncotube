/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class LanNectarTransportBridge(
    private val scope: CoroutineScope,
    private val advertiser: NectarCastAdvertiser,
) : NectarTransportBridge {

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    @Volatile private var incomingHandler: (suspend (String, IncomingNectarMessage) -> NectarAck)? = null

    override val localListenPort: Int?
        get() = serverSocket?.localPort?.takeIf { it > 0 }

    override fun setIncomingHandler(
        handler: suspend (remoteIp: String, message: IncomingNectarMessage) -> NectarAck,
    ) {
        incomingHandler = handler
    }

    override fun startServer() {
        if (acceptJob?.isActive == true) return
        acceptJob = scope.launch(Dispatchers.IO) {
            val server = runCatching { ServerSocket(0) }.getOrNull() ?: return@launch
            serverSocket = server
            advertiser.start(server.localPort)

            while (isActive && !server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                launch(Dispatchers.IO) { handleClient(client) }
            }
        }
    }

    override fun refreshAdvertising() {
        advertiser.refresh()
    }

    override fun stopServer() {
        advertiser.stop()
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
    }

    override fun observeNectarLinkPeers(): Flow<RawDiscoveryDelta> = emptyFlow()

    override suspend fun sendHandoff(peer: NectarPeer, payload: NectarHandoffPayload): Result<NectarAck> {
        val queueArray = JSONArray().apply {
            payload.queue.forEach { track ->
                put(
                    JSONObject()
                        .put("videoId", track.videoId)
                        .put("title", track.title)
                        .put("artists", JSONArray(track.artists))
                        .put("durationSec", track.durationSec)
                        .put("thumbnailUrl", track.thumbnailUrl ?: JSONObject.NULL),
                )
            }
        }

        val json = JSONObject()
            .put("type", "handoff")
            .put("transferId", payload.transferId)
            .put("stateVersion", payload.stateVersion)
            .put("sentAtUnixMs", payload.sentAtUnixMs)
            .put("controllerDeviceId", payload.controllerDeviceId)
            .put("controllerName", payload.controllerName)
            .put("controllerPort", payload.controllerPort)
            .put("activeVideoId", payload.activeVideoId)
            .put("positionMs", payload.positionMs)
            .put("isPlaying", payload.isPlaying)
            .put("volume", payload.volume.toDouble())
            .put("repeatMode", payload.repeatMode)
            .put("queueTitle", payload.queueTitle ?: JSONObject.NULL)
            .put("queue", queueArray)
            .put("queueIndex", payload.queueIndex)

        return sendToPeer(peer, json)
    }

    override suspend fun sendCommand(
        peer: NectarPeer,
        senderDeviceId: String,
        stateVersion: Long,
        wire: OutboundCommandWire,
    ): Result<NectarAck> {
        val json = JSONObject()
            .put("type", "command")
            .put("senderDeviceId", senderDeviceId)
            .put("stateVersion", stateVersion)
            .put("action", wire.action)
        wire.positionMs?.let { json.put("positionMs", it) }
        wire.volume?.let { json.put("volume", it.toDouble()) }
        wire.queueIndex?.let { json.put("queueIndex", it) }
        return sendToPeer(peer, json)
    }

    override suspend fun sendRelease(peer: NectarPeer, senderDeviceId: String): Result<NectarAck> {
        val json = JSONObject()
            .put("type", "release")
            .put("senderDeviceId", senderDeviceId)
        return sendToPeer(peer, json)
    }

    override suspend fun pushStateSync(host: String, port: Int, sync: NectarStateSync): Result<Unit> {
        val json = encodeStateSync(sync).put("type", "state_sync")
        return sendRaw(host, port, json).map { Unit }
    }

    private suspend fun sendToPeer(peer: NectarPeer, json: JSONObject): Result<NectarAck> {
        val ipRoutes = peer.routes.filter { !it.host.isNullOrBlank() && (it.port ?: 0) > 0 }
        if (ipRoutes.isEmpty()) {
            return Result.failure(
                IllegalStateException("${peer.displayName} only seen over Bluetooth; join same Wi-Fi to hand off"),
            )
        }
        var lastError: Throwable? = null
        for (route in ipRoutes) {
            val result = sendRaw(route.host!!, route.port!!, json)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
        }
        return Result.failure(lastError ?: IllegalStateException("Failed to reach ${peer.displayName}"))
    }

    private suspend fun sendRaw(host: String, port: Int, json: JSONObject): Result<NectarAck> =
        withContext(Dispatchers.IO) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 2_000)
                    socket.soTimeout = 4_000
                    val body = json.toString().encodeToByteArray()
                    val out = DataOutputStream(socket.getOutputStream())
                    out.writeInt(body.size)
                    out.write(body)
                    out.flush()

                    val input = DataInputStream(socket.getInputStream())
                    val ackLen = input.readInt()
                    require(ackLen in 1..MAX_FRAME_BYTES) { "Invalid ACK length: $ackLen" }
                    val ackBytes = ByteArray(ackLen)
                    input.readFully(ackBytes)
                    val ackJson = JSONObject(ackBytes.decodeToString())
                    val ack = NectarAck(
                        accepted = ackJson.optBoolean("accepted", false),
                        error = ackJson.optString("error").takeIf { it.isNotBlank() },
                        state = ackJson.optJSONObject("state")?.let(::decodeStateSync),
                    )
                    if (!ack.accepted) {
                        error(ack.error ?: "Peer rejected request")
                    }
                    ack
                }
            }
        }

    private suspend fun handleClient(socket: Socket) {
        socket.use { s ->
            runCatching {
                s.soTimeout = 4_000
                val remoteIp = s.inetAddress?.hostAddress.orEmpty()
                val input = DataInputStream(s.getInputStream())
                val output = DataOutputStream(s.getOutputStream())
                val len = input.readInt()
                if (len !in 1..MAX_FRAME_BYTES) return
                val bytes = ByteArray(len)
                input.readFully(bytes)

                val json = JSONObject(bytes.decodeToString())
                val parsed = decodeIncoming(json)
                val ack = if (parsed == null) {
                    NectarAck(accepted = false, error = "Malformed message")
                } else {
                    incomingHandler?.invoke(remoteIp, parsed)
                        ?: NectarAck(accepted = false, error = "Receiver not ready")
                }

                val ackJson = JSONObject()
                    .put("accepted", ack.accepted)
                ack.error?.let { ackJson.put("error", it) }
                ack.state?.let { ackJson.put("state", encodeStateSync(it)) }

                val outBytes = ackJson.toString().encodeToByteArray()
                output.writeInt(outBytes.size)
                output.write(outBytes)
                output.flush()
            }
        }
    }

    private fun decodeIncoming(json: JSONObject): IncomingNectarMessage? =
        when (json.optString("type")) {
            "handoff" -> {
                val queueArr = json.optJSONArray("queue") ?: JSONArray()
                val queue = List(queueArr.length()) { idx ->
                    val obj = queueArr.getJSONObject(idx)
                    val artistsArr = obj.optJSONArray("artists") ?: JSONArray()
                    NectarQueueTrack(
                        videoId = obj.getString("videoId"),
                        title = obj.optString("title", ""),
                        artists = List(artistsArr.length()) { artistsArr.getString(it) },
                        durationSec = obj.optInt("durationSec", 0),
                        thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() && it != "null" },
                    )
                }
                IncomingNectarMessage.Handoff(
                    NectarHandoffPayload(
                        transferId = json.getLong("transferId"),
                        stateVersion = json.getLong("stateVersion"),
                        sentAtUnixMs = json.getLong("sentAtUnixMs"),
                        controllerDeviceId = json.getString("controllerDeviceId"),
                        controllerName = json.optString("controllerName", "Remote Device"),
                        controllerPort = json.optInt("controllerPort", 0),
                        activeVideoId = json.getString("activeVideoId"),
                        positionMs = json.getLong("positionMs"),
                        isPlaying = json.getBoolean("isPlaying"),
                        volume = json.optDouble("volume", 1.0).toFloat(),
                        repeatMode = json.optInt("repeatMode", 0),
                        queueTitle = json.optString("queueTitle").takeIf { it.isNotBlank() && it != "null" },
                        queue = queue,
                        queueIndex = json.optInt("queueIndex", 0),
                    ),
                )
            }

            "command" -> IncomingNectarMessage.Command(
                senderDeviceId = json.optString("senderDeviceId", ""),
                stateVersion = json.optLong("stateVersion", 0L),
                wire = OutboundCommandWire(
                    action = json.getString("action"),
                    positionMs = if (json.has("positionMs")) json.getLong("positionMs") else null,
                    volume = if (json.has("volume")) json.getDouble("volume").toFloat() else null,
                    queueIndex = if (json.has("queueIndex")) json.getInt("queueIndex") else null,
                ),
            )

            "state_sync" -> IncomingNectarMessage.StateSync(decodeStateSync(json))

            "release" -> IncomingNectarMessage.Release(
                senderDeviceId = json.optString("senderDeviceId", ""),
            )

            else -> null
        }

    private fun encodeStateSync(sync: NectarStateSync): JSONObject =
        JSONObject()
            .put("stateVersion", sync.stateVersion)
            .put("ownerDeviceId", sync.ownerDeviceId)
            .put("updatedAtUnixMs", sync.updatedAtUnixMs)
            .put("activeVideoId", sync.activeVideoId ?: JSONObject.NULL)
            .put("positionMs", sync.positionMs)
            .put("durationMs", sync.durationMs)
            .put("isPlaying", sync.isPlaying)
            .put("volume", sync.volume.toDouble())
            .put("queueIndex", sync.queueIndex)
            .put("title", sync.title ?: JSONObject.NULL)
            .put("artist", sync.artist ?: JSONObject.NULL)
            .put("error", sync.error ?: JSONObject.NULL)

    private fun decodeStateSync(json: JSONObject): NectarStateSync =
        NectarStateSync(
            stateVersion = json.optLong("stateVersion", 0L),
            ownerDeviceId = json.optString("ownerDeviceId", ""),
            updatedAtUnixMs = json.optLong("updatedAtUnixMs", System.currentTimeMillis()),
            activeVideoId = json.optString("activeVideoId").takeIf { it.isNotBlank() && it != "null" },
            positionMs = json.optLong("positionMs", 0L),
            durationMs = json.optLong("durationMs", 0L),
            isPlaying = json.optBoolean("isPlaying", false),
            volume = json.optDouble("volume", 1.0).toFloat(),
            queueIndex = json.optInt("queueIndex", 0),
            title = json.optString("title").takeIf { it.isNotBlank() && it != "null" },
            artist = json.optString("artist").takeIf { it.isNotBlank() && it != "null" },
            error = json.optString("error").takeIf { it.isNotBlank() && it != "null" },
        )

    private companion object {
        const val MAX_FRAME_BYTES = 512 * 1024
    }
}
