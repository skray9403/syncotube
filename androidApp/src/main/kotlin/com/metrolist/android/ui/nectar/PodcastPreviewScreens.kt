@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.android.ui.nectar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.preview_add_favorite
import com.metrolist.shared.generated.resources.preview_back
import com.metrolist.shared.generated.resources.preview_back_to_show
import com.metrolist.shared.generated.resources.preview_cast
import com.metrolist.shared.generated.resources.preview_completed
import com.metrolist.shared.generated.resources.preview_description
import com.metrolist.shared.generated.resources.preview_done
import com.metrolist.shared.generated.resources.preview_episode_count
import com.metrolist.shared.generated.resources.preview_episode_details_1
import com.metrolist.shared.generated.resources.preview_episode_details_2
import com.metrolist.shared.generated.resources.preview_episode_details_3
import com.metrolist.shared.generated.resources.preview_episode_details_4
import com.metrolist.shared.generated.resources.preview_episode_details_5
import com.metrolist.shared.generated.resources.preview_episode_title_1
import com.metrolist.shared.generated.resources.preview_episode_title_2
import com.metrolist.shared.generated.resources.preview_episode_title_3
import com.metrolist.shared.generated.resources.preview_episode_title_4
import com.metrolist.shared.generated.resources.preview_episode_title_5
import com.metrolist.shared.generated.resources.preview_episodes
import com.metrolist.shared.generated.resources.preview_in_progress
import com.metrolist.shared.generated.resources.preview_like
import com.metrolist.shared.generated.resources.preview_liked
import com.metrolist.shared.generated.resources.preview_more_options
import com.metrolist.shared.generated.resources.preview_notifications_on
import com.metrolist.shared.generated.resources.preview_notify_me
import com.metrolist.shared.generated.resources.preview_original_series
import com.metrolist.shared.generated.resources.preview_pause
import com.metrolist.shared.generated.resources.preview_play
import com.metrolist.shared.generated.resources.preview_play_episode
import com.metrolist.shared.generated.resources.preview_play_latest
import com.metrolist.shared.generated.resources.preview_player
import com.metrolist.shared.generated.resources.preview_player_options
import com.metrolist.shared.generated.resources.preview_podcast_categories
import com.metrolist.shared.generated.resources.preview_podcast_tagline
import com.metrolist.shared.generated.resources.preview_podcast_title
import com.metrolist.shared.generated.resources.preview_poster_title
import com.metrolist.shared.generated.resources.preview_see_all
import com.metrolist.shared.generated.resources.preview_select
import com.metrolist.shared.generated.resources.preview_show
import com.metrolist.shared.generated.resources.preview_up_next
import org.jetbrains.compose.resources.stringResource

private enum class PodcastPreviewPage { Show, Player, Episodes }

private data class PreviewEpisode(
    val title: String,
    val details: String,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PodcastPreviewRoot() {
    var page by rememberSaveable { mutableStateOf(PodcastPreviewPage.Show) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var liked by rememberSaveable { mutableStateOf(false) }
    var subscribed by rememberSaveable { mutableStateOf(false) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var progress by rememberSaveable { mutableFloatStateOf(0.38f) }
    val episodes = listOf(
        PreviewEpisode(stringResource(Res.string.preview_episode_title_1), stringResource(Res.string.preview_episode_details_1)),
        PreviewEpisode(stringResource(Res.string.preview_episode_title_2), stringResource(Res.string.preview_episode_details_2)),
        PreviewEpisode(stringResource(Res.string.preview_episode_title_3), stringResource(Res.string.preview_episode_details_3)),
        PreviewEpisode(stringResource(Res.string.preview_episode_title_4), stringResource(Res.string.preview_episode_details_4)),
        PreviewEpisode(stringResource(Res.string.preview_episode_title_5), stringResource(Res.string.preview_episode_details_5)),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val destinations = listOf(
                    Triple(stringResource(Res.string.preview_show), MetrolistIcons.Outlined.Podcasts, PodcastPreviewPage.Show),
                    Triple(stringResource(Res.string.preview_player), MetrolistIcons.Rounded.PlayArrow, PodcastPreviewPage.Player),
                    Triple(stringResource(Res.string.preview_episodes), MetrolistIcons.Rounded.QueueMusic, PodcastPreviewPage.Episodes),
                )
                destinations.forEach { (label, icon, destination) ->
                    NavigationBarItem(
                        selected = page == destination,
                        onClick = { page = destination },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (page) {
            PodcastPreviewPage.Show -> ShowPreview(
                padding = padding,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                subscribed = subscribed,
                onSubscribe = { subscribed = !subscribed },
                favorite = favorite,
                onFavorite = { favorite = !favorite },
                onPlay = { page = PodcastPreviewPage.Player; playing = true },
                onEpisodes = { page = PodcastPreviewPage.Episodes },
                onEpisode = { page = PodcastPreviewPage.Player; playing = true },
                episodes = episodes,
            )

            PodcastPreviewPage.Player -> PlayerPreview(
                padding = padding,
                playing = playing,
                liked = liked,
                progress = progress,
                onPlayPause = { playing = !playing },
                onLike = { liked = !liked },
                onSeek = { progress = it },
                onUpNext = { page = PodcastPreviewPage.Episodes },
            )

            PodcastPreviewPage.Episodes -> EpisodesPreview(
                padding = padding,
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                onBack = { page = PodcastPreviewPage.Show },
                onEpisode = { page = PodcastPreviewPage.Player; playing = true },
                episodes = episodes,
            )
        }
    }
}

@Composable
private fun ShowPreview(
    padding: PaddingValues,
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    subscribed: Boolean,
    onSubscribe: () -> Unit,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onPlay: () -> Unit,
    onEpisodes: () -> Unit,
    onEpisode: () -> Unit,
    episodes: List<PreviewEpisode>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box {
                PodcastArtwork(
                    title = stringResource(Res.string.preview_poster_title),
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.98f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = onEpisodes) {
                        Icon(MetrolistIcons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.preview_back))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = onPlay) {
                            Icon(MetrolistIcons.Rounded.PlayArrow, contentDescription = stringResource(Res.string.preview_play_latest))
                        }
                        FilledTonalIconButton(onClick = onEpisodes) {
                            Icon(MetrolistIcons.Default.MoreVert, contentDescription = stringResource(Res.string.preview_more_options))
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.preview_podcast_title), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(Res.string.preview_podcast_tagline), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.preview_podcast_categories), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onPlay) {
                    Icon(MetrolistIcons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.preview_play_latest))
                }
                FilledTonalIconToggleButton(checked = subscribed, onCheckedChange = { onSubscribe() }) {
                    Icon(
                        MetrolistIcons.Outlined.NotificationsNone,
                        contentDescription = stringResource(if (subscribed) Res.string.preview_notifications_on else Res.string.preview_notify_me),
                    )
                }
                FilledTonalIconToggleButton(checked = favorite, onCheckedChange = { onFavorite() }) {
                    Icon(MetrolistIcons.Outlined.Star, contentDescription = stringResource(Res.string.preview_add_favorite))
                }
            }
        }
        item {
            Text(
                stringResource(Res.string.preview_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            PreviewFilters(selectedFilter, onFilterSelected)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.preview_episode_count), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                TextButton(onClick = onEpisodes) { Text(stringResource(Res.string.preview_see_all)) }
            }
        }
        itemsIndexed(episodes.take(3)) { _, episode ->
            EpisodePreviewRow(episode, onClick = onEpisode)
        }
    }
}

@Composable
private fun EpisodesPreview(
    padding: PaddingValues,
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onEpisode: () -> Unit,
    episodes: List<PreviewEpisode>,
) {
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(MetrolistIcons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.preview_back_to_show))
            }
            Text(stringResource(Res.string.preview_podcast_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            FilledTonalIconButton(onClick = onEpisode) {
                Icon(MetrolistIcons.Rounded.PlayArrow, contentDescription = stringResource(Res.string.preview_play_latest))
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = onEpisode) {
                Icon(MetrolistIcons.Default.MoreVert, contentDescription = stringResource(Res.string.preview_more_options))
            }
        }
        PreviewFilters(selectedFilter, onFilterSelected, Modifier.padding(horizontal = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.preview_episode_count), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { selecting = !selecting; selectedIds = emptySet() }) {
                Text(stringResource(if (selecting) Res.string.preview_done else Res.string.preview_select))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(episodes) { _, episode ->
                EpisodePreviewRow(
                    episode = episode,
                    selecting = selecting,
                    selected = episode.title in selectedIds,
                    onClick = {
                        if (selecting) {
                            selectedIds = if (episode.title in selectedIds) selectedIds - episode.title else selectedIds + episode.title
                        } else {
                            onEpisode()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewFilters(
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            stringResource(Res.string.preview_episodes),
            stringResource(Res.string.preview_in_progress),
            stringResource(Res.string.preview_liked),
            stringResource(Res.string.preview_completed),
        ).forEachIndexed { index, label ->
            FilterChip(
                selected = selectedFilter == index,
                onClick = { onFilterSelected(index) },
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun EpisodePreviewRow(
    episode: PreviewEpisode,
    selecting: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PodcastArtwork(
                title = stringResource(Res.string.preview_podcast_title),
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)),
                compact = true,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(episode.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(episode.details, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selecting) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            } else {
                FilledTonalIconButton(onClick = onClick) {
                    Icon(MetrolistIcons.Rounded.PlayArrow, contentDescription = stringResource(Res.string.preview_play_episode))
                }
            }
        }
    }
}

@Composable
private fun PlayerPreview(
    padding: PaddingValues,
    playing: Boolean,
    liked: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onLike: () -> Unit,
    onSeek: (Float) -> Unit,
    onUpNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = onUpNext) { Text(stringResource(Res.string.preview_up_next)) }
            FilledTonalButton(onClick = onUpNext) { Text(stringResource(Res.string.preview_cast)) }
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(onClick = onUpNext) {
                Icon(MetrolistIcons.Default.MoreVert, contentDescription = stringResource(Res.string.preview_player_options))
            }
        }
        PodcastArtwork(
            title = stringResource(Res.string.preview_poster_title),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 4.dp),
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.preview_episode_title_1), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(Res.string.preview_podcast_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (playing) LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Slider(value = progress, onValueChange = onSeek, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("11:24", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("−19:44", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FilledTonalIconToggleButton(checked = liked, onCheckedChange = { onLike() }) {
                Icon(if (liked) MetrolistIcons.Filled.Favorite else MetrolistIcons.Outlined.FavoriteBorder, contentDescription = stringResource(Res.string.preview_like))
            }
            IconButton(onClick = onUpNext) { Text("−30", style = MaterialTheme.typography.titleMedium) }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(76.dp), shapes = IconButtonDefaults.shapes()) {
                Icon(
                    if (playing) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (playing) Res.string.preview_pause else Res.string.preview_play),
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(onClick = onUpNext) { Text("+30", style = MaterialTheme.typography.titleMedium) }
            FilledTonalIconButton(onClick = onUpNext) {
                Icon(MetrolistIcons.Rounded.QueueMusic, contentDescription = stringResource(Res.string.preview_episodes))
            }
        }
    }
}

@Composable
private fun PodcastArtwork(
    title: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 18.dp else 32.dp))
            .background(colors.primaryContainer),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush = Brush.linearGradient(listOf(colors.tertiaryContainer, colors.primaryContainer)))
            drawCircle(
                color = colors.secondaryContainer,
                radius = size.minDimension * 0.37f,
                center = Offset(size.width * 0.78f, size.height * 0.32f),
            )
            drawCircle(
                color = colors.tertiary,
                radius = size.minDimension * 0.19f,
                center = Offset(size.width * 0.19f, size.height * 0.63f),
            )
            val city = Path().apply {
                moveTo(0f, size.height * 0.74f)
                lineTo(size.width * 0.15f, size.height * 0.66f)
                lineTo(size.width * 0.24f, size.height * 0.73f)
                lineTo(size.width * 0.34f, size.height * 0.57f)
                lineTo(size.width * 0.46f, size.height * 0.72f)
                lineTo(size.width * 0.59f, size.height * 0.61f)
                lineTo(size.width * 0.72f, size.height * 0.74f)
                lineTo(size.width * 0.84f, size.height * 0.65f)
                lineTo(size.width, size.height * 0.72f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(city, color = colors.primary)
            drawRect(
                color = colors.tertiaryContainer,
                topLeft = Offset(0f, size.height * 0.84f),
                size = Size(size.width, size.height * 0.16f),
            )
            for (index in 0..4) {
                val x = size.width * (index / 4f)
                drawLine(
                    color = colors.onTertiaryContainer,
                    start = Offset(x, size.height * 0.73f),
                    end = Offset(x, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        if (!compact) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp, start = 12.dp, end = 12.dp),
                color = colors.onTertiaryContainer,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 36.sp,
                lineHeight = 38.sp,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                color = colors.tertiaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    stringResource(Res.string.preview_original_series),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = colors.onTertiaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Text("S", modifier = Modifier.align(Alignment.Center), color = colors.onPrimaryContainer, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
