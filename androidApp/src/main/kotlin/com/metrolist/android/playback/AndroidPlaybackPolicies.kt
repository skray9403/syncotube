package com.metrolist.android.playback

import androidx.media3.common.Player
import com.metrolist.android.models.PersistPlayerState
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.withDownloadsPlayable
import com.metrolist.shared.playback.MusicQueue
import com.metrolist.shared.playback.MusicQueueSnapshot
import com.metrolist.shared.playback.PlaybackCrossfadeState
import com.metrolist.shared.playback.isAutoMixQueueSource
import com.metrolist.shared.playback.isRecommendationGeneratedQueueSource
import com.metrolist.shared.playback.isRecommendedQueueSource
import com.metrolist.shared.youtube.extraction.models.toContentHints
import com.metrolist.shared.youtube.playback.PlaybackResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

internal const val NEXT_TRACK_PRELOAD_DELAY_MS = 5_000L
internal const val NEXT_TRACK_PRELOAD_SETTLE_DELAY_MS = 500L
internal const val CROSSFADE_UPDATE_INTERVAL_MS = 16L

internal typealias AndroidCrossfadeState = PlaybackCrossfadeState<SongItem>

internal const val QUEUE_AUTO_LOAD_LIMIT = 2
internal const val REPORTING_TAG = "DeviceConnectReporting"

internal suspend fun releaseIdleTokenProvider(
    playbackRequested: Flow<Boolean>,
    playbackLoading: Flow<Boolean>,
    tokenProvider: TokenProvider,
) {
    try {
        combine(playbackRequested, playbackLoading) { requested, loading -> requested || loading }
            .distinctUntilChanged()
            .collectLatest { active ->
                if (!active) {
                    delay(60.seconds)
                    tokenProvider.close()
                }
            }
    } finally {
        withContext(NonCancellable) { tokenProvider.close() }
    }
}

internal fun automaticCacheTracks(
    song: SongItem?,
    resource: PlaybackResource?,
    isPlaying: Boolean,
): List<SongItem> = listOfNotNull(song).takeIf { song?.id == resource?.videoId && isPlaying }.orEmpty()

internal data class AutoLoadMoreTrigger(
    val sessionId: Long,
    val currentIndex: Int,
    val queueSize: Int,
    val currentSongId: String?,
    val shouldLoadMore: Boolean,
)

internal fun shouldBootstrapQueueRecommendations(
    finiteQueue: Boolean,
    autoLoadMore: Boolean,
    hasMorePlaylistPages: Boolean,
): Boolean = !hasMorePlaylistPages && (!finiteQueue || autoLoadMore)

internal fun hasUpcomingQueuedTracks(
    queue: List<SongItem>,
    currentIndex: Int,
): Boolean {
    if (queue.isEmpty() || currentIndex !in queue.indices) return false
    for (i in (currentIndex + 1) until queue.size) {
        if (!queue[i].queueSourceTitle.isRecommendedQueueSource()) {
            return true
        }
    }
    return false
}

internal fun shouldAutoLoadMore(
    currentIndex: Int,
    queue: List<SongItem>,
    autoLoadMore: Boolean,
    repeatMode: Int,
): Boolean =
    autoLoadMore &&
        queue.isNotEmpty() &&
        currentIndex in queue.indices &&
        !hasUpcomingQueuedTracks(queue, currentIndex) &&
        repeatMode == Player.REPEAT_MODE_OFF

internal fun shouldPreloadNextTrack(
    playbackPositionMs: Long,
    nextVideoId: String?,
    repeatMode: Int,
    stopAfterCurrentSong: Boolean,
): Boolean =
    playbackPositionMs >= NEXT_TRACK_PRELOAD_DELAY_MS &&
        nextVideoId != null &&
        repeatMode != Player.REPEAT_MODE_ONE &&
        !stopAfterCurrentSong

internal fun shouldStartAndroidCrossfade(
    playbackPositionMs: Long,
    durationMs: Long,
    crossfadeDurationMs: Long,
    isPlaying: Boolean,
    crossfadeEnabled: Boolean,
    isCrossfading: Boolean,
    repeatMode: Int,
    isGapless: Boolean,
    stopAfterCurrentSong: Boolean,
): Boolean {
    if (!isPlaying || !crossfadeEnabled || isCrossfading || isGapless || stopAfterCurrentSong || durationMs <= 0L) return false
    if (repeatMode == Player.REPEAT_MODE_ONE || playbackPositionMs <= 500L) return false
    val remainingMs = durationMs - playbackPositionMs
    return remainingMs in 1L..crossfadeDurationMs
}

internal class CrossfadeCompletionGuard {
    private var playbackInstanceId: Long? = null
    private var completionConsumed = false
    val hasConsumedCompletion: Boolean get() = completionConsumed

    fun arm(playbackInstanceId: Long) {
        this.playbackInstanceId = playbackInstanceId
        completionConsumed = false
    }

    fun clear() {
        playbackInstanceId = null
        completionConsumed = false
    }

    fun consume(completedPlaybackInstanceId: Long?): Boolean {
        if (completedPlaybackInstanceId == null || completedPlaybackInstanceId != playbackInstanceId) return false
        playbackInstanceId = null
        completionConsumed = true
        return true
    }

    fun takeConsumedCompletion(): Boolean = completionConsumed.also { completionConsumed = false }

    fun discardConsumedCompletion() {
        completionConsumed = false
    }
}

internal fun repeatedQueueIndex(
    repeatMode: Int,
    currentIndex: Int,
    lastIndex: Int,
    hasMore: Boolean = false,
): Int? =
    when {
        currentIndex !in 0..lastIndex -> null
        repeatMode == Player.REPEAT_MODE_ONE -> currentIndex
        repeatMode == Player.REPEAT_MODE_ALL && currentIndex == lastIndex && !hasMore -> 0
        else -> null
    }

internal fun nextQueuedIndex(
    queueSize: Int,
    currentIndex: Int,
    repeatMode: Int,
    hasMore: Boolean = false,
): Int? =
    if (currentIndex !in 0 until queueSize) {
        null
    } else {
        (currentIndex + 1).takeIf { it < queueSize }
            ?: repeatedQueueIndex(repeatMode, currentIndex, queueSize - 1, hasMore)
    }

internal suspend fun <T> cachedAudioResource(
    load: suspend () -> T?,
): T? = load()

internal enum class CastQueueReplacementResult {
    REMOTE,
    LOCAL,
    STALE,
}

internal class CastDisconnectSuppression {
    private var explicitDisconnectInProgress = false
    private var explicitDisconnectPending = false
    private var replacementPending = false

    fun beginExplicitDisconnect() {
        explicitDisconnectInProgress = true
        explicitDisconnectPending = false
    }

    fun finishExplicitDisconnect(disconnected: Boolean) {
        if (!explicitDisconnectInProgress) return
        explicitDisconnectInProgress = false
        explicitDisconnectPending = disconnected
    }

    fun beginReplacement() {
        replacementPending = true
    }

    fun cancelReplacement() {
        replacementPending = false
    }

    fun finishReplacement(disconnected: Boolean) {
        if (!replacementPending) return
        replacementPending = false
        if (disconnected) explicitDisconnectPending = true
    }

    fun onConnectionState(isConnected: Boolean): Boolean {
        if (isConnected) {
            if (!explicitDisconnectInProgress && !replacementPending) explicitDisconnectPending = false
            return false
        }
        val suppress = explicitDisconnectInProgress || explicitDisconnectPending || replacementPending
        explicitDisconnectInProgress = false
        explicitDisconnectPending = false
        replacementPending = false
        return suppress
    }
}

internal suspend fun routeCastQueueReplacement(
    suppression: CastDisconnectSuppression,
    isCurrentRequest: () -> Boolean,
    isConnected: () -> Boolean,
    replace: suspend () -> Boolean,
    disconnect: suspend () -> Unit,
    onFailure: (Exception) -> Unit = {},
): CastQueueReplacementResult {
    if (!isCurrentRequest()) return CastQueueReplacementResult.STALE
    suppression.beginReplacement()
    val replaced =
        try {
            replace()
        } catch (error: CancellationException) {
            suppression.cancelReplacement()
            throw error
        } catch (error: Exception) {
            onFailure(error)
            false
        }
    val connected = isConnected()
    suppression.finishReplacement(disconnected = !connected)
    if (!isCurrentRequest()) return CastQueueReplacementResult.STALE
    if (replaced && connected) return CastQueueReplacementResult.REMOTE
    if (connected) {
        suppression.beginExplicitDisconnect()
        try {
            disconnect()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            onFailure(error)
        } finally {
            suppression.finishExplicitDisconnect(disconnected = !isConnected())
        }
    }
    return if (isCurrentRequest()) CastQueueReplacementResult.LOCAL else CastQueueReplacementResult.STALE
}

internal fun SongItem.toAndroidContentHints(
    @Suppress("UNUSED_PARAMETER") wantVideo: Boolean = false,
) = toContentHints().copy(wantVideo = false)

internal fun isCurrentRecommendationRequest(
    expectedSessionId: Long,
    expectedRequestGeneration: Long,
    currentSessionId: Long,
    currentRequestGeneration: Long,
): Boolean = expectedSessionId == currentSessionId && expectedRequestGeneration == currentRequestGeneration

internal val externalTransportPlayerCommandCodes =
    setOf(
        Player.COMMAND_PLAY_PAUSE,
        Player.COMMAND_PREPARE,
        Player.COMMAND_STOP,
        Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
        Player.COMMAND_SEEK_BACK,
        Player.COMMAND_SEEK_FORWARD,
        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
        Player.COMMAND_SET_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        Player.COMMAND_SET_VOLUME,
        Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
        Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
    )

internal fun externalTransportPlayerCommands(): Player.Commands =
    Player.Commands
        .Builder()
        .addAllReadOnlyCommands()
        .apply { externalTransportPlayerCommandCodes.forEach(::add) }
        .build()

internal fun newQueueShuffleEnabled(
    requestedShuffle: Boolean?,
    currentShuffle: Boolean,
    persistentShuffle: Boolean,
): Boolean = requestedShuffle ?: (currentShuffle && persistentShuffle)

internal fun restoredShuffleMode(
    persistedShuffle: Boolean,
    rememberShuffleAndRepeat: Boolean,
): Boolean = persistedShuffle && rememberShuffleAndRepeat

internal fun filteredQueueSelection(
    songs: List<SongItem>,
    startIndex: Int,
    isBlocked: (SongItem) -> Boolean,
): Pair<List<SongItem>, Int>? {
    if (songs.isEmpty()) return null
    val queue = MusicQueue().apply { setQueue(songs, startIndex.coerceIn(songs.indices)) }
    val queued = queue.items.value
    queue.removeAt(queued.indices.filterTo(mutableSetOf()) { isBlocked(queued[it]) })
    return queue.items.value
        .takeIf { it.isNotEmpty() }
        ?.let { it to queue.currentIndex.value }
}

internal fun PersistPlayerState.restoredPlaybackState(
    rememberShuffleAndRepeat: Boolean,
    isBlocked: (SongItem) -> Boolean,
): Pair<PersistPlayerState, MusicQueueSnapshot> {
    val persistedQueue =
        queueSnapshot?.takeIf { it.displayedItems.isNotEmpty() }
            ?: MusicQueueSnapshot(queueSongs, queueSongs, currentMediaItemIndex, shuffleModeEnabled)
    val queue = MusicQueue().apply { restore(persistedQueue) }
    val displayed = queue.items.value
    val blockedIndices = displayed.indices.filterTo(mutableSetOf()) { isBlocked(displayed[it]) }
    val currentSurvives =
        persistedQueue.displayedItems.getOrNull(persistedQueue.currentIndex)?.let { !it.isUnavailable && !isBlocked(it) } == true
    queue.removeAt(blockedIndices)
    if (!rememberShuffleAndRepeat && queue.isShuffled) queue.toggleShuffle()
    val restoredQueue = queue.snapshot()
    return copy(currentPosition = currentPosition.takeIf { currentSurvives } ?: 0L) to restoredQueue
}

internal fun generatedQueueIndicesToRemoveForRepeat(
    queue: List<SongItem>,
    currentIndex: Int,
    preserveAutoMix: Boolean = false,
): List<Int> =
    queue.indices.filter {
        it != currentIndex && queue[it].queueSourceTitle.isRecommendationGeneratedQueueSource() &&
            !(preserveAutoMix && queue[it].queueSourceTitle.isAutoMixQueueSource())
    }

internal fun downloadedAlbumQueueSongs(
    songs: List<SongItem>,
    downloadedSongIds: Set<String>,
    remotePlayback: Boolean,
): List<SongItem> = if (remotePlayback) songs else songs.withDownloadsPlayable(downloadedSongIds)
