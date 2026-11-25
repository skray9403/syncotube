/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.shared.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.shared.constants.QueuePeekHeight
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.hours
import com.metrolist.shared.generated.resources.hours_and_minutes
import com.metrolist.shared.generated.resources.less_than_one_minute
import com.metrolist.shared.generated.resources.minutes
import com.metrolist.shared.generated.resources.sleep_timer
import com.metrolist.shared.generated.resources.sleep_timer_end_current_song_description
import com.metrolist.shared.generated.resources.sleep_timer_remaining
import com.metrolist.shared.generated.resources.volume
import com.metrolist.shared.playback.LocalPlayerSession
import com.metrolist.shared.playback.SleepTimerState
import com.metrolist.shared.ui.components.focusOutline
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.playback.retainPlaybackKeyFocus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Modifier.focusedPlayerBorder(
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.primary,
    borderWidth: Dp = 3.dp,
): Modifier = focusOutline(shape, color, borderWidth)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun PlayerBottomControls(
    sleepTimerState: SleepTimerState,
    onShowSleepTimer: () -> Unit,
    isShuffleOn: Boolean,
    onToggleShuffle: (() -> Unit)?,
    repeatMode: Int,
    onToggleRepeat: () -> Unit,
    navigationBarBottomPadding: Dp,
    onOpenQueue: () -> Unit,
    onShowPlayerMenu: () -> Unit,
    isLyricsVisible: Boolean,
    onIsLyricsVisibleChange: (Boolean) -> Unit,
    isVideoAvailable: Boolean = false,
    onFullscreenClick: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    onDeviceConnectClick: (() -> Unit)? = null,
    isDeviceConnectActive: Boolean = false,
    modifier: Modifier = Modifier,
    isListenTogetherGuest: Boolean = false,
    controlHeight: Dp = QueuePeekHeight,
    touchTarget: Dp = 40.dp,
    showQueueHandle: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = navigationBarBottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            onClick = { onIsLyricsVisibleChange(!isLyricsVisible) },
        ) {
            Text(if (isLyricsVisible) "Hide lyrics" else "Lyrics")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerVolumeControl() {
    val playerConnection = LocalPlayerSession.current ?: return
    val volume by playerConnection.volume.collectWhilePlayerActive()
    val sliderState = rememberSliderState(value = volume)
    sliderState.value = volume
    val volumeDescription = stringResource(Res.string.volume)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(MetrolistIcons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
        Slider(
            state = sliderState,
            onValueChange = playerConnection::setVolume,
            modifier = Modifier.retainPlaybackKeyFocus().weight(1f).semantics { contentDescription = volumeDescription },
        )
    }
}

internal fun shouldShowVideoButton(
    isListenTogetherGuest: Boolean,
    hideVideoSongs: Boolean,
    videoPlaybackEnabled: Boolean,
    songId: String?,
    musicVideoType: String?,
    isLocalSong: Boolean,
    isUploadedSong: Boolean,
): Boolean {
    if (isListenTogetherGuest) return false
    if (hideVideoSongs || !videoPlaybackEnabled) return false
    if (songId == null) return false
    if (isLocalSong || isUploadedSong || songId.startsWith("local_")) return false
    return true
}

@Composable
internal fun sleepTimerContentDescription(state: SleepTimerState): String {
    if (state.isActive && state.stopAfterCurrentSong) return stringResource(Res.string.sleep_timer_end_current_song_description)
    val remainingMs = state.remainingMs?.takeIf { state.isActive } ?: return stringResource(Res.string.sleep_timer)
    val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val duration =
        when {
            hours > 0L && minutes > 0L -> {
                stringResource(
                    Res.string.hours_and_minutes,
                    pluralStringResource(Res.plurals.hours, hours.toInt(), hours),
                    pluralStringResource(Res.plurals.minutes, minutes.toInt(), minutes),
                )
            }

            hours > 0L -> {
                pluralStringResource(Res.plurals.hours, hours.toInt(), hours)
            }

            totalMinutes > 0L -> {
                pluralStringResource(Res.plurals.minutes, totalMinutes.toInt(), totalMinutes)
            }

            else -> {
                stringResource(Res.string.less_than_one_minute)
            }
        }
    return stringResource(Res.string.sleep_timer_remaining, duration)
}
