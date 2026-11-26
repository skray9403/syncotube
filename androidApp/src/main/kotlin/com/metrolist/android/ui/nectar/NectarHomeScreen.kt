package com.metrolist.android.ui.nectar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.ui.icons.MetrolistIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NectarHomeScreen(
    currentTrack: NectarTrack,
    isPlaying: Boolean,
    progress: Float,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onTogglePlay: () -> Unit,
    onNextTrack: () -> Unit,
    onSelectTrack: (NectarTrack) -> Unit,
    onOpenPlayer: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val carouselState = rememberCarouselState { NectarSampleData.albums.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nectar",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Outlined.Settings,
                            contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NectarMiniPlayerBar(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    progress = progress,
                    onTogglePlay = onTogglePlay,
                    onNext = onNextTrack,
                    onClick = onOpenPlayer,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(12.dp))
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    val items = listOf(
                        Triple("Home", MetrolistIcons.Outlined.Home, 0),
                        Triple("Search", MetrolistIcons.Outlined.Search, 1),
                        Triple("Library", MetrolistIcons.Outlined.LibraryMusic, 2),
                    )
                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                ContinueListeningCard(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    progress = progress,
                    onCardClick = onOpenPlayer,
                    onPlayClick = onTogglePlay,
                )
            }

            item {
                Text(
                    text = "Quick picks",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            items(NectarSampleData.tracks, key = { it.id }) { track ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelectTrack(track) },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    leadingContent = {
                        NectarArtwork(
                            palette = track.palette,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(56.dp),
                        )
                    },
                    headlineContent = {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.durationLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(
                                onClick = { onSelectTrack(track) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = MetrolistIcons.Rounded.MoreVert,
                                    contentDescription = "More options",
                                )
                            }
                        }
                    },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Jump back in",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    HorizontalUncontainedCarousel(
                        state = carouselState,
                        itemWidth = 148.dp,
                        itemSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { index ->
                        val album = NectarSampleData.albums[index]
                        Column(
                            modifier = Modifier
                                .width(148.dp)
                                .clickable { onOpenPlayer() },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NectarArtwork(
                                palette = album.palette,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.size(148.dp),
                            )
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = album.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(
    track: NectarTrack,
    isPlaying: Boolean,
    progress: Float,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    Card(
        onClick = onCardClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NectarArtwork(
                palette = track.palette,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(96.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "CONTINUE LISTENING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
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
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FilledIconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
        }
    }
}

@Composable
private fun NectarMiniPlayerBar(
    track: NectarTrack,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
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
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
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
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(2.dp),
            )
        }
    }
}
