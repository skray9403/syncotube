/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.android.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.android.LocalMiniPlayerBottomInset
import com.metrolist.android.LocalStartupPresentation
import com.metrolist.android.playback.LocalPlayerConnection
import com.metrolist.android.ui.component.AdaptiveModalBottomSheet
import com.metrolist.android.ui.component.tabContentTopPadding
import com.metrolist.android.ui.components.ErrorBox
import com.metrolist.android.ui.components.handleItemClick
import com.metrolist.android.ui.utils.playSong
import com.metrolist.android.viewmodels.HomeViewModel
import com.metrolist.shared.auth.AuthManager
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.error_occurred
import com.metrolist.shared.generated.resources.home_load_failed
import com.metrolist.shared.generated.resources.home_offline_empty
import com.metrolist.shared.generated.resources.keep_listening
import com.metrolist.shared.generated.resources.no_playable_items
import com.metrolist.shared.generated.resources.pause
import com.metrolist.shared.generated.resources.play
import com.metrolist.shared.generated.resources.pin_to_speed_dial
import com.metrolist.shared.generated.resources.player_not_ready
import com.metrolist.shared.generated.resources.quick_picks
import com.metrolist.shared.generated.resources.shuffle
import com.metrolist.shared.generated.resources.try_again
import com.metrolist.shared.generated.resources.unpin_from_speed_dial
import com.metrolist.shared.model.BrowseItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.model.isAvailableOffline
import com.metrolist.shared.settings.LibraryFilterType
import com.metrolist.shared.ui.components.ChipShelf
import com.metrolist.shared.ui.components.HomeLoadingPlaceholder
import com.metrolist.shared.ui.components.PopupVisibility
import com.metrolist.shared.ui.components.RefreshIndicator
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.navigation.LocalRootContentActive
import com.metrolist.shared.ui.screens.HomeContent
import com.metrolist.shared.ui.screens.HomeShelf
import com.metrolist.shared.ui.screens.homeContentText
import com.metrolist.shared.ui.wrapped.WrappedHomeCard
import com.metrolist.shared.youtube.playback.PlaybackNetworkStateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun HomeScreen(
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToPlaylist: (String, String?) -> Unit = { _, _ -> },
    onNavigateToPodcast: (String) -> Unit = {},
    onNavigateToWrapped: () -> Unit = {},
    onNavigateToBrowse: (String, String?, String) -> Unit = { _, _, _ -> },
    onNavigateToVideoBrowse: (String, String?, String) -> Unit = onNavigateToBrowse,
    onNavigateToHomeSection: (HomeShelf) -> Unit = {},
    onNavigateToLibrary: (LibraryFilterType?) -> Unit = {},
    floatingActionsBottomInset: State<Dp>,
    scrollToTopEvents: Flow<Long> = emptyFlow(),
    viewModel: HomeViewModel = koinViewModel<HomeViewModel>(),
) {
    val playerNotReadyText = stringResource(Res.string.player_not_ready)
    val noPlayableItemsText = stringResource(Res.string.no_playable_items)
    val shuffleText = stringResource(Res.string.shuffle)

    val rootActive = LocalRootContentActive.current
    val context = LocalContext.current
    val uiState by viewModel.shared.uiState.collectAsStateWithLifecycle()
    val startupPresentation = LocalStartupPresentation.current
    SideEffect { startupPresentation?.homeCacheReady = !uiState.isRestoringCache }
    val playerConnection = LocalPlayerConnection.current
    val currentSong = playerConnection?.currentSong?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val isPlaying = playerConnection?.isPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val database: MusicDatabase = koinInject()
    val scope = rememberCoroutineScope()
    val networkState by PlaybackNetworkStateStore.state.collectAsStateWithLifecycle()
    val cachedSongIds by remember(database) {
        database
            .dao()
            .cachedAudioIds()
            .map { it.toSet() }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = emptySet())
    val isOffline = !networkState.connected
    val isHomeItemEnabled: (YTItem) -> Boolean = { item ->
        !isOffline || item.isAvailableOffline(cachedSongIds)
    }

    val listState = rememberLazyListState()

    var lastScrollToTopEvent by rememberSaveable { mutableLongStateOf(-1L) }
    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect { event ->
            if (event != lastScrollToTopEvent) {
                lastScrollToTopEvent = event
                listState.scrollToItem(0)
            }
        }
    }

    var selectedMenuItem by remember { mutableStateOf<YTItem?>(null) }
    var showSpeedDialSurpriseMenu by remember { mutableStateOf(false) }
    val authManager = remember { AuthManager() }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFloatingActions by remember { mutableStateOf(true) }
    val fabNestedScrollConnection =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput) {
                        when {
                            available.y < -1f -> showFloatingActions = false
                            available.y > 1f -> showFloatingActions = true
                        }
                    }
                    return Offset.Zero
                }
            }
        }

    val selectedChip = uiState.selectedChip

    val miniPlayerInset = LocalMiniPlayerBottomInset.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val topPadding = tabContentTopPadding()

    fun shuffleHomeContent() {
        if (playerConnection == null) {
            Toast.makeText(context, playerNotReadyText, Toast.LENGTH_SHORT).show()
            return
        }
        val songs = uiState.loadedHomeSongs().filter(isHomeItemEnabled)
        if (songs.isEmpty()) {
            Toast.makeText(context, noPlayableItemsText, Toast.LENGTH_SHORT).show()
            return
        }
        playerConnection.shuffleQueue(
            songs,
            queueTitle = shuffleText,
        )
    }

    val homeContent: @Composable () -> Unit = {
        val sharedText = homeContentText()
        val onHomeItemClick: (YTItem) -> Unit = { item ->
            handleItemClick(
                item,
                onNavigateToAlbum,
                onNavigateToArtist,
                onNavigateToPlaylist,
                onNavigateToPodcast,
                playerConnection,
                scope,
                onNavigateToBrowse,
                startVideo = false,
            )
        }
        HomeContent(
            uiState = uiState,
            shelfCache = viewModel.shared.shelfCache,
            onLoadMore = viewModel.shared::loadMoreSections,
            text = sharedText,
            onVideoItemClick = { item ->
                when (item) {
                    is SongItem -> playSong(item, playerConnection, scope)
                    is BrowseItem -> onNavigateToVideoBrowse(item.id, item.params, item.title)
                    else -> Unit
                }
            },
            onItemClick = onHomeItemClick,
            onSpeedDialItemClick = { item ->
                if (item is SongItem) {
                    playerConnection?.startRadio(item)
                } else {
                    onHomeItemClick(item)
                }
            },
            onItemLongClick = { selectedMenuItem = it },
            onShuffleSongs = { songs -> playerConnection?.shuffleQueue(songs, queueTitle = sharedText.shuffle) },
            onPlaySongs = { songs -> playerConnection?.playQueue(songs, queueTitle = sharedText.quickPicks) },
            onLibraryClick = onNavigateToLibrary,
            onLocalShelfClick = onNavigateToHomeSection,
            onArtistClick = { artistId -> onNavigateToArtist(artistId) },
            onPlayAlbum = { album ->
                playerConnection?.playAlbum(album.id, album.title)
            },
            isItemEnabled = isHomeItemEnabled,
            onShowSurpriseMenu = { showSpeedDialSurpriseMenu = true },
            currentSongId = remember(currentSong) { derivedStateOf { currentSong.value?.id } },
            isPlaying = isPlaying,
            isLandscape = isLandscape,
            speedDialSurpriseAtEndOfFirstPage = true,
            listState = listState,
            contentPadding =
                PaddingValues(
                    top = topPadding,
                    bottom = miniPlayerInset + 16.dp,
                ),
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        startupPresentation?.homeLaidOut = true
                    }.focusGroup()
                    .focusRestorer(),
            header = {
                when {
                    uiState.showLoadingPlaceholder -> {
                        item(key = "home_loading") {
                            HomeLoadingPlaceholder(isLandscape = isLandscape)
                        }
                    }

                    isOffline && uiState.hasNoHomeContent() -> {
                        item(key = "home_offline") {
                            Text(stringResource(Res.string.home_offline_empty), modifier = Modifier.padding(24.dp))
                        }
                    }

                    !isOffline && !uiState.error.isNullOrBlank() -> {
                        item {
                            Column(
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Text(stringResource(Res.string.error_occurred))
                                ErrorBox(stringResource(Res.string.home_load_failed))
                                TextButton(onClick = { viewModel.shared.refresh {} }, shapes = ButtonDefaults.shapes()) {
                                    Text(stringResource(Res.string.try_again))
                                }
                            }
                        }
                    }

                    else -> {
                        val featuredSong =
                            currentSong.value
                                ?: uiState.keepListening.firstOrNull(isHomeItemEnabled)
                                ?: uiState.quickPicks.firstOrNull(isHomeItemEnabled)
                        if (featuredSong != null) {
                            item(key = "featured_listening") {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    val quickPickPreview =
                                        uiState.quickPicks
                                            .filter { it.id != featuredSong.id && isHomeItemEnabled(it) }
                                            .take(3)
                                    if (maxWidth >= 800.dp && quickPickPreview.isNotEmpty()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            FeaturedListeningCard(
                                                song = featuredSong,
                                                isPlaying = featuredSong.id == currentSong.value?.id && isPlaying.value,
                                                onPlay = {
                                                    if (featuredSong.id == currentSong.value?.id) {
                                                        playerConnection?.togglePlayPause()
                                                    } else {
                                                        playSong(featuredSong, playerConnection, scope)
                                                    }
                                                },
                                                modifier = Modifier.weight(1.35f),
                                            )
                                            QuickPickPreview(
                                                songs = quickPickPreview,
                                                onSelect = { playSong(it, playerConnection, scope) },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    } else {
                                        FeaturedListeningCard(
                                            song = featuredSong,
                                            isPlaying = featuredSong.id == currentSong.value?.id && isPlaying.value,
                                            onPlay = {
                                                if (featuredSong.id == currentSong.value?.id) {
                                                    playerConnection?.togglePlayPause()
                                                } else {
                                                    playSong(featuredSong, playerConnection, scope)
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            ChipShelf(
                                options = uiState.chips,
                                selectedOption = selectedChip,
                                onSelectionChange = viewModel.shared::toggleChip,
                                label = { it.title },
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        item(key = "wrapped_card") {
                            WrappedHomeCard(
                                onClick = onNavigateToWrapped,
                                onDismissed = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() },
                            )
                        }
                    }
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().nestedScroll(fabNestedScrollConnection),
    ) {
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.shared.refresh { isRefreshing = false }
            },
            state = pullToRefreshState,
            indicator = {
                RefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topPadding),
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            homeContent()
        }

        AnimatedVisibility(
            visible = showFloatingActions,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = floatingActionsBottomInset.value,
                    ),
        ) {
            FloatingActionButton(
                onClick = ::shuffleHomeContent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = MetrolistIcons.Rounded.Shuffle,
                    contentDescription = stringResource(Res.string.shuffle),
                )
            }
        }
    }

    HomeItemMenu(
        selectedItem = selectedMenuItem,
        onDismiss = { selectedMenuItem = null },
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToPodcast = onNavigateToPodcast,
        onSongMetadataUpdated = viewModel.shared::updateSongMetadata,
        active = rootActive,
    )

    PopupVisibility(visible = rootActive && showSpeedDialSurpriseMenu) {
        HomeSpeedDialSurpriseMenu(
            isPinned = uiState.speedDialSurprisePinned,
            onDismiss = { showSpeedDialSurpriseMenu = false },
            onSetPinned = viewModel.shared::setSpeedDialSurprisePinned,
        )
    }
}

@Composable
private fun FeaturedListeningCard(
    song: SongItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 2.dp,
    ) {
        BoxWithConstraints {
            val artworkSize = if (maxWidth < 360.dp) 88.dp else 104.dp
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImage(
                    url = song.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(artworkSize).clip(MaterialTheme.shapes.large),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.keep_listening),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artists.joinToString { it.name }.ifBlank { song.album?.name.orEmpty() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FilledTonalButton(
                        onClick = onPlay,
                        shapes = ButtonDefaults.shapesFor(40.dp),
                        contentPadding = ButtonDefaults.contentPaddingFor(40.dp),
                    ) {
                        Icon(
                            if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (isPlaying) Res.string.pause else Res.string.play))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPickPreview(
    songs: List<SongItem>,
    onSelect: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.quick_picks),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            songs.forEach { song ->
                Surface(
                    onClick = { onSelect(song) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            url = song.thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = MetrolistIcons.Rounded.PlayArrow,
                            contentDescription = stringResource(Res.string.play),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeSpeedDialSurpriseMenu(
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onSetPinned: (Boolean) -> Unit,
) {
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(Res.string.shuffle),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            ListItem(
                leadingContent = {
                    Icon(
                        if (isPinned) MetrolistIcons.Outlined.KeepOff else MetrolistIcons.Filled.PushPin,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier.clickable {
                        onSetPinned(!isPinned)
                        onDismiss()
                    },
            ) {
                Text(stringResource(if (isPinned) Res.string.unpin_from_speed_dial else Res.string.pin_to_speed_dial))
            }
        }
    }
}
