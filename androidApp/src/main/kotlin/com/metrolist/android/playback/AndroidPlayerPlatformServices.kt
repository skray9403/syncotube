package com.metrolist.android.playback

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.metrolist.android.ui.theme.PlayerColorExtractor
import com.metrolist.android.utils.artworkUiVisible
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.shared.api.artwork.CanvasContentSource
import com.metrolist.shared.api.artwork.DirectCanvasProvider
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.AnimatedCoverResolution
import com.metrolist.shared.settings.MusicVideoQuality
import com.metrolist.shared.settings.YouTubeCanvasMode
import com.metrolist.shared.ui.theme.DefaultThemeColor
import com.metrolist.shared.utils.canonicalArtworkUrl
import com.metrolist.shared.viewmodels.PlayerArtworkColors
import com.metrolist.shared.viewmodels.PlayerPlatformServices
import com.metrolist.shared.viewmodels.PlayerViewModel.MusicVideoCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlayerPlatformServices(
    private val context: Context,
    private val imageLoader: ImageLoader,
    @Suppress("UNUSED_PARAMETER") private val directCanvasProvider: DirectCanvasProvider,
) : PlayerPlatformServices {
    override val supportsCanvasPreloading = false
    override val artworkVisible = artworkUiVisible(context)

    override fun onMemoryPressure(trim: () -> Unit) {
        context.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                @Deprecated("Required by ComponentCallbacks on Android versions before API 34")
                override fun onLowMemory() = trim()

                @Suppress("DEPRECATION")
                override fun onTrimMemory(level: Int) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) trim()
                }
            },
        )
    }

    override suspend fun extractArtworkColors(
        songId: String,
        thumbnailUrl: String,
    ): PlayerArtworkColors? = PlayerColorExtractor.extractColors(context, imageLoader, thumbnailUrl, DefaultThemeColor)

    override fun shareText(text: String) {
        val intent =
            Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Metrolist", text))
    }

    override suspend fun spotifyCanvas(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long?,
        primaryArtist: String?,
    ): String? = null

    override fun musicVideoCanvasHints(song: SongItem): ContentHints = song.toAndroidContentHints(wantVideo = false)

    override suspend fun cachedAnimatedCover(
        songId: String,
        source: CanvasContentSource,
        resolution: AnimatedCoverResolution,
        tall: Boolean,
    ): String? = null

    override suspend fun cachedMusicVideoCanvas(
        songId: String,
        mode: YouTubeCanvasMode,
        quality: MusicVideoQuality,
    ): MusicVideoCanvas? = null

    override suspend fun prepareMusicVideoCanvas(
        songId: String,
        canvas: MusicVideoCanvas,
        mode: YouTubeCanvasMode,
        quality: MusicVideoQuality,
        preload: Boolean,
    ): MusicVideoCanvas = canvas

    override suspend fun preloadAnimatedCover(
        songId: String,
        source: CanvasContentSource,
        url: String,
        tall: Boolean,
    ): String = url
}
