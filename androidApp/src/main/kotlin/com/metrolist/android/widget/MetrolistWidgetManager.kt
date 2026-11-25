/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.os.BundleCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.metrolist.android.R
import com.metrolist.android.playback.MusicService
import com.metrolist.android.ui.theme.extractThemeColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

class MetrolistWidgetManager(
    private val context: Context,
    private val imageLoader: ImageLoader,
) {
    private val updateMutex = Mutex()
    private var cachedArtworkUri: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedCookie12Art: Bitmap? = null
    private var cachedCookie9Art: Bitmap? = null
    private var cachedWideCardArt: Bitmap? = null
    private var cachedArtworkSeed: Color? = null
    private val cookie12DefaultIcon by lazy { getCookieBitmap(appIconBitmap()) }
    private val cookie9DefaultIcon by lazy { getCookie9Bitmap(appIconBitmap()) }

    private val progressDemand = MutableStateFlow(false)
    internal val hasProgressWidgets = progressDemand.asStateFlow()
    private var publishedPlayback: Pair<String?, Long>? = null
    private val progressBuckets = mutableMapOf<Int, Long>()
    private val musicWidgetViews = mutableMapOf<Int, MusicWidgetRemoteViews>()

    @Volatile
    private var lastControls = WidgetControls()

    @Volatile
    private var lastWidgetState =
        WidgetState(
            title = context.getString(R.string.not_playing),
            artist = context.getString(R.string.tap_to_play),
            artworkUri = null,
            isPlaying = false,
            isLiked = false,
            duration = 0,
            currentPosition = 0,
        )

    internal fun refreshProgressDemand() {
        val manager = AppWidgetManager.getInstance(context)
        val music = manager.getAppWidgetIds(ComponentName(context, MusicWidgetReceiver::class.java))
        progressDemand.value =
            music.any { id ->
                val options = manager.getAppWidgetOptions(id)
                widgetSizes(options).any { (width, height) ->
                    musicWidgetSizePolicy(width, height, context.resources.configuration.fontScale).layout.showsProgress
                }
            }
    }

    internal suspend fun updateProgress(
        songId: String,
        playbackInstanceId: Long,
        duration: Long,
        position: Long,
    ) = updateMutex.withLock {
        if (publishedPlayback != (songId to playbackInstanceId)) return@withLock
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MusicWidgetReceiver::class.java))
        progressBuckets.keys.retainAll(ids.toSet())
        musicWidgetViews.keys.retainAll(ids.toSet())
        lastWidgetState = lastWidgetState.copy(duration = duration, currentPosition = position)
        val bucket = position.coerceAtLeast(0L) / 1000L
        ids.forEach { id ->
            val previous = progressBuckets[id] ?: return@forEach
            if (previous == bucket) return@forEach
            val snapshot = musicWidgetViews[id] ?: return@forEach
            if (snapshot.sizes != widgetSizes(manager.getAppWidgetOptions(id))) return@forEach
            // Android merges only top-level actions, so wrapped layouts need their complete tree.
            if (safeUpdateAppWidget(manager, id, snapshot.create(duration, position))) {
                progressBuckets[id] = bucket
            }
        }
        refreshProgressDemand()
    }

    internal suspend fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0,
        controls: WidgetControls = WidgetControls(),
        songId: String? = null,
        playbackInstanceId: Long = 0,
    ) = updateMutex.withLock {
        publishedPlayback = null
        lastWidgetState = WidgetState(title, artist, artworkUri, isPlaying, isLiked, duration, currentPosition)
        lastControls = controls
        renderPlaybackWidgets()
        publishedPlayback = songId to playbackInstanceId
        refreshProgressDemand()
    }

    private suspend fun renderPlaybackWidgets() =
        withContext(Dispatchers.IO) {
            val (title, artist, artworkUri, _, isLiked, duration, currentPosition) = lastWidgetState
            val controls = lastControls
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val musicWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MusicWidgetReceiver::class.java))
            musicWidgetViews.keys.retainAll(musicWidgetIds.toSet())
            if (musicWidgetIds.isNotEmpty()) {
                val albumArt = getCachedAlbumArt(artworkUri)
                val schemes = resolveSchemesForArtwork(albumArt)
                progressBuckets.clear()
                musicWidgetIds.forEach { widgetId ->
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val snapshot =
                        createRemoteViewsForSize(
                            options = options,
                            title = title,
                            artist = artist,
                            albumArt = albumArt,
                            isLiked = isLiked,
                            duration = duration,
                            currentPosition = currentPosition,
                            controls = controls,
                            schemes = schemes,
                        )
                    if (safeUpdateAppWidget(appWidgetManager, widgetId, snapshot.create())) {
                        musicWidgetViews[widgetId] = snapshot
                        if (duration > 0 &&
                            widgetSizes(options).any { (width, height) ->
                                musicWidgetSizePolicy(width, height, context.resources.configuration.fontScale).layout.showsProgress
                            }
                        ) {
                            progressBuckets[widgetId] = currentPosition.coerceAtLeast(0L) / 1000L
                        }
                    }
                }
            }
        }

    internal suspend fun refreshWidgetsTheme() {
        updateMutex.withLock {
            if (MusicService.isRunning) {
                renderPlaybackWidgets()
            } else {
                lastWidgetState = idleWidgetState()
                lastControls = WidgetControls()
                publishedPlayback = null
                renderPlaybackWidgets()
            }
            refreshProgressDemand()
        }
    }

    suspend fun updateIdleMusicWidgets(appWidgetIds: IntArray) =
        updateMutex.withLock {
            if (appWidgetIds.isEmpty() || MusicService.isRunning) return@withLock
            publishedPlayback = null
            withContext(Dispatchers.IO) {
                updateMusicWidgets(appWidgetIds, idleWidgetState())
            }
        }

    private suspend fun updateMusicWidgets(
        appWidgetIds: IntArray,
        state: WidgetState,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val albumArt = getCachedAlbumArt(state.artworkUri)
        val schemes = resolveSchemesForArtwork(albumArt)
        appWidgetIds.forEach { widgetId ->
            val snapshot =
                createRemoteViewsForSize(
                    options = appWidgetManager.getAppWidgetOptions(widgetId),
                    title = state.title,
                    artist = state.artist,
                    albumArt = albumArt,
                    isLiked = state.isLiked,
                    duration = state.duration,
                    currentPosition = state.currentPosition,
                    controls = WidgetControls(),
                    schemes = schemes,
                )
            if (safeUpdateAppWidget(appWidgetManager, widgetId, snapshot.create())) {
                musicWidgetViews[widgetId] = snapshot
            }
        }
    }

    private fun safeUpdateAppWidget(
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        views: RemoteViews,
    ): Boolean =
        try {
            appWidgetManager.updateAppWidget(widgetId, views)
            true
        } catch (error: Exception) {
            Timber.w(error, "Failed to update widget %d", widgetId)
            false
        }

    private fun idleWidgetState() =
        WidgetState(
            title = context.getString(R.string.not_playing),
            artist = context.getString(R.string.tap_to_play),
            artworkUri = null,
            isPlaying = false,
            isLiked = false,
            duration = 0,
            currentPosition = 0,
        )

    private fun resolveSchemesForArtwork(albumArt: Bitmap?): WidgetColorSchemes {
        val config = WidgetThemeConfig.fromSettings()
        val seed =
            if (config.materialYouEnabled && config.dynamicTheme) {
                cachedArtworkSeed ?: albumArt?.extractThemeColor()?.also { cachedArtworkSeed = it }
            } else {
                null
            }
        return resolveWidgetColorSchemes(context, seed, config)
    }

    private fun createRemoteViewsForSize(
        options: Bundle,
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long,
        controls: WidgetControls,
        schemes: WidgetColorSchemes,
    ): MusicWidgetRemoteViews {
        fun create(
            width: Int,
            height: Int,
        ): RemoteViews {
            val policy = musicWidgetSizePolicy(width, height, context.resources.configuration.fontScale)
            val views =
                when (policy.layout) {
                    MusicWidgetLayout.CONTROLS -> {
                        RemoteViews(context.packageName, R.layout.widget_compact_controls)
                    }

                    MusicWidgetLayout.COMPACT -> {
                        createCompactRemoteViews(title, artist, albumArt, duration, currentPosition, policy.showArtwork)
                    }

                    MusicWidgetLayout.WIDE -> {
                        createCompactWideRemoteViews(title, artist, albumArt, policy.showArtwork)
                    }

                    MusicWidgetLayout.SQUARE -> {
                        createCompactSquareRemoteViews(
                            title,
                            artist,
                            albumArt,
                            duration,
                            currentPosition,
                            policy.showArtwork,
                            schemes,
                        )
                    }

                    MusicWidgetLayout.PLAYER -> {
                        createPlayerRemoteViews(title, artist, albumArt, duration, currentPosition)
                    }
                }
            bindMusicControls(views, policy.layout, controls, isLiked)
            bindMusicColors(views, policy.layout, controls.playbackRequested, isLiked, schemes)
            applyResponsiveInsetPadding(views, policy.layout, width, height)
            return views
        }
        return remoteViewsForSizes(options, ::create)
    }

    private fun applyResponsiveInsetPadding(
        views: RemoteViews,
        layout: MusicWidgetLayout,
        width: Int,
        height: Int,
    ) {
        val density = context.resources.displayMetrics.density
        when (layout) {
            MusicWidgetLayout.CONTROLS,
            MusicWidgetLayout.COMPACT,
            MusicWidgetLayout.WIDE,
            -> {
                // Keep 1-row pill widgets a sleek capsule vertically centered inside tall launcher rows.
                val targetPillHeight =
                    maxOf(if (layout == MusicWidgetLayout.WIDE) 74f else 68f, 20f + 34f * context.resources.configuration.fontScale)
                val verticalDp = ((height - targetPillHeight) / 2f).coerceAtLeast(0f)
                val verticalPx = (verticalDp * density).roundToInt()
                views.setViewPadding(android.R.id.background, 0, verticalPx, 0, verticalPx)
            }

            MusicWidgetLayout.SQUARE -> {
                // Keep 2x2 cards close to expressive squircle proportions on extra-tall 5x5 grids.
                val maxCardHeight = maxOf(width * 1.28f, 196f)
                val verticalDp = ((height - maxCardHeight) / 2f).coerceAtLeast(0f)
                val verticalPx = (verticalDp * density).roundToInt()
                views.setViewPadding(android.R.id.background, 0, verticalPx, 0, verticalPx)
            }

            MusicWidgetLayout.PLAYER -> {
                // Prevent extra-tall 2-row cells from creating empty space above the player content.
                val maxPlayerHeight = maxOf(204f, 110f + 45f * context.resources.configuration.fontScale)
                val verticalDp = ((height - maxPlayerHeight) / 2f).coerceAtLeast(0f)
                val verticalPx = (verticalDp * density).roundToInt()
                views.setViewPadding(android.R.id.background, 0, verticalPx, 0, verticalPx)
            }
        }
    }

    private fun widgetSizes(options: Bundle): List<Pair<Int, Int>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizes = BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            if (!sizes.isNullOrEmpty()) return sizes.map { it.width.toInt() to it.height.toInt() }.distinct()
        }
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
        return listOf(maxWidth to minHeight, minWidth to maxHeight)
    }

    private fun remoteViewsForSizes(
        options: Bundle,
        create: (Int, Int) -> RemoteViews,
    ): MusicWidgetRemoteViews {
        val sizes = widgetSizes(options)
        val useSizeMap =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !BundleCompat.getParcelableArrayList(options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java).isNullOrEmpty()
        return MusicWidgetRemoteViews(context, sizes, sizes.map { (width, height) -> create(width, height) }, useSizeMap)
    }

    private fun bindMusicControls(
        views: RemoteViews,
        layout: MusicWidgetLayout,
        controls: WidgetControls,
        isLiked: Boolean,
    ) {
        fun bind(
            id: Int,
            label: Int,
            enabled: Boolean,
            action: String,
            requestCode: Int,
        ) {
            views.setContentDescription(id, context.getString(label))
            views.setBoolean(id, "setEnabled", enabled)
            views.setOnClickPendingIntent(id, getMusicWidgetIntent(action, requestCode))
        }

        val playbackRequested = controls.playbackRequested
        val label = if (playbackRequested) R.string.pause else R.string.play
        bind(R.id.widget_prev_container, R.string.previous, controls.canPrevious, MusicWidgetReceiver.ACTION_PREVIOUS, 1)
        bind(R.id.widget_play_pause_container, label, controls.canPlayPause, MusicWidgetReceiver.ACTION_PLAY_PAUSE, 2)
        bind(R.id.widget_next_container, R.string.next, controls.canNext, MusicWidgetReceiver.ACTION_NEXT, 3)

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (playbackRequested) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        )

        if (layout == MusicWidgetLayout.PLAYER) {
            bind(
                R.id.widget_like_container,
                if (isLiked) R.string.unlike else R.string.action_like,
                controls.canLike,
                MusicWidgetReceiver.ACTION_LIKE,
                4,
            )
            views.setTextViewText(R.id.widget_play_pause_label, context.getString(label))
            views.setImageViewResource(
                R.id.widget_play_background,
                if (playbackRequested) R.drawable.widget_play_pill_selected_bg else R.drawable.widget_play_pill_bg,
            )
            if (isLiked) {
                views.setImageViewBitmap(R.id.widget_like_background, cookie9MaskBitmap())
            } else {
                views.setImageViewResource(R.id.widget_like_background, R.drawable.widget_circle_bg)
            }
            views.setImageViewResource(
                R.id.widget_like_button,
                if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav,
            )
        } else {
            views.setImageViewBitmap(R.id.widget_play_background, cookie12MaskBitmap())
        }
        views.setInt(R.id.widget_prev, "setImageAlpha", if (controls.canPrevious) 255 else 97)
        views.setInt(R.id.widget_next, "setImageAlpha", if (controls.canNext) 255 else 97)
        views.setInt(R.id.widget_play_pause, "setImageAlpha", if (controls.canPlayPause) 255 else 97)
        if (layout == MusicWidgetLayout.PLAYER) {
            views.setInt(R.id.widget_like_button, "setImageAlpha", if (controls.canLike) 255 else 97)
        }
        views.setOnClickPendingIntent(android.R.id.background, getOpenAppIntent())
    }

    private fun bindMusicColors(
        views: RemoteViews,
        layout: MusicWidgetLayout,
        isPlaying: Boolean,
        isLiked: Boolean,
        schemes: WidgetColorSchemes,
    ) {
        views.setWidgetTint(context, R.id.widget_surface_background, schemes, ::widgetSurfaceRole)

        when (layout) {
            MusicWidgetLayout.CONTROLS -> {
                views.setWidgetTint(context, R.id.widget_prev, schemes) { it.onSurface }
                views.setWidgetTint(context, R.id.widget_next, schemes) { it.onSurface }
                views.setWidgetTint(context, R.id.widget_play_background, schemes) {
                    if (isPlaying) it.primary else it.primaryContainer
                }
                views.setWidgetTint(context, R.id.widget_play_pause, schemes) {
                    if (isPlaying) it.onPrimary else it.onPrimaryContainer
                }
            }

            MusicWidgetLayout.COMPACT -> {
                views.setWidgetTextColor(context, R.id.widget_wide_song_title, schemes) { it.onSurface }
                views.setWidgetTextColor(context, R.id.widget_wide_artist_name, schemes) { it.onSurfaceVariant }
                views.setWidgetTint(context, R.id.widget_prev_background, schemes) { it.secondaryContainer }
                views.setWidgetTint(context, R.id.widget_next_background, schemes) { it.secondaryContainer }
                views.setWidgetTint(context, R.id.widget_prev, schemes) { it.onSecondaryContainer }
                views.setWidgetTint(context, R.id.widget_next, schemes) { it.onSecondaryContainer }
                views.setWidgetTint(context, R.id.widget_play_background, schemes) { it.primary }
                views.setWidgetTint(context, R.id.widget_play_pause, schemes) { it.onPrimary }
                views.setWidgetProgressTint(context, musicWidgetProgressIds, schemes)
            }

            MusicWidgetLayout.WIDE -> {
                views.setWidgetTextColor(context, R.id.widget_wide_song_title, schemes) { it.onSurface }
                views.setWidgetTextColor(context, R.id.widget_wide_artist_name, schemes) { it.onSurfaceVariant }
                views.setWidgetTint(context, R.id.widget_controls_pill_bg, schemes) { it.tertiaryContainer }
                views.setWidgetTint(context, R.id.widget_prev, schemes) { it.onTertiaryContainer }
                views.setWidgetTint(context, R.id.widget_next, schemes) { it.onTertiaryContainer }
                views.setWidgetTint(context, R.id.widget_play_background, schemes) { it.primary }
                views.setWidgetTint(context, R.id.widget_play_pause, schemes) { it.onPrimary }
            }

            MusicWidgetLayout.SQUARE -> {
                views.setWidgetTint(context, R.id.widget_prev_background, schemes) { it.tertiaryContainer }
                views.setWidgetTint(context, R.id.widget_next_background, schemes) { it.tertiaryContainer }
                views.setWidgetTint(context, R.id.widget_prev, schemes) { it.onTertiaryContainer }
                views.setWidgetTint(context, R.id.widget_next, schemes) { it.onTertiaryContainer }
                views.setWidgetTint(context, R.id.widget_play_background, schemes) {
                    if (isPlaying) it.primary else it.primaryContainer
                }
                views.setWidgetTint(context, R.id.widget_play_pause, schemes) {
                    if (isPlaying) it.onPrimary else it.onPrimaryContainer
                }
                views.setWidgetProgressTint(context, musicWidgetProgressIds, schemes)
                views.setWidgetTextColor(context, R.id.widget_square_time, schemes) { it.onSurfaceVariant }
                views.setWidgetTextColor(context, R.id.widget_square_song_title, schemes) { it.onSurface }
                views.setWidgetTextColor(context, R.id.widget_square_artist_name, schemes) { it.onSurfaceVariant }
            }

            MusicWidgetLayout.PLAYER -> {
                views.setWidgetTextColor(context, R.id.widget_song_title, schemes) { it.onSurface }
                views.setWidgetTextColor(context, R.id.widget_artist_name, schemes) { it.onSurfaceVariant }
                views.setWidgetTint(context, R.id.widget_prev_background, schemes) { it.tertiaryContainer }
                views.setWidgetTint(context, R.id.widget_next_background, schemes) { it.tertiaryContainer }
                views.setWidgetTint(context, R.id.widget_prev, schemes) { it.onTertiaryContainer }
                views.setWidgetTint(context, R.id.widget_next, schemes) { it.onTertiaryContainer }
                views.setWidgetTint(context, R.id.widget_play_background, schemes) { it.primary }
                views.setWidgetTint(context, R.id.widget_play_pause, schemes) { it.onPrimary }
                views.setWidgetTextColor(context, R.id.widget_play_pause_label, schemes) { it.onPrimary }
                views.setWidgetTint(context, R.id.widget_like_background, schemes) {
                    if (isLiked) it.tertiary else it.secondaryContainer
                }
                views.setWidgetTint(context, R.id.widget_like_button, schemes) {
                    if (isLiked) it.onTertiary else it.onSecondaryContainer
                }
                views.setWidgetProgressTint(context, musicWidgetProgressIds, schemes)
                views.setWidgetTextColor(context, R.id.widget_time_current, schemes) { it.onSurfaceVariant }
                views.setWidgetTextColor(context, R.id.widget_time_duration, schemes) { it.onSurfaceVariant }
            }
        }
    }

    private fun createPlayerRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        duration: Long,
        currentPosition: Long,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)
        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)
        views.setImageViewBitmap(R.id.widget_album_art, getCachedCookie9Art(albumArt) ?: cookie9DefaultIcon)
        if (duration > 0) {
            views.setViewVisibility(R.id.widget_progress_container, View.VISIBLE)
            views.setWidgetProgress(musicWidgetProgressIds, duration, currentPosition)
            views.setTextViewText(R.id.widget_time_current, formatWidgetDuration(context, currentPosition))
            views.setTextViewText(R.id.widget_time_duration, formatWidgetDuration(context, duration))
        } else {
            views.setViewVisibility(R.id.widget_progress_container, View.GONE)
        }
        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        return views
    }

    private fun createCompactSquareRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        duration: Long,
        currentPosition: Long,
        showArtwork: Boolean,
        schemes: WidgetColorSchemes,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_square)
        views.setViewVisibility(R.id.widget_square_album_art, if (showArtwork) View.VISIBLE else View.GONE)
        views.setImageViewBitmap(
            R.id.widget_square_album_art,
            getCachedWideCardArtwork(albumArt) ?: getWideCardDefaultIcon(schemes),
        )
        views.setTextViewText(R.id.widget_square_song_title, title)
        views.setTextViewText(R.id.widget_square_artist_name, artist)
        if (duration > 0) {
            views.setViewVisibility(R.id.widget_progress_container, View.VISIBLE)
            views.setViewVisibility(R.id.widget_square_idle_section, View.GONE)
            views.setWidgetProgress(musicWidgetProgressIds, duration, currentPosition)
            views.setTextViewText(R.id.widget_square_time, formatWidgetTimePair(context, currentPosition, duration))
        } else {
            views.setViewVisibility(R.id.widget_progress_container, View.GONE)
            views.setViewVisibility(R.id.widget_square_idle_section, View.VISIBLE)
        }
        views.setContentDescription(
            R.id.widget_square_album_art,
            context.getString(R.string.widget_track_description, title, artist),
        )
        views.setOnClickPendingIntent(R.id.widget_square_album_art, getOpenAppIntent())
        return views
    }

    private fun createCompactRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        duration: Long,
        currentPosition: Long,
        showArtwork: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        views.setViewVisibility(R.id.widget_wide_artwork, if (showArtwork) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_wide_song_title, title)
        views.setTextViewText(R.id.widget_wide_artist_name, artist)
        views.setImageViewBitmap(R.id.widget_wide_album_art, getCachedCookie12Art(albumArt) ?: cookie12DefaultIcon)
        if (duration > 0) {
            views.setViewVisibility(R.id.widget_progress_container, View.VISIBLE)
            views.setWidgetProgress(musicWidgetProgressIds, duration, currentPosition)
        } else {
            views.setViewVisibility(R.id.widget_progress_container, View.GONE)
        }
        views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent())
        return views
    }

    private fun createCompactWideRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        showArtwork: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)
        views.setViewVisibility(R.id.widget_wide_artwork, if (showArtwork) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_wide_song_title, title)
        views.setTextViewText(R.id.widget_wide_artist_name, artist)
        views.setImageViewBitmap(R.id.widget_wide_album_art, getCachedCookie9Art(albumArt) ?: cookie9DefaultIcon)
        views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent())
        return views
    }

    private suspend fun getCachedAlbumArt(artworkUri: String?): Bitmap? {
        if (artworkUri != null && artworkUri == cachedArtworkUri && cachedAlbumArt != null) return cachedAlbumArt
        val albumArt = artworkUri?.let { loadAlbumArt(it, 300) }
        cachedArtworkUri = artworkUri
        cachedAlbumArt = albumArt
        cachedCookie12Art = null
        cachedCookie9Art = null
        cachedWideCardArt = null
        cachedArtworkSeed = null
        return albumArt
    }

    private fun getCachedCookie12Art(albumArt: Bitmap?): Bitmap? =
        cachedCookie12Art ?: albumArt?.let(::getCookieBitmap)?.also { cachedCookie12Art = it }

    private fun getCachedCookie9Art(albumArt: Bitmap?): Bitmap? =
        cachedCookie9Art ?: albumArt?.let(::getCookie9Bitmap)?.also { cachedCookie9Art = it }

    private fun getCachedWideCardArtwork(albumArt: Bitmap?): Bitmap? =
        cachedWideCardArt ?: albumArt?.let(::getWideCardArtworkBitmap)?.also { cachedWideCardArt = it }

    private suspend fun loadAlbumArt(
        artworkUri: String,
        size: Int,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(artworkUri)
                        .size(size, size)
                        .allowHardware(false)
                        .crossfade(false)
                        .build()
                imageLoader.execute(request).image?.toBitmap()
            }.getOrNull()
        }

    private fun appIconBitmap(): Bitmap {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val bitmap = createBitmap(240, 240)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 240, 240)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getWideCardDefaultIcon(schemes: WidgetColorSchemes): Bitmap =
        createThemedPlaceholderBitmap(
            width = 280,
            height = 160,
            cornerRadius = 38f,
            backgroundColor = schemes.current(context).primaryContainer.toArgb(),
            iconColor = schemes.current(context).onPrimaryContainer.toArgb(),
        )

    private fun createThemedPlaceholderBitmap(
        width: Int,
        height: Int,
        cornerRadius: Float,
        backgroundColor: Int,
        iconColor: Int,
    ): Bitmap {
        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = backgroundColor
            }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), cornerRadius, cornerRadius, paint)
        AppCompatResources.getDrawable(context, R.drawable.ic_launcher_foreground)?.mutate()?.let { glyph ->
            glyph.setTint(iconColor)
            val iconSize = (minOf(width, height) * 0.9f).roundToInt()
            val left = (width - iconSize) / 2
            val top = (height - iconSize) / 2
            glyph.setBounds(left, top, left + iconSize, top + iconSize)
            glyph.draw(canvas)
        }
        return output
    }

    private fun getOpenAppIntent(): PendingIntent = openAppPendingIntent(context, 0)

    private fun getMusicWidgetIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent = widgetActionPendingIntent(context, action, requestCode)
}
