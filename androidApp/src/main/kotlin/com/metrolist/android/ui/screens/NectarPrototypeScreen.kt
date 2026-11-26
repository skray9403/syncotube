@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.shared.ui.icons.MetrolistIcons
import kotlinx.coroutines.delay

private data class NectarDemoTrack(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val artworkColor: Color,
    val downloaded: Boolean,
    val lyrics: List<Pair<Int, String>>,
)

private val nectarDemoTracks =
    listOf(
        NectarDemoTrack(
            "Bairan",
            "Banjaare",
            224,
            Color(0xFFDFD2F6),
            true,
            listOf(
                0 to "Instrumental intro · Acoustic guitar",
                18 to "Walking past the quiet station lights",
                34 to "Carrying echoes from the monsoon night",
                42 to "Every mile we left behind still calls your name",
                56 to "Through the static and the silver rain",
                72 to "Hold the lantern steady by the window sill",
                88 to "Even distant rivers find the sea again",
                108 to "Bairan winds are turning toward the dawn",
            ),
        ),
        NectarDemoTrack(
            "Driven",
            "Kiasmos",
            268,
            Color(0xFFD7E3FF),
            true,
            listOf(
                0 to "Minimal analog pulse · 122 BPM",
                24 to "Northern horizon under pale daylight",
                48 to "Soft percussion over open glass",
                76 to "Everything settles into steady motion",
                110 to "Clear signal across the water",
            ),
        ),
        NectarDemoTrack(
            "Open Eye Signal",
            "Jon Hopkins",
            312,
            Color(0xFFFFD8E4),
            false,
            listOf(
                0 to "Sub-bass harmonic swell",
                30 to "First light breaking over concrete",
                62 to "Unbroken rhythm in the morning air",
                95 to "No noise, only pure frequency",
            ),
        ),
    )

private data class NectarDemoPeer(
    val name: String,
    val route: String,
    val deviceType: String,
    val reachable: Boolean,
)

private val nectarDemoPeers =
    listOf(
        NectarDemoPeer("ASUS Vivobook", "Wi-Fi · QUIC (+1 route)", "computer", true),
        NectarDemoPeer("Linux Workstation", "NectarLink · QUIC", "computer", true),
        NectarDemoPeer("Pixel 6a", "Bluetooth", "phone", false),
    )

/**
 * Native Material 3 Expressive UI prototype. All interactions use local demo state.
 * Real playback, account, search and device-connect services are intentionally not
 * invoked here; those will be wired after the visual design is approved.
 */
@Composable
fun NectarPrototypeScreen(initialTab: String = "home") {
    val nectarColors =
        remember {
            lightColorScheme(
                primary = Color(0xFF6750A4),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEADDFF),
                onPrimaryContainer = Color(0xFF21005D),
                secondaryContainer = Color(0xFFE8DEF8),
                onSecondaryContainer = Color(0xFF1D192B),
                tertiaryContainer = Color(0xFFFFD8E4),
                onTertiaryContainer = Color(0xFF31111D),
                background = Color(0xFFFCF8F8),
                onBackground = Color(0xFF1D1B20),
                surface = Color(0xFFFCF8F8),
                onSurface = Color(0xFF1D1B20),
                surfaceVariant = Color(0xFFEAE4E8),
                onSurfaceVariant = Color(0xFF49454F),
                outline = Color(0xFF79747E),
                outlineVariant = Color(0xFFCAC4D0),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF7F2F4),
                surfaceContainer = Color(0xFFF4F0F2),
                surfaceContainerHigh = Color(0xFFEAE4E8),
                surfaceContainerHighest = Color(0xFFE2DCE0),
            )
        }

    MaterialExpressiveTheme(
        colorScheme = nectarColors,
        motionScheme = MotionScheme.expressive(),
    ) {
        NectarPrototypeContent(initialTab)
    }
}

@Composable
private fun NectarPrototypeContent(initialTab: String) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var showingSettings by rememberSaveable { mutableStateOf(false) }
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var lyricsMode by rememberSaveable { mutableStateOf(false) }
    var castSheetVisible by rememberSaveable { mutableStateOf(false) }
    var activePeerIndex by rememberSaveable { mutableIntStateOf(-1) }
    var trackIndex by rememberSaveable { mutableIntStateOf(0) }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var positionSeconds by rememberSaveable { mutableIntStateOf(42) }
    var shuffleEnabled by rememberSaveable { mutableStateOf(false) }
    var audioEnabled by rememberSaveable { mutableStateOf(true) }
    var appearanceEnabled by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchFilter by rememberSaveable { mutableStateOf("Songs") }
    var libraryFilter by rememberSaveable { mutableStateOf("Playlists") }
    val likedTrackIndexes = remember { mutableStateListOf(0, 2) }

    val track = nectarDemoTracks[trackIndex]
    val activePeer = nectarDemoPeers.getOrNull(activePeerIndex)
    val activeLyricIndex = track.lyrics.indexOfLast { it.first <= positionSeconds }.coerceAtLeast(0)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isPlaying, trackIndex) {
        while (isPlaying) {
            delay(1000)
            val duration = nectarDemoTracks[trackIndex].durationSeconds
            positionSeconds = if (positionSeconds + 1 >= duration) 0 else positionSeconds + 1
        }
    }

    BackHandler(enabled = playerExpanded || castSheetVisible || showingSettings) {
        when {
            castSheetVisible -> castSheetVisible = false
            playerExpanded -> playerExpanded = false
            showingSettings -> showingSettings = false
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 164.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when {
                showingSettings -> NectarSettingsScreen(
                    audioEnabled = audioEnabled,
                    onAudioEnabledChange = { audioEnabled = it },
                    appearanceEnabled = appearanceEnabled,
                    onAppearanceEnabledChange = { appearanceEnabled = it },
                    onBack = { showingSettings = false },
                    onOpenCast = { castSheetVisible = true },
                )

                selectedTab == "search" -> NectarSearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    selectedFilter = searchFilter,
                    onFilterChange = { searchFilter = it },
                    onPlayTrack = { index ->
                        trackIndex = index
                        positionSeconds = 0
                        isPlaying = true
                    },
                )

                selectedTab == "library" -> NectarLibraryScreen(
                    filter = libraryFilter,
                    onFilterChange = { libraryFilter = it },
                    likedTrackIndexes = likedTrackIndexes,
                    onPlayTrack = { index ->
                        trackIndex = index
                        positionSeconds = 0
                        isPlaying = true
                    },
                )

                else -> NectarHomeScreen(
                    onOpenSettings = { showingSettings = true },
                    onPlayTrack = { index ->
                        trackIndex = index
                        positionSeconds = 0
                        isPlaying = true
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = !playerExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 76.dp),
        ) {
            NectarMiniPlayer(
                track = track,
                isPlaying = isPlaying,
                activePeer = activePeer,
                onOpenPlayer = { playerExpanded = true },
                onOpenCast = { castSheetVisible = true },
                onTogglePlay = { isPlaying = !isPlaying },
                onNext = {
                    trackIndex = (trackIndex + 1) % nectarDemoTracks.size
                    positionSeconds = 0
                    isPlaying = true
                },
                onReturnToPhone = { activePeerIndex = -1 },
            )
        }

        AnimatedVisibility(
            visible = !playerExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
        ) {
            NectarFloatingNavigation(
                selectedTab = if (showingSettings) "" else selectedTab,
                onSelectTab = {
                    showingSettings = false
                    selectedTab = it
                },
            )
        }

        AnimatedVisibility(
            visible = playerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            NectarFullPlayer(
                track = track,
                isPlaying = isPlaying,
                positionSeconds = positionSeconds,
                lyricsMode = lyricsMode,
                currentLyricIndex = activeLyricIndex,
                shuffleEnabled = shuffleEnabled,
                isLiked = likedTrackIndexes.contains(trackIndex),
                activePeer = activePeer,
                onClose = { playerExpanded = false },
                onOpenCast = { castSheetVisible = true },
                onToggleLyrics = { lyricsMode = !lyricsMode },
                onTogglePlay = { isPlaying = !isPlaying },
                onSeek = { positionSeconds = it.coerceIn(0, track.durationSeconds) },
                onSeekToLyric = {
                    positionSeconds = it
                    isPlaying = true
                },
                onToggleLike = {
                    if (likedTrackIndexes.contains(trackIndex)) {
                        likedTrackIndexes.remove(trackIndex)
                    } else {
                        likedTrackIndexes.add(trackIndex)
                    }
                },
                onToggleShuffle = { shuffleEnabled = !shuffleEnabled },
                onPrevious = {
                    trackIndex = (trackIndex - 1 + nectarDemoTracks.size) % nectarDemoTracks.size
                    positionSeconds = 0
                    isPlaying = true
                },
                onNext = {
                    trackIndex = (trackIndex + 1) % nectarDemoTracks.size
                    positionSeconds = 0
                    isPlaying = true
                },
            )
        }

        if (castSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { castSheetVisible = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
            ) {
                NectarCastSheet(
                    activePeerIndex = activePeerIndex,
                    onConnect = { index ->
                        activePeerIndex = index
                        isPlaying = true
                        castSheetVisible = false
                    },
                    onReturnToPhone = { activePeerIndex = -1 },
                )
            }
        }
    }
}

@Composable
private fun NectarHomeScreen(
    onOpenSettings: () -> Unit,
    onPlayTrack: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Nectar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onOpenSettings) {
                Icon(MetrolistIcons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NectarArtwork(nectarDemoTracks[0].artworkColor, 96, 36)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        "CONTINUE LISTENING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Text("Bairan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Banjaare", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onPlayTrack(0) }, shape = CircleShape) {
                        Icon(MetrolistIcons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Quick Picks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("YouTube Music", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            nectarDemoTracks.drop(1).forEachIndexed { index, item ->
                NectarTrackRow(item, nectarFormatTime(item.durationSeconds), { onPlayTrack(index + 1) })
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Jump Back In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 2.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(nectarDemoTracks) { index, item ->
                    Column(
                        modifier = Modifier.width(128.dp).clickable { onPlayTrack(index) },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NectarArtwork(item.artworkColor, 128, 34)
                        Text(item.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(item.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun NectarSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onPlayTrack: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(MetrolistIcons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search YouTube Music", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        MetrolistIcons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onQueryChange("") },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter) },
                    shape = CircleShape,
                )
            }
        }

        val results =
            nectarDemoTracks.withIndex().filter { (_, item) ->
                query.isBlank() || item.title.contains(query, ignoreCase = true) || item.artist.contains(query, ignoreCase = true)
            }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            results.forEach { (index, item) ->
                NectarTrackRow(item, nectarFormatTime(item.durationSeconds), { onPlayTrack(index) }, showContainer = false)
            }
            if (results.isEmpty()) {
                Text("No demo results", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun NectarLibraryScreen(
    filter: String,
    onFilterChange: (String) -> Unit,
    likedTrackIndexes: List<Int>,
    onPlayTrack: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape) {
                Text("Local & Synced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Playlists", "Downloaded", "Liked").forEach { item ->
                FilterChip(selected = filter == item, onClick = { onFilterChange(item) }, label = { Text(item) }, shape = CircleShape)
            }
        }

        val indexes =
            nectarDemoTracks.indices.filter { index ->
                when (filter) {
                    "Downloaded" -> nectarDemoTracks[index].downloaded
                    "Liked" -> likedTrackIndexes.contains(index)
                    else -> true
                }
            }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            indexes.forEach { index ->
                NectarTrackRow(
                    nectarDemoTracks[index],
                    if (nectarDemoTracks[index].downloaded) "Saved" else "",
                    { onPlayTrack(index) },
                )
            }
        }
    }
}

@Composable
private fun NectarSettingsScreen(
    audioEnabled: Boolean,
    onAudioEnabledChange: (Boolean) -> Unit,
    appearanceEnabled: Boolean,
    onAppearanceEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenCast: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(MetrolistIcons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Normal, modifier = Modifier.padding(horizontal = 2.dp))
        }

        NectarSettingsGroup {
            NectarPreferenceRow(
                MetrolistIcons.Outlined.Person,
                Color(0xFFEADDFF),
                Color(0xFF21005D),
                "Account & InnerTube",
                "Signed in · PoToken & SABR active",
            )
            NectarSettingsDivider()
            NectarPreferenceRow(
                MetrolistIcons.Outlined.Cast,
                Color(0xFFD7E3FF),
                Color(0xFF001B3F),
                "NectarCast & Device Connect",
                "Wi-Fi mDNS · Bluetooth · NectarLink",
                onClick = onOpenCast,
            )
        }

        NectarSettingsGroup {
            NectarPreferenceRow(
                MetrolistIcons.Filled.MusicNote,
                Color(0xFFFFD8E4),
                Color(0xFF31111D),
                "Audio & Playback",
                "Opus 251 · Normalization on",
                trailing = { Switch(checked = audioEnabled, onCheckedChange = onAudioEnabledChange) },
                onClick = { onAudioEnabledChange(!audioEnabled) },
            )
            NectarSettingsDivider()
            NectarPreferenceRow(
                MetrolistIcons.Outlined.Palette,
                Color(0xFFD8E7CB),
                Color(0xFF131F0D),
                "Appearance",
                "Expressive Light · Static artwork",
                trailing = { Switch(checked = appearanceEnabled, onCheckedChange = onAppearanceEnabledChange) },
                onClick = { onAppearanceEnabledChange(!appearanceEnabled) },
            )
        }

        NectarSettingsGroup {
            NectarPreferenceRow(
                MetrolistIcons.Outlined.Storage,
                Color(0xFFFBE0B6),
                Color(0xFF281800),
                "Storage & Downloads",
                "342 MB cached · 2.0 GB limit",
            )
            NectarSettingsDivider()
            NectarPreferenceRow(
                MetrolistIcons.Outlined.Backup,
                Color(0xFFE8DEF8),
                Color(0xFF1D192B),
                "Backup & Restore",
                "Export or restore local library database",
            )
        }
    }
}

@Composable
private fun NectarSettingsGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun NectarSettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.background, thickness = 1.dp, modifier = Modifier.padding(horizontal = 14.dp))
}

@Composable
private fun NectarPreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconContentColor: Color,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(color = iconColor, shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconContentColor, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        trailing?.invoke()
    }
}

@Composable
private fun NectarTrackRow(
    track: NectarDemoTrack,
    trailingText: String,
    onClick: () -> Unit,
    showContainer: Boolean = true,
) {
    Surface(
        color = if (showContainer) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NectarArtwork(track.artworkColor, 44, 20, 14)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (trailingText.isNotEmpty()) {
                Text(trailingText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
        }
    }
}

@Composable
private fun NectarArtwork(
    color: Color,
    size: Int,
    iconSize: Int,
    corner: Int = 24,
) {
    Surface(color = color, shape = RoundedCornerShape(corner.dp), modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(MetrolistIcons.Filled.MusicNote, contentDescription = null, tint = Color(0xFF6750A4), modifier = Modifier.size(iconSize.dp))
        }
    }
}

@Composable
private fun NectarMiniPlayer(
    track: NectarDemoTrack,
    isPlaying: Boolean,
    activePeer: NectarDemoPeer?,
    onOpenPlayer: () -> Unit,
    onOpenCast: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onReturnToPhone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (activePeer != null) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(7.dp)) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        activePeer.name + " · " + activePeer.route.substringBefore(" (+"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    TextButton(onClick = onReturnToPhone) { Text("Return", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape =
                if (activePeer == null) RoundedCornerShape(18.dp)
                else RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenPlayer),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NectarArtwork(track.artworkColor, 44, 20, 12)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(track.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(track.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                IconButton(onClick = onOpenCast, modifier = Modifier.size(38.dp)) {
                    Icon(MetrolistIcons.Rounded.Cast, contentDescription = "Cast", tint = if (activePeer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = if (isPlaying) CircleShape else RoundedCornerShape(14.dp),
                    modifier = Modifier.size(width = if (isPlaying) 44.dp else 40.dp, height = 40.dp).clickable(onClick = onTogglePlay),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(MetrolistIcons.Rounded.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NectarFloatingNavigation(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = CircleShape,
        shadowElevation = 6.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(modifier = Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            NectarNavigationItem("home", "Home", MetrolistIcons.Filled.Home, selectedTab, onSelectTab)
            NectarNavigationItem("search", "Search", MetrolistIcons.Filled.Search, selectedTab, onSelectTab)
            NectarNavigationItem("library", "Library", MetrolistIcons.Filled.LibraryMusic, selectedTab, onSelectTab)
        }
    }
}

@Composable
private fun NectarNavigationItem(
    tab: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
) {
    val selected = selectedTab == tab
    val foreground = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.clickable { onSelectTab(tab) }.animateContentSize(spring()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(17.dp))
            Text(label, color = foreground, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

@Composable
private fun NectarFullPlayer(
    track: NectarDemoTrack,
    isPlaying: Boolean,
    positionSeconds: Int,
    lyricsMode: Boolean,
    currentLyricIndex: Int,
    shuffleEnabled: Boolean,
    isLiked: Boolean,
    activePeer: NectarDemoPeer?,
    onClose: () -> Unit,
    onOpenCast: () -> Unit,
    onToggleLyrics: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onSeekToLyric: (Int) -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onClose) {
                Icon(MetrolistIcons.Filled.ChevronLeft, contentDescription = "Collapse player")
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = CircleShape, modifier = Modifier.clickable(onClick = onOpenCast)) {
                Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(MetrolistIcons.Rounded.Cast, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(activePeer?.name ?: "This Phone", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
            Surface(
                color = if (lyricsMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                shape = CircleShape,
                modifier = Modifier.clickable(onClick = onToggleLyrics),
            ) {
                Text(
                    "Lyrics",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lyricsMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }

        AnimatedContent(targetState = lyricsMode, modifier = Modifier.weight(1f).fillMaxWidth(), label = "player-content") { showLyrics ->
            if (showLyrics) {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(track.lyrics) { index, line ->
                        val active = index == currentLyricIndex
                        val scale by animateFloatAsState(
                            targetValue = if (active) 1f else 0.93f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                            label = "lyric-scale",
                        )
                        val alpha by animateFloatAsState(targetValue = if (active) 1f else 0.35f, label = "lyric-alpha")
                        Surface(
                            color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                    }
                                    .clickable { onSeekToLyric(line.first) },
                        ) {
                            Text(
                                line.second,
                                style = if (active) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(color = track.artworkColor, shape = RoundedCornerShape(30.dp), modifier = Modifier.size(276.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Surface(color = Color.White.copy(alpha = 0.55f), shape = RoundedCornerShape(28.dp), modifier = Modifier.size(88.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(MetrolistIcons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("OPUS 251 · STATIC ART", style = MaterialTheme.typography.labelSmall, color = Color(0xFF21005D).copy(alpha = 0.7f), letterSpacing = 1.2.sp)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        if (isLiked) MetrolistIcons.Filled.Favorite else MetrolistIcons.Filled.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Slider(
                value = positionSeconds.toFloat().coerceIn(0f, track.durationSeconds.toFloat()),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..track.durationSeconds.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(nectarFormatTime(positionSeconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(nectarFormatTime(track.durationSeconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(MetrolistIcons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPrevious) {
                    Icon(MetrolistIcons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(28.dp))
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = if (isPlaying) CircleShape else RoundedCornerShape(22.dp),
                    modifier = Modifier.size(width = if (isPlaying) 94.dp else 82.dp, height = 58.dp).clickable(onClick = onTogglePlay),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(MetrolistIcons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onOpenCast) {
                    Icon(MetrolistIcons.Rounded.Cast, contentDescription = "Cast", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NectarCastSheet(
    activePeerIndex: Int,
    onConnect: (Int) -> Unit,
    onReturnToPhone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Connect to a device", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Wi-Fi, Bluetooth & NectarLink deduplicated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = {}) { Text("Rescan") }
        }

        if (activePeerIndex >= 0) {
            Button(onClick = onReturnToPhone, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(MetrolistIcons.Outlined.PhoneAndroid, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Switch playback back to this phone")
            }
        }

        nectarDemoPeers.forEachIndexed { index, peer ->
            val active = activePeerIndex == index
            Surface(
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().then(if (peer.reachable) Modifier.clickable { onConnect(index) } else Modifier),
            ) {
                Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (peer.deviceType == "computer") MetrolistIcons.Outlined.Computer else MetrolistIcons.Outlined.PhoneAndroid,
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(peer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(peer.route, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(
                        when {
                            active -> "Active"
                            peer.reachable -> "Connect"
                            else -> "Beacon"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (peer.reachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Text(
            "Demo devices only. Device discovery and transport are not connected.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun nectarFormatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    return (safeSeconds / 60).toString() + ":" + (safeSeconds % 60).toString().padStart(2, '0')
}
