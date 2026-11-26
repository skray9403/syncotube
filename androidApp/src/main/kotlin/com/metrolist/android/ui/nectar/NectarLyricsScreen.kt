package com.metrolist.android.ui.nectar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.shared.ui.icons.MetrolistIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NectarLyricsScreen(
    track: NectarTrack,
    isPlaying: Boolean,
    progress: Float,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekToSec: (Int) -> Unit,
) {
    var isSyncedMode by rememberSaveable { mutableStateOf(true) }
    val currentSec = (progress * track.durationSec).toInt()
    val activeIndex = NectarSampleData.lyrics
        .indexOfLast { it.timestampSec <= currentSec }
        .coerceAtLeast(0)

    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex, isSyncedMode) {
        if (isSyncedMode) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Rounded.KeyboardArrowDown,
                            contentDescription = "Back",
                        )
                    }
                },
                title = {
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        SegmentedButton(
                            selected = isSyncedMode,
                            onClick = { isSyncedMode = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text("Synced", style = MaterialTheme.typography.labelMedium) },
                        )
                        SegmentedButton(
                            selected = !isSyncedMode,
                            onClick = { isSyncedMode = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("Plain", style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NectarArtwork(
                            palette = track.palette,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(44.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            itemsIndexed(NectarSampleData.lyrics) { index, line ->
                val isActive = isSyncedMode && index == activeIndex
                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onSeekToSec(line.timestampSec) },
                    ) {
                        Text(
                            text = line.text,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                } else {
                    Text(
                        text = line.text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSeekToSec(line.timestampSec) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
