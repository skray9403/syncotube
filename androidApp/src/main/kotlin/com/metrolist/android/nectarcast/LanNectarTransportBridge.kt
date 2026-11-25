/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import com.metrolist.shared.deviceconnect.DcPlaybackCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

sealed interface IncomingNectarMessage {
    data class Handoff(
        val payload: NectarHandoffPayload,
    ) : IncomingNectarMessage

    data class Command(
        val actionName: String,
        val positionMs: Long?,
        val volume: Float?,
        val index: Int?,
        val speed: Float?,
    ) : IncomingNectarMessage

    data object Release : IncomingNectarMessage
}

class LanNectarTransportBridge(
    private val scope: CoroutineScope,
    private val advertiser: NectarCastAdvertiser,
) : NectarTransportBridge {
    private val _incoming = MutableSharedFlow<IncomingNectarMessage>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<IncomingNectarMessage> = _incoming.asSharedFlow()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var acceptJob: Job? = null

    @Synchronized
    override fun startServer() {
        if (acceptJob?.isActive == true) return
        acceptJob =
            scope.launch(Dispatchers.IO) {
                val server = runCatching { ServerSocket(0) }.getOrNull() ?: return@launch
                serverSocket = server
                advertiser.start(server.localPort)

                while (isActive && !server.isClosed) {
                    val client = runCatching { server.accept() }.getOrNull() ?: break
                    launch { runCatching { handleClient(client) } }
                }
            }
    }

    @Synchronized
    override fun stopServer() {
        advertiser.stop()
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
    }

    override fun refreshAdvertising() {
        val port = serverSocket?.takeIf { !it.isClosed }?.localPort ?: return
        advertiser.start(port)
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 4_000
            val input = DataInputStream(s.getInputStream())
            val output = DataOutputStream(s.getOutputStream())
            val len = runCatching { input.readInt() }.getOrNull() ?: return
            if (len <= 0 || len > 512 * 1024) return
            val bytes = ByteArray(len)
            input.readFully(bytes)

            val json = JSONObject(bytes.decodeToString())
            val ok =
                when (json.optString("type")) {
                    "handoff" -> {
                        val queueJson = json.optJSONArray("queueVideoIds") ?: JSONArray()
                        val queueIds = List(queueJson.length()) { queueJson.getString(it) }
                        _incoming.tryEmit(
                            IncomingNectarMessage.Handoff(
                                NectarHandoffPayload(
                                    transferId = json.getLong("transferId"),
                                    stateVersion = json.getLong("stateVersion"),
                                    sentAtUnixMs = json.getLong("sentAtUnixMs"),
                                    activeVideoId = json.getString("activeVideoId"),
                                    positionMs = json.getLong("positionMs"),
                                    isPlaying = json.getBoolean("isPlaying"),
                                    volume = json.optDouble("volume", 1.0).toFloat(),
                                    queueVideoIds = queueIds,
                                    queueIndex = json.optInt("queueIndex", 0),
                                    titleHint = json.optString("titleHint", ""),
                                    artistHint = json.optString("artistHint", ""),
                                ),
                            ),
                        )
                    }

                    "command" -> {
                        _incoming.tryEmit(
                            IncomingNectarMessage.Command(
                                actionName = json.getString("action"),
                                positionMs = if (json.has("positionMs")) json.getLong("positionMs") else null,
                                volume = if (json.has("volume")) json.getDouble("volume").toFloat() else null,
                                index = if (json.has("index")) json.getInt("index") else null,
                                speed = if (json.has("speed")) json.getDouble("speed").toFloat() else null,
                            ),
                        )
                    }

                    "release" -> {
                        _incoming.tryEmit(IncomingNectarMessage.Release)
                    }

                    else -> false
                }

            val ack = JSONObject().put("accepted", ok).toString().encodeToByteArray()
            output.writeInt(ack.size)
            output.write(ack)
            output.flush()
        }
    }

    override fun observeNectarLinkPeers(): Flow<RawNectarEvent> = emptyFlow()

    override suspend fun sendHandoff(
        peer: NectarPeer,
        payload: NectarHandoffPayload,
    ): Result<Unit> {
        val json =
            JSONObject()
                .put("type", "handoff")
                .put("transferId", payload.transferId)
                .put("stateVersion", payload.stateVersion)
                .put("sentAtUnixMs", payload.sentAtUnixMs)
                .put("activeVideoId", payload.activeVideoId)
                .put("positionMs", payload.positionMs)
                .put("isPlaying", payload.isPlaying)
                .put("volume", payload.volume.toDouble())
                .put("queueVideoIds", JSONArray(payload.queueVideoIds))
                .put("queueIndex", payload.queueIndex)
                .put("titleHint", payload.titleHint)
                .put("artistHint", payload.artistHint)
        return sendPacket(peer, json)
    }

    override suspend fun sendCommand(
        peer: NectarPeer,
        command: DcPlaybackCommand,
    ): Result<Unit> {
        val json =
            JSONObject()
                .put("type", "command")
                .put("action", command.action.name)
                .put("positionMs", command.positionMs)
                .put("index", command.index)
                .put("speed", command.speed.toDouble())
        command.volume?.let { json.put("volume", it.toDouble()) }
        return sendPacket(peer, json)
    }

    override suspend fun releaseRemote(peer: NectarPeer): Result<Unit> =
        sendPacket(peer, JSONObject().put("type", "release"))

    private suspend fun sendPacket(
        peer: NectarPeer,
        json: JSONObject,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            val route =
                peer.routes.firstOrNull { !it.host.isNullOrBlank() && it.port != null }
                    ?: return@withContext Result.failure(
                        IllegalStateException("No reachable Wi-Fi IP route for ${peer.displayName}"),
                    )

            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(route.host, route.port!!), 2_000)
                    socket.soTimeout = 4_000
                    val body = json.toString().encodeToByteArray()
                    val out = DataOutputStream(socket.getOutputStream())
                    out.writeInt(body.size)
                    out.write(body)
                    out.flush()

                    val input = DataInputStream(socket.getInputStream())
                    val ackLen = input.readInt()
                    if (ackLen <= 0 || ackLen > 64 * 1024) {
                        error("Invalid ack length: $ackLen")
                    }
                    val ackBytes = ByteArray(ackLen)
                    input.readFully(ackBytes)
                    val ack = JSONObject(ackBytes.decodeToString())
                    if (!ack.optBoolean("accepted", false)) {
                        error("Peer rejected packet")
                    }
                }
            }
        }
}
