package com.metrolist.android.ui.nectar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private enum class NectarPreviewRoute {
    Home,
    NowPlaying,
    Lyrics,
}

class NectarPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }
            var route by rememberSaveable { mutableStateOf(NectarPreviewRoute.Home) }
            var trackIndex by rememberSaveable { mutableIntStateOf(0) }
            var isPlaying by rememberSaveable { mutableStateOf(true) }
            var progress by rememberSaveable { mutableFloatStateOf(0.38f) }
            var isLiked by rememberSaveable { mutableStateOf(false) }
            var isShuffle by rememberSaveable { mutableStateOf(false) }
            var selectedDevice by rememberSaveable { mutableStateOf(NectarSampleData.outputDevices.first()) }

            val currentTrack = NectarSampleData.tracks[trackIndex]

            LaunchedEffect(isPlaying, trackIndex) {
                while (isPlaying) {
                    delay(1000L)
                    progress = (progress + (1f / currentTrack.durationSec)).let {
                        if (it >= 1f) 0f else it
                    }
                }
            }

            NectarTheme(darkTheme = isDarkTheme) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            initialOffsetY = { it / 6 },
                        ) + fadeIn()).togetherWith(
                            slideOutVertically(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetY = { -it / 12 },
                            ) + fadeOut(),
                        )
                    },
                    label = "nectarRoute",
                ) { activeRoute ->
                    when (activeRoute) {
                        NectarPreviewRoute.Home -> NectarHomeScreen(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            progress = progress,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            onTogglePlay = { isPlaying = !isPlaying },
                            onNextTrack = {
                                trackIndex = (trackIndex + 1) % NectarSampleData.tracks.size
                                progress = 0f
                            },
                            onSelectTrack = { track ->
                                trackIndex = NectarSampleData.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                progress = 0.1f
                                isPlaying = true
                                route = NectarPreviewRoute.NowPlaying
                            },
                            onOpenPlayer = { route = NectarPreviewRoute.NowPlaying },
                        )

                        NectarPreviewRoute.NowPlaying -> NectarNowPlayingScreen(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progress = progress,
                            isLiked = isLiked,
                            isShuffle = isShuffle,
                            selectedDevice = selectedDevice,
                            onBack = { route = NectarPreviewRoute.Home },
                            onOpenLyrics = { route = NectarPreviewRoute.Lyrics },
                            onTogglePlay = { isPlaying = !isPlaying },
                            onPrevious = {
                                trackIndex = if (trackIndex == 0) NectarSampleData.tracks.lastIndex else trackIndex - 1
                                progress = 0f
                            },
                            onNext = {
                                trackIndex = (trackIndex + 1) % NectarSampleData.tracks.size
                                progress = 0f
                            },
                            onSeek = { progress = it },
                            onToggleLike = { isLiked = !isLiked },
                            onToggleShuffle = { isShuffle = !isShuffle },
                            onSelectDevice = { selectedDevice = it },
                            onSelectTrack = { track ->
                                trackIndex = NectarSampleData.tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                progress = 0f
                                isPlaying = true
                            },
                        )

                        NectarPreviewRoute.Lyrics -> NectarLyricsScreen(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progress = progress,
                            onBack = { route = NectarPreviewRoute.NowPlaying },
                            onTogglePlay = { isPlaying = !isPlaying },
                            onSeekToSec = { sec ->
                                progress = (sec.toFloat() / currentTrack.durationSec).coerceIn(0f, 1f)
                            },
                        )
                    }
                }
            }
        }
    }
}
