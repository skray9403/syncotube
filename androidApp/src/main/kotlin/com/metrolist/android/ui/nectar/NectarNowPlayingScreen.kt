package com.metrolist.android.ui.nectar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.metrolist.shared.ui.icons.MetrolistIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NectarNowPlayingScreen(
    track: NectarTrack,
    isPlaying: Boolean,
    progress: Float,
    isLiked: Boolean,
    isShuffle: Boolean,
    selectedDevice: String,
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onSelectTrack: (NectarTrack) -> Unit,
) {
    var showDeviceSheet by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }

    val playCorner by animateDpAsState(
        targetValue = if (isPlaying) 20.dp else 32.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "playCorner",
    )
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale",
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse",
                        )
                    }
                },
                title = {
                    AssistChip(
                        onClick = { showDeviceSheet = true },
                        label = { Text(selectedDevice, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = MetrolistIcons.Outlined.Headphones,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onOpenLyrics,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Outlined.Lyrics,
                            contentDescription = "Lyrics",
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            val artSize = min(maxWidth, min(340.dp, maxHeight * 0.48f))

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.height(4.dp))

                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(artSize),
                ) {
                    NectarArtwork(
                        palette = track.palette,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalIconToggleButton(
                            checked = isLiked,
                            onCheckedChange = { onToggleLike() },
                            modifier = Modifier
                                .size(48.dp)
                                .scale(likeScale),
                        ) {
                            Icon(
                                imageVector = if (isLiked) MetrolistIcons.Rounded.Favorite else MetrolistIcons.Rounded.FavoriteBorder,
                                contentDescription = "Like",
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isPlaying) {
                            LinearWavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Slider(
                            value = progress,
                            onValueChange = onSeek,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val currentSec = (progress * track.durationSec).toInt()
                            Text(
                                text = formatDuration(currentSec),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = track.durationLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconToggleButton(
                            checked = isShuffle,
                            onCheckedChange = { onToggleShuffle() },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = MetrolistIcons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                            )
                        }
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = MetrolistIcons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                            )
                        }
                        FilledIconButton(
                            onClick = onTogglePlay,
                            shape = RoundedCornerShape(playCorner),
                            modifier = Modifier
                                .width(76.dp)
                                .height(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = MetrolistIcons.Rounded.SkipNext,
                                contentDescription = "Next",
                            )
                        }
                        IconButton(
                            onClick = { showQueueSheet = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = MetrolistIcons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Queue",
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeviceSheet) {
        ModalBottomSheet(onDismissRequest = { showDeviceSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Output device",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                NectarSampleData.outputDevices.forEach { device ->
                    ListItem(
                        headlineContent = { Text(device) },
                        supportingContent = {
                            if (device == selectedDevice) Text("Active", color = MaterialTheme.colorScheme.primary)
                        },
                        leadingContent = {
                            Icon(MetrolistIcons.Outlined.Headphones, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            onSelectDevice(device)
                            showDeviceSheet = false
                        },
                    )
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                NectarSampleData.tracks.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.artist) },
                        leadingContent = {
                            NectarArtwork(
                                palette = item.palette,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(48.dp),
                            )
                        },
                        trailingContent = {
                            Text(item.durationLabel, style = MaterialTheme.typography.labelMedium)
                        },
                        modifier = Modifier.clickable {
                            onSelectTrack(item)
                            showQueueSheet = false
                        },
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}
