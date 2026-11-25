/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.nectarcast

import com.metrolist.android.deviceconnect.toDeviceConnectRepeatMode
import com.metrolist.android.deviceconnect.toPlayerRepeatMode
import com.metrolist.shared.deviceconnect.DcLocalPlaybackState
import com.metrolist.shared.deviceconnect.DcPlaybackAction
import com.metrolist.shared.deviceconnect.DcPlaybackCommand
import com.metrolist.shared.deviceconnect.DcPlaybackSnapshot
import com.metrolist.shared.deviceconnect.DeviceConnectPlaybackPort
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.playback.MusicQueueSnapshot

/**
 * Isolates all Metrolist `shared/` model constructors and enum mappings.
 */
object NectarSnapshotBridge {

    fun extractQueueTracks(state: DcLocalPlaybackState): List<NectarQueueTrack> =
        state.queue.displayedItems
            .filterNot { it.isLocal || it.id.startsWith("local_") }
            .map { song ->
                NectarQueueTrack(
                    videoId = song.id,
                    title = song.title,
                    artists = song.artists.map { it.name },
                    durationSec = (song.duration ?: 0L).toInt(),
                    thumbnailUrl = song.thumbnail,
                )
            }

    fun toStateSync(
        ownerDeviceId: String,
        stateVersion: Long,
        state: DcLocalPlaybackState,
        error: String? = null,
    ): NectarStateSync {
        val current = state.queue.displayedItems.getOrNull(state.queue.currentIndex)
        return NectarStateSync(
            stateVersion = stateVersion,
            ownerDeviceId = ownerDeviceId,
            updatedAtUnixMs = System.currentTimeMillis(),
            activeVideoId = current?.id,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isPlaying = state.playing,
            volume = state.volume,
            queueIndex = state.queue.currentIndex,
            title = current?.title,
            artist = current?.artists?.joinToString(", ") { it.name },
            error = error,
        )
    }

    fun buildIncomingSnapshot(payload: NectarHandoffPayload): DcPlaybackSnapshot {
        val songs: List<SongItem> = payload.queue.map { track ->
            SongItem(
                id = track.videoId,
                title = track.title,
                thumbnail = track.thumbnailUrl,
                artists = track.artists.map { ArtistTiny(id = null, name = it) },
                album = null,
                duration = track.durationSec.toLong(),
            )
        }
        val clampedIndex = payload.queueIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))

        val queueSnapshot = MusicQueueSnapshot(
            displayedItems = songs,
            originalItems = songs,
            currentIndex = clampedIndex,
            isShuffled = false,
            originalIndices = songs.indices.toList(),
        )

        val localState = DcLocalPlaybackState(
            queue = queueSnapshot,
            title = payload.queueTitle,
            repeatMode = payload.repeatMode.toDeviceConnectRepeatMode(),
            playing = payload.isPlaying,
            positionMs = payload.positionMs,
            durationMs = songs.getOrNull(clampedIndex)?.duration?.times(1000L) ?: 0L,
            volume = payload.volume,
            ready = true,
        )

        return DcPlaybackSnapshot(
            sessionId = "nectar_${payload.transferId}",
            revision = payload.stateVersion,
            queueRevision = payload.stateVersion,
            trackRevision = payload.stateVersion,
            playback = localState,
            anchorTimeMs = payload.sentAtUnixMs,
        )
    }

    fun mapOutboundCommand(command: DcPlaybackCommand): OutboundCommandWire {
        return OutboundCommandWire(
            action = command.action.name,
            positionMs = if (command.positionMs > 0L || command.action == DcPlaybackAction.SEEK) command.positionMs else null,
            volume = command.volume,
            queueIndex = if (command.index >= 0) command.index else null,
        )
    }

    suspend fun applyIncomingCommand(
        port: DeviceConnectPlaybackPort,
        wire: OutboundCommandWire,
        stateVersion: Long,
    ) {
        val action = runCatching { DcPlaybackAction.valueOf(wire.action) }.getOrNull() ?: return
        when (action) {
            DcPlaybackAction.PLAY -> port.setPlaying(true)
            DcPlaybackAction.PAUSE -> port.setPlaying(false)
            DcPlaybackAction.SEEK -> wire.positionMs?.let { port.seekTo(it) }
            DcPlaybackAction.VOLUME -> wire.volume?.let { port.setVolume(it) }
            DcPlaybackAction.NEXT -> {
                val current = port.snapshot()
                val nextIndex = (current.queue.currentIndex + 1).coerceAtMost(current.queue.displayedItems.lastIndex)
                if (nextIndex >= 0 && nextIndex != current.queue.currentIndex) {
                    val updatedSnapshot = DcPlaybackSnapshot(
                        sessionId = "nectar_cmd_${System.currentTimeMillis()}",
                        revision = stateVersion,
                        queueRevision = stateVersion,
                        trackRevision = stateVersion,
                        playback = current.copy(
                            queue = current.queue.copy(currentIndex = nextIndex),
                            positionMs = 0L,
                            playing = true,
                        ),
                        anchorTimeMs = System.currentTimeMillis(),
                    )
                    port.apply(updatedSnapshot, 0L) { true }
                }
            }
            DcPlaybackAction.PREVIOUS -> {
                val current = port.snapshot()
                val prevIndex = (current.queue.currentIndex - 1).coerceAtLeast(0)
                if (prevIndex != current.queue.currentIndex) {
                    val updatedSnapshot = DcPlaybackSnapshot(
                        sessionId = "nectar_cmd_${System.currentTimeMillis()}",
                        revision = stateVersion,
                        queueRevision = stateVersion,
                        trackRevision = stateVersion,
                        playback = current.copy(
                            queue = current.queue.copy(currentIndex = prevIndex),
                            positionMs = 0L,
                            playing = true,
                        ),
                        anchorTimeMs = System.currentTimeMillis(),
                    )
                    port.apply(updatedSnapshot, 0L) { true }
                }
            }
            DcPlaybackAction.SELECT -> {
                val targetIndex = wire.queueIndex
                if (targetIndex != null) {
                    val current = port.snapshot()
                    val clamped = targetIndex.coerceIn(0, current.queue.displayedItems.lastIndex.coerceAtLeast(0))
                    val updatedSnapshot = DcPlaybackSnapshot(
                        sessionId = "nectar_cmd_${System.currentTimeMillis()}",
                        revision = stateVersion,
                        queueRevision = stateVersion,
                        trackRevision = stateVersion,
                        playback = current.copy(
                            queue = current.queue.copy(currentIndex = clamped),
                            positionMs = 0L,
                            playing = true,
                        ),
                        anchorTimeMs = System.currentTimeMillis(),
                    )
                    port.apply(updatedSnapshot, 0L) { true }
                }
            }
            else -> Unit
        }
    }

    fun applyRemoteStateToShadow(
        shadow: DcLocalPlaybackState,
        sync: NectarStateSync,
    ): DcLocalPlaybackState {
        val targetIndex = sync.activeVideoId?.let { id ->
            shadow.queue.displayedItems.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        } ?: sync.queueIndex.coerceIn(0, (shadow.queue.displayedItems.size - 1).coerceAtLeast(0))

        val updatedQueue = if (targetIndex != shadow.queue.currentIndex) {
            shadow.queue.copy(currentIndex = targetIndex)
        } else {
            shadow.queue
        }

        return shadow.copy(
            queue = updatedQueue,
            playing = sync.isPlaying,
            positionMs = sync.positionMs,
            durationMs = if (sync.durationMs > 0L) sync.durationMs else shadow.durationMs,
            volume = sync.volume,
        )
    }

    fun repeatModeToInt(state: DcLocalPlaybackState): Int = state.repeatMode.toPlayerRepeatMode()
}
