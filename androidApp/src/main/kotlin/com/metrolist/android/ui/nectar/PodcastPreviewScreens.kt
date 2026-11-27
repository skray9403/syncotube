@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.android.ui.nectar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.preview_add_favorite
import com.metrolist.shared.generated.resources.preview_back
import com.metrolist.shared.generated.resources.preview_bookmark
import com.metrolist.shared.generated.resources.preview_cast
import com.metrolist.shared.generated.resources.preview_completed
import com.metrolist.shared.generated.resources.preview_done
import com.metrolist.shared.generated.resources.preview_download
import com.metrolist.shared.generated.resources.preview_episode_count
import com.metrolist.shared.generated.resources.preview_episode_detail_body
import com.metrolist.shared.generated.resources.preview_episode_detail_meta
import com.metrolist.shared.generated.resources.preview_episode_detail_title
import com.metrolist.shared.generated.resources.preview_episode_details_1
import com.metrolist.shared.generated.resources.preview_episode_details_2
import com.metrolist.shared.generated.resources.preview_episode_details_3
import com.metrolist.shared.generated.resources.preview_episode_details_4
import com.metrolist.shared.generated.resources.preview_episode_details_5
import com.metrolist.shared.generated.resources.preview_episode_poster_badge
import com.metrolist.shared.generated.resources.preview_episode_poster_title
import com.metrolist.shared.generated.resources.preview_episode_show
import com.metrolist.shared.generated.resources.preview_episode_title_1
import com.metrolist.shared.generated.resources.preview_episode_title_2
import com.metrolist.shared.generated.resources.preview_episode_title_3
import com.metrolist.shared.generated.resources.preview_episode_title_4
import com.metrolist.shared.generated.resources.preview_episode_title_5
import com.metrolist.shared.generated.resources.preview_episodes
import com.metrolist.shared.generated.resources.preview_forward_30
import com.metrolist.shared.generated.resources.preview_in_progress
import com.metrolist.shared.generated.resources.preview_like
import com.metrolist.shared.generated.resources.preview_liked
import com.metrolist.shared.generated.resources.preview_more_options
import com.metrolist.shared.generated.resources.preview_next_chapter
import com.metrolist.shared.generated.resources.preview_notifications_on
import com.metrolist.shared.generated.resources.preview_notify_me
import com.metrolist.shared.generated.resources.preview_pause
import com.metrolist.shared.generated.resources.preview_play
import com.metrolist.shared.generated.resources.preview_play_episode
import com.metrolist.shared.generated.resources.preview_play_latest
import com.metrolist.shared.generated.resources.preview_player_chapter_subtitle
import com.metrolist.shared.generated.resources.preview_player_chapter_title
import com.metrolist.shared.generated.resources.preview_player_info
import com.metrolist.shared.generated.resources.preview_player_options
import com.metrolist.shared.generated.resources.preview_player_poster
import com.metrolist.shared.generated.resources.preview_podcast_categories
import com.metrolist.shared.generated.resources.preview_podcast_title
import com.metrolist.shared.generated.resources.preview_poster_title
import com.metrolist.shared.generated.resources.preview_previous_chapter
import com.metrolist.shared.generated.resources.preview_replay_30
import com.metrolist.shared.generated.resources.preview_select
import com.metrolist.shared.generated.resources.preview_show_description
import com.metrolist.shared.generated.resources.preview_show_settings
import com.metrolist.shared.generated.resources.preview_show_subtitle
import com.metrolist.shared.generated.resources.preview_sleep_timer
import com.metrolist.shared.generated.resources.preview_subscribed
import com.metrolist.shared.generated.resources.preview_up_next_label
import com.metrolist.shared.ui.icons.MetrolistIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private data class PreviewEpisode(
    val id: String,
    val title: String,
    val details: String,
)

private val PlaybackSpeeds = listOf(1.0f, 1.2f, 1.5f, 2.0f)

private val BookmarkOutlineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BookmarkOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.Black),
        ) {
            moveTo(17f, 3f)
            horizontalLineTo(7f)
            curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f)
            verticalLineTo(21f)
            lineTo(12f, 18f)
            lineTo(19f, 21f)
            verticalLineTo(5f)
            curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f)
            close()
            moveTo(17f, 18f)
            lineTo(12f, 15.82f)
            lineTo(7f, 18f)
            verticalLineTo(5f)
            horizontalLineTo(17f)
            verticalLineTo(18f)
            close()
        }
    }.build()
}

private val BookmarkFilledIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BookmarkFilled",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.Black),
        ) {
            moveTo(17f, 3f)
            horizontalLineTo(7f)
            curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f)
            verticalLineTo(21f)
            lineTo(12f, 18f)
            lineTo(19f, 21f)
            verticalLineTo(5f)
            curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f)
            close()
        }
    }.build()
}

@Composable
fun PodcastPreviewRoot() {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    var playing by rememberSaveable { mutableStateOf(true) }
    var liked by rememberSaveable { mutableStateOf(false) }
    var bookmarked by rememberSaveable { mutableStateOf(false) }
    var sleepTimerActive by rememberSaveable { mutableStateOf(false) }
    var downloaded by rememberSaveable { mutableStateOf(false) }
    var subscribed by rememberSaveable { mutableStateOf(true) }
    var notifyEnabled by rememberSaveable { mutableStateOf(false) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
    var speedIndex by rememberSaveable { mutableIntStateOf(0) }
    var chapterIndex by rememberSaveable { mutableIntStateOf(11) }
    var progress by rememberSaveable { mutableFloatStateOf(0.50f) }

    val episodes = listOf(
        PreviewEpisode("ep1", stringResource(Res.string.preview_episode_title_1), stringResource(Res.string.preview_episode_details_1)),
        PreviewEpisode("ep2", stringResource(Res.string.preview_episode_title_2), stringResource(Res.string.preview_episode_details_2)),
        PreviewEpisode("ep3", stringResource(Res.string.preview_episode_title_3), stringResource(Res.string.preview_episode_details_3)),
        PreviewEpisode("ep4", stringResource(Res.string.preview_episode_title_4), stringResource(Res.string.preview_episode_details_4)),
        PreviewEpisode("ep5", stringResource(Res.string.preview_episode_title_5), stringResource(Res.string.preview_episode_details_5)),
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
        val scale = 1f - (abs(pageOffset) * 0.04f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            when (page) {
                0 -> BrinkScreenTheme(BrinkEpisodeColorScheme) {
                    EpisodeDetailScreen(
                        downloaded = downloaded,
                        onBack = { scope.launch { pagerState.animateScrollToPage(2) } },
                        onPlay = {
                            playing = true
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onDownload = { downloaded = !downloaded },
                        onOpenShow = { scope.launch { pagerState.animateScrollToPage(2) } },
                    )
                }

                1 -> BrinkScreenTheme(BrinkPlayerColorScheme) {
                    BrinkPlayerScreen(
                        playing = playing,
                        liked = liked,
                        bookmarked = bookmarked,
                        sleepTimerActive = sleepTimerActive,
                        speed = PlaybackSpeeds[speedIndex],
                        chapter = chapterIndex,
                        totalChapters = 17,
                        progress = progress,
                        onDismiss = { scope.launch { pagerState.animateScrollToPage(0) } },
                        onUpNext = { scope.launch { pagerState.animateScrollToPage(2) } },
                        onPreviousChapter = { if (chapterIndex > 1) chapterIndex -= 1 },
                        onNextChapter = { if (chapterIndex < 17) chapterIndex += 1 },
                        onToggleLike = { liked = !liked },
                        onCycleSpeed = { speedIndex = (speedIndex + 1) % PlaybackSpeeds.size },
                        onToggleBookmark = { bookmarked = !bookmarked },
                        onToggleSleepTimer = { sleepTimerActive = !sleepTimerActive },
                        onSeek = { progress = it.coerceIn(0f, 1f) },
                        onReplay30 = { progress = (progress - 0.03f).coerceAtLeast(0f) },
                        onForward30 = { progress = (progress + 0.03f).coerceAtMost(1f) },
                        onPlayPause = { playing = !playing },
                    )
                }

                2 -> BrinkScreenTheme(BrinkShowColorScheme) {
                    ShowDetailScreen(
                        selectedFilter = selectedFilter,
                        subscribed = subscribed,
                        notifyEnabled = notifyEnabled,
                        favorite = favorite,
                        episodes = episodes,
                        onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                        onPlayLatest = {
                            playing = true
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onToggleSubscribed = { subscribed = !subscribed },
                        onToggleNotify = { notifyEnabled = !notifyEnabled },
                        onToggleFavorite = { favorite = !favorite },
                        onFilterSelected = { selectedFilter = it },
                        onEpisodeClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        onPlayEpisode = {
                            playing = true
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeDetailScreen(
    downloaded: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onOpenShow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to colors.tertiary,
                        0.40f to colors.tertiary.copy(alpha = 0.85f),
                        0.50f to colors.surface,
                        1.0f to colors.surface,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = MetrolistIcons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.preview_back),
                    )
                }

                ConnectedIconActionGroup(
                    actions = listOf(
                        ConnectedActionItem(
                            icon = MetrolistIcons.Rounded.PlayArrow,
                            contentDescription = stringResource(Res.string.preview_play),
                            checked = false,
                            onClick = onPlay,
                        ),
                        ConnectedActionItem(
                            icon = if (downloaded) MetrolistIcons.Default.Check else MetrolistIcons.Default.Download,
                            contentDescription = stringResource(Res.string.preview_download),
                            checked = downloaded,
                            onClick = onDownload,
                        ),
                        ConnectedActionItem(
                            icon = MetrolistIcons.Default.MoreHoriz,
                            contentDescription = stringResource(Res.string.preview_more_options),
                            checked = false,
                            onClick = onOpenShow,
                        ),
                    ),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    checkedContainerColor = colors.primary,
                    checkedContentColor = colors.onPrimary,
                )
            }

            Spacer(Modifier.height(14.dp))

            EpisodeHeroArtwork(
                title = stringResource(Res.string.preview_episode_poster_title),
                badge = stringResource(Res.string.preview_episode_poster_badge),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.95f),
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenShow)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.preview_episode_show),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = MetrolistIcons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = stringResource(Res.string.preview_episode_detail_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 29.sp,
                ),
                color = colors.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.preview_episode_detail_meta),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(Res.string.preview_episode_detail_body),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                ),
                color = colors.onSurface,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BrinkPlayerScreen(
    playing: Boolean,
    liked: Boolean,
    bookmarked: Boolean,
    sleepTimerActive: Boolean,
    speed: Float,
    chapter: Int,
    totalChapters: Int,
    progress: Float,
    onDismiss: () -> Unit,
    onUpNext: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onToggleLike: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleSleepTimer: () -> Unit,
    onSeek: (Float) -> Unit,
    onReplay30: () -> Unit,
    onForward30: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val totalSeconds = 239 * 60 + 5
    val elapsedSeconds = (totalSeconds * progress).roundToInt()
    val remainingSeconds = (totalSeconds - elapsedSeconds).coerceAtLeast(0)

    val artworkScale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "artworkScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to colors.surfaceContainerLowest,
                        0.44f to colors.background,
                        0.62f to colors.surface,
                        1.0f to colors.surface,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp),
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = MetrolistIcons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(Res.string.preview_back),
                    )
                }

                ButtonGroup(
                    overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    customItem(
                        buttonGroupContent = {
                            val interactionSource = remember { MutableInteractionSource() }
                            ToggleButton(
                                checked = false,
                                onCheckedChange = { onDismiss() },
                                interactionSource = interactionSource,
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(42.dp),
                            ) {
                                Icon(
                                    imageVector = MetrolistIcons.Default.Info,
                                    contentDescription = stringResource(Res.string.preview_player_info),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.preview_player_info)) },
                                onClick = onDismiss,
                            )
                        },
                    )
                    customItem(
                        buttonGroupContent = {
                            val interactionSource = remember { MutableInteractionSource() }
                            ToggleButton(
                                checked = false,
                                onCheckedChange = { onUpNext() },
                                interactionSource = interactionSource,
                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(42.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.preview_up_next_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.preview_up_next_label)) },
                                onClick = onUpNext,
                            )
                        },
                    )
                    customItem(
                        buttonGroupContent = {
                            val interactionSource = remember { MutableInteractionSource() }
                            ToggleButton(
                                checked = false,
                                onCheckedChange = { onUpNext() },
                                interactionSource = interactionSource,
                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(42.dp),
                            ) {
                                Icon(
                                    imageVector = MetrolistIcons.Rounded.Cast,
                                    contentDescription = stringResource(Res.string.preview_cast),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.preview_cast)) },
                                onClick = onUpNext,
                            )
                        },
                    )
                    customItem(
                        buttonGroupContent = {
                            val interactionSource = remember { MutableInteractionSource() }
                            ToggleButton(
                                checked = false,
                                onCheckedChange = { onUpNext() },
                                interactionSource = interactionSource,
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = colors.primaryContainer,
                                    contentColor = colors.onPrimaryContainer,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(42.dp),
                            ) {
                                Icon(
                                    imageVector = MetrolistIcons.Default.MoreHoriz,
                                    contentDescription = stringResource(Res.string.preview_player_options),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.preview_player_options)) },
                                onClick = onUpNext,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.weight(0.55f))

            PlayerPosterArtwork(
                title = stringResource(Res.string.preview_player_poster),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.10f)
                    .graphicsLayer {
                        scaleX = artworkScale
                        scaleY = artworkScale
                    },
            )

            Spacer(Modifier.weight(0.45f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(
                    onClick = onPreviousChapter,
                    modifier = Modifier.size(44.dp),
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = MetrolistIcons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.preview_previous_chapter),
                        modifier = Modifier.size(18.dp),
                    )
                }

                AnimatedContent(
                    targetState = chapter,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { it / 3 }) togetherWith
                            (fadeOut(tween(180)) + slideOutVertically { -it / 3 })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    label = "chapterInfo",
                ) { currentChapter ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.preview_player_chapter_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.preview_player_chapter_subtitle, currentChapter, totalChapters),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val chapterProgress by animateFloatAsState(
                        targetValue = chapter.toFloat() / totalChapters.toFloat(),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "chapterProgress",
                    )
                    CircularProgressIndicator(
                        progress = { chapterProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = colors.primary,
                        trackColor = colors.surfaceVariant.copy(alpha = 0.40f),
                        strokeWidth = 3.5.dp,
                    )
                    FilledTonalIconButton(
                        onClick = onNextChapter,
                        modifier = Modifier.size(44.dp),
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(Res.string.preview_next_chapter),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlayerActionPill(
                    selected = liked,
                    onClick = onToggleLike,
                    icon = if (liked) MetrolistIcons.Rounded.Favorite else MetrolistIcons.Outlined.FavoriteBorder,
                    label = stringResource(Res.string.preview_like),
                )
                PlayerActionPill(
                    selected = speed != 1.0f,
                    onClick = onCycleSpeed,
                    icon = MetrolistIcons.Default.Speed,
                    label = "${speed}×",
                )
                PlayerActionPill(
                    selected = bookmarked,
                    onClick = onToggleBookmark,
                    icon = if (bookmarked) BookmarkFilledIcon else BookmarkOutlineIcon,
                    label = stringResource(Res.string.preview_bookmark),
                )
                PlayerActionPill(
                    selected = sleepTimerActive,
                    onClick = onToggleSleepTimer,
                    icon = MetrolistIcons.Outlined.Bedtime,
                    label = stringResource(Res.string.preview_sleep_timer),
                )
            }

            Spacer(Modifier.height(18.dp))

            ExpressiveWavyScrubber(
                progress = progress,
                playing = playing,
                activeColor = colors.primary,
                inactiveColor = colors.onSurfaceVariant.copy(alpha = 0.35f),
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatHms(elapsedSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = "-${formatHms(remainingSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(18.dp))

            val replayInteraction = remember { MutableInteractionSource() }
            val replayPressed by replayInteraction.collectIsPressedAsState()
            val replayScale by animateFloatAsState(
                targetValue = if (replayPressed) 0.93f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "replayScale",
            )

            val playInteraction = remember { MutableInteractionSource() }
            val playPressed by playInteraction.collectIsPressedAsState()
            val playScale by animateFloatAsState(
                targetValue = if (playPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "playScale",
            )
            val playCorner by animateDpAsState(
                targetValue = if (playing) 20.dp else 38.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "playCorner",
            )

            val forwardInteraction = remember { MutableInteractionSource() }
            val forwardPressed by forwardInteraction.collectIsPressedAsState()
            val forwardScale by animateFloatAsState(
                targetValue = if (forwardPressed) 0.93f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "forwardScale",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onReplay30,
                    interactionSource = replayInteraction,
                    shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 14.dp, bottomEnd = 14.dp),
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = replayScale
                            scaleY = replayScale
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SkipSecondsBadge(
                            seconds = 30,
                            forward = false,
                            tint = colors.onSecondaryContainer,
                            contentDescription = stringResource(Res.string.preview_replay_30),
                        )
                    }
                }

                Surface(
                    onClick = onPlayPause,
                    interactionSource = playInteraction,
                    shape = RoundedCornerShape(playCorner),
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                    modifier = Modifier
                        .weight(1.18f)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = playing,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.72f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                                    (scaleOut(targetScale = 0.72f) + fadeOut())
                            },
                            label = "playPauseIcon",
                        ) { isPlaying ->
                            Icon(
                                imageVector = if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) Res.string.preview_pause else Res.string.preview_play),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                Surface(
                    onClick = onForward30,
                    interactionSource = forwardInteraction,
                    shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 28.dp, bottomEnd = 28.dp),
                    color = colors.secondaryContainer,
                    contentColor = colors.onSecondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = forwardScale
                            scaleY = forwardScale
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        SkipSecondsBadge(
                            seconds = 30,
                            forward = true,
                            tint = colors.onSecondaryContainer,
                            contentDescription = stringResource(Res.string.preview_forward_30),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowDetailScreen(
    selectedFilter: Int,
    subscribed: Boolean,
    notifyEnabled: Boolean,
    favorite: Boolean,
    episodes: List<PreviewEpisode>,
    onBack: () -> Unit,
    onPlayLatest: () -> Unit,
    onToggleSubscribed: () -> Unit,
    onToggleNotify: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFilterSelected: (Int) -> Unit,
    onEpisodeClick: (PreviewEpisode) -> Unit,
    onPlayEpisode: (PreviewEpisode) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    val heroScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 400f
        }
    }

    val filterLabels = listOf(
        stringResource(Res.string.preview_episodes),
        stringResource(Res.string.preview_in_progress),
        stringResource(Res.string.preview_liked),
        stringResource(Res.string.preview_completed),
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item(key = "show_hero", contentType = "show_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(365.dp),
            ) {
                ShowHeroArtwork(
                    title = stringResource(Res.string.preview_poster_title),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = heroScrollOffset * 0.28f
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.surface),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp),
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.preview_back),
                        )
                    }

                    ConnectedIconActionGroup(
                        actions = listOf(
                            ConnectedActionItem(
                                icon = MetrolistIcons.Rounded.PlayArrow,
                                contentDescription = stringResource(Res.string.preview_play_latest),
                                checked = false,
                                onClick = onPlayLatest,
                            ),
                            ConnectedActionItem(
                                icon = MetrolistIcons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.preview_more_options),
                                checked = false,
                                onClick = { selecting = !selecting },
                            ),
                        ),
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        checkedContainerColor = colors.primary,
                        checkedContentColor = colors.onPrimary,
                    )
                }

                Text(
                    text = stringResource(Res.string.preview_show_subtitle),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                )
            }
        }

        item(key = "show_actions", contentType = "show_actions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ConnectedIconActionGroup(
                    actions = listOf(
                        ConnectedActionItem(
                            icon = MetrolistIcons.Default.Check,
                            contentDescription = stringResource(Res.string.preview_subscribed),
                            checked = subscribed,
                            onClick = onToggleSubscribed,
                        ),
                        ConnectedActionItem(
                            icon = MetrolistIcons.Outlined.NotificationsNone,
                            contentDescription = stringResource(if (notifyEnabled) Res.string.preview_notifications_on else Res.string.preview_notify_me),
                            checked = notifyEnabled,
                            onClick = onToggleNotify,
                        ),
                        ConnectedActionItem(
                            icon = MetrolistIcons.Default.Settings,
                            contentDescription = stringResource(Res.string.preview_show_settings),
                            checked = false,
                            onClick = { selecting = !selecting },
                        ),
                        ConnectedActionItem(
                            icon = if (favorite) MetrolistIcons.Default.Star else MetrolistIcons.Outlined.Star,
                            contentDescription = stringResource(Res.string.preview_add_favorite),
                            checked = favorite,
                            onClick = onToggleFavorite,
                        ),
                    ),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    checkedContainerColor = colors.primaryContainer,
                    checkedContentColor = colors.onPrimaryContainer,
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(Res.string.preview_show_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.preview_podcast_categories),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        item(key = "show_filters", contentType = "show_filters") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filterLabels.forEachIndexed { index, label ->
                    val selected = selectedFilter == index
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) colors.primary else colors.primaryContainer,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "filterPill",
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (selected) colors.onPrimary else colors.onPrimaryContainer,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "filterText",
                    )
                    val cornerRadius by animateDpAsState(
                        targetValue = if (selected) 20.dp else 14.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "filterCorner",
                    )
                    Surface(
                        onClick = { onFilterSelected(index) },
                        shape = RoundedCornerShape(cornerRadius),
                        color = pillColor,
                        contentColor = labelColor,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }

        item(key = "show_list_header", contentType = "show_list_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.preview_episode_count),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(if (selecting) Res.string.preview_done else Res.string.preview_select),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selecting = !selecting
                            if (!selecting) selectedIds = emptySet()
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }

        items(
            items = episodes,
            key = { it.id },
            contentType = { "show_episode_row" },
        ) { episode ->
            val checked = episode.id in selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selecting) {
                            selectedIds = if (checked) selectedIds - episode.id else selectedIds + episode.id
                        } else {
                            onEpisodeClick(episode)
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShowEpisodeThumbnail(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.preview_podcast_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = episode.details,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }

                AnimatedVisibility(
                    visible = selecting,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            selectedIds = if (checked) selectedIds - episode.id else selectedIds + episode.id
                        },
                    )
                }
                AnimatedVisibility(
                    visible = !selecting,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                ) {
                    ConnectedIconActionGroup(
                        actions = listOf(
                            ConnectedActionItem(
                                icon = MetrolistIcons.Rounded.PlayArrow,
                                contentDescription = stringResource(Res.string.preview_play_episode),
                                checked = false,
                                onClick = { onPlayEpisode(episode) },
                            ),
                            ConnectedActionItem(
                                icon = MetrolistIcons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.preview_more_options),
                                checked = false,
                                onClick = { onEpisodeClick(episode) },
                            ),
                        ),
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        checkedContainerColor = colors.primary,
                        checkedContentColor = colors.onPrimary,
                    )
                }
            }
        }
    }
}

private data class ConnectedActionItem(
    val icon: ImageVector,
    val contentDescription: String,
    val checked: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun ConnectedIconActionGroup(
    actions: List<ConnectedActionItem>,
    containerColor: Color,
    contentColor: Color,
    checkedContainerColor: Color,
    checkedContentColor: Color,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier,
    ) {
        actions.forEachIndexed { index, action ->
            customItem(
                buttonGroupContent = {
                    val shapes = when {
                        actions.size == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    FilledTonalToggleButton(
                        checked = action.checked,
                        onCheckedChange = { action.onClick() },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                            checkedContainerColor = checkedContainerColor,
                            checkedContentColor = checkedContentColor,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(42.dp),
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.contentDescription,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                menuContent = {
                    DropdownMenuItem(
                        leadingIcon = { Icon(action.icon, contentDescription = null) },
                        text = { Text(action.contentDescription) },
                        onClick = action.onClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun PlayerActionPill(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillScale",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.secondaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillFg",
    )
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ExpressiveWavyScrubber(
    progress: Float,
    playing: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (playing) 1f else 0.12f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "waveAmp",
    )

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            },
    ) {
        val centerY = size.height / 2f
        val playheadX = size.width * clamped
        val baseAmplitude = size.height * 0.27f * amplitudeMultiplier
        val wavePeriodPx = 28.dp.toPx()
        val strokePx = 2.6.dp.toPx()

        if (playheadX > 4.dp.toPx()) {
            val wavePath = Path()
            val stepPx = 2.5f
            var x = 0f
            wavePath.moveTo(0f, centerY)
            while (x <= playheadX) {
                val t = x / size.width.coerceAtLeast(1f)
                val envelope = 0.65f + 0.35f * sin(t * 7.5f + phase * 0.5f)
                val angle = (x / wavePeriodPx) * (2f * PI.toFloat()) - phase
                val y = centerY + sin(angle) * baseAmplitude * envelope
                wavePath.lineTo(x, y)
                x += stepPx
            }
            drawPath(
                path = wavePath,
                color = activeColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        val segmentWidth = 22.dp.toPx()
        val gapWidth = 6.dp.toPx()
        var segStart = playheadX + 6.dp.toPx()
        while (segStart < size.width) {
            val segEnd = (segStart + segmentWidth).coerceAtMost(size.width)
            drawLine(
                color = inactiveColor,
                start = Offset(segStart, centerY),
                end = Offset(segEnd, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            segStart += segmentWidth + gapWidth
        }

        drawRoundRect(
            color = activeColor,
            topLeft = Offset(playheadX - 2.dp.toPx(), 2.dp.toPx()),
            size = Size(4.dp.toPx(), size.height - 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
    }
}

@Composable
private fun SkipSecondsBadge(
    seconds: Int,
    forward: Boolean,
    tint: Color,
    contentDescription: String,
) {
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) MetrolistIcons.Outlined.FastForward else MetrolistIcons.Outlined.Replay,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = seconds.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = tint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun EpisodeHeroArtwork(
    title: String,
    badge: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF47BBE2)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF39B5E6),
                        0.55f to Color(0xFFA7DFDF),
                        0.72f to Color(0xFFE7CC82),
                        1.0f to Color(0xFFDDC06C),
                    ),
                ),
            )
            // Neighborhood houses silhouettes
            val houseColors = listOf(Color(0xFFB36542), Color(0xFFD2A765), Color(0xFF6B8A9C), Color(0xFFC47A53))
            val widths = listOf(0.22f, 0.24f, 0.25f, 0.29f)
            var curX = 0f
            widths.forEachIndexed { idx, wFrac ->
                val hw = size.width * wFrac
                val hh = size.height * (0.20f + (idx % 2) * 0.05f)
                val topY = size.height * 0.66f - hh
                drawRect(
                    color = houseColors[idx],
                    topLeft = Offset(curX, topY),
                    size = Size(hw - 4.dp.toPx(), hh),
                )
                val roof = Path().apply {
                    moveTo(curX - 4.dp.toPx(), topY)
                    lineTo(curX + hw * 0.5f, topY - size.height * 0.08f)
                    lineTo(curX + hw, topY)
                    close()
                }
                drawPath(roof, color = Color(0xFF51382A))
                curX += hw
            }
            // Sidewalk
            drawRect(
                color = Color(0xFFDFC78B),
                topLeft = Offset(0f, size.height * 0.66f),
                size = Size(size.width, size.height * 0.34f),
            )
            // Cartoon character: head, headphones, red shirt, dark pants
            drawCircle(
                color = Color(0xFFF3C4A4),
                radius = size.minDimension * 0.11f,
                center = Offset(size.width * 0.49f, size.height * 0.46f),
            )
            // Headphones band
            drawArc(
                color = Color(0xFF1E2832),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(size.width * 0.36f, size.height * 0.32f),
                size = Size(size.width * 0.26f, size.height * 0.22f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            // Red shirt torso
            drawRoundRect(
                color = Color(0xFFD62B2B),
                topLeft = Offset(size.width * 0.41f, size.height * 0.56f),
                size = Size(size.width * 0.16f, size.height * 0.16f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            )
            // Dark pants/legs
            drawRoundRect(
                color = Color(0xFF28303D),
                topLeft = Offset(size.width * 0.42f, size.height * 0.70f),
                size = Size(size.width * 0.06f, size.height * 0.15f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0xFF28303D),
                topLeft = Offset(size.width * 0.50f, size.height * 0.70f),
                size = Size(size.width * 0.06f, size.height * 0.14f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }
        // Bold comic logo
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp,
                ),
                color = Color(0xFFF6D638),
                textAlign = TextAlign.Center,
            )
        }
        Surface(
            color = Color(0xFF56A9D4),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PlayerPosterArtwork(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF10212B)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0C1C26),
                        0.60f to Color(0xFF1F3847),
                        0.72f to Color(0xFF7B93A3),
                        1.0f to Color(0xFF8EA4B2),
                    ),
                ),
            )
            // Ground shadow
            drawOval(
                color = Color(0xAA0A161E),
                topLeft = Offset(size.width * 0.14f, size.height * 0.67f),
                size = Size(size.width * 0.72f, size.height * 0.07f),
            )
            // Ferrari F40-inspired silhouette facing left
            val carBody = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.66f)
                lineTo(size.width * 0.14f, size.height * 0.60f)
                lineTo(size.width * 0.34f, size.height * 0.56f)
                lineTo(size.width * 0.45f, size.height * 0.49f)
                lineTo(size.width * 0.63f, size.height * 0.49f)
                lineTo(size.width * 0.76f, size.height * 0.55f)
                lineTo(size.width * 0.80f, size.height * 0.45f)
                lineTo(size.width * 0.86f, size.height * 0.45f)
                lineTo(size.width * 0.87f, size.height * 0.65f)
                close()
            }
            drawPath(carBody, color = Color(0xFFC81E1E))
            // Cabin glass
            val cabin = Path().apply {
                moveTo(size.width * 0.38f, size.height * 0.55f)
                lineTo(size.width * 0.46f, size.height * 0.50f)
                lineTo(size.width * 0.60f, size.height * 0.50f)
                lineTo(size.width * 0.66f, size.height * 0.55f)
                close()
            }
            drawPath(cabin, color = Color(0xFF1A252D))
            // Wheels + silver rims
            val wheelRadius = size.minDimension * 0.068f
            listOf(0.28f, 0.72f).forEach { cx ->
                drawCircle(
                    color = Color(0xFF121619),
                    radius = wheelRadius,
                    center = Offset(size.width * cx, size.height * 0.66f),
                )
                drawCircle(
                    color = Color(0xFFC5CFD5),
                    radius = wheelRadius * 0.58f,
                    center = Offset(size.width * cx, size.height * 0.66f),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                fontSize = 40.sp,
            ),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 34.dp),
        )
    }
}

@Composable
private fun ShowHeroArtwork(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(colors.tertiary),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF28AEE6),
                        0.55f to Color(0xFF4CC4F0),
                        1.0f to colors.surface,
                    ),
                ),
            )
            // Clouds
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = size.minDimension * 0.11f,
                center = Offset(size.width * 0.06f, size.height * 0.25f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = size.minDimension * 0.09f,
                center = Offset(size.width * 0.91f, size.height * 0.22f),
            )
            // Rolling emerald hill
            val hill = Path().apply {
                moveTo(0f, size.height * 0.66f)
                quadraticTo(size.width * 0.45f, size.height * 0.54f, size.width, size.height * 0.68f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hill, color = colors.surface)
            // Billboard grid
            val boardLeft = size.width * 0.16f
            val boardTop = size.height * 0.44f
            val boardWidth = size.width * 0.68f
            val boardHeight = size.height * 0.23f
            drawRect(
                color = Color(0xFF39B2E3),
                topLeft = Offset(boardLeft, boardTop),
                size = Size(boardWidth, boardHeight),
            )
            drawRect(
                color = Color(0xFF0A2416),
                topLeft = Offset(boardLeft, boardTop),
                size = Size(boardWidth, boardHeight),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            for (i in 1..5) {
                val x = boardLeft + boardWidth * (i / 6f)
                drawLine(
                    color = Color(0xFF0A2416).copy(alpha = 0.6f),
                    start = Offset(x, boardTop),
                    end = Offset(x, boardTop + boardHeight),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            for (j in 1..2) {
                val y = boardTop + boardHeight * (j / 3f)
                drawLine(
                    color = Color(0xFF0A2416).copy(alpha = 0.6f),
                    start = Offset(boardLeft, y),
                    end = Offset(boardLeft + boardWidth, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                fontSize = 42.sp,
            ),
            color = Color(0xFFF6D96B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 60.dp, start = 20.dp, end = 20.dp),
        )
    }
}

@Composable
private fun ShowEpisodeThumbnail(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(colors.tertiary),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF34B5E8))
            drawRect(
                color = colors.surface,
                topLeft = Offset(0f, size.height * 0.62f),
                size = Size(size.width, size.height * 0.38f),
            )
            drawCircle(
                color = Color(0xFFF6D96B),
                radius = size.minDimension * 0.24f,
                center = Offset(size.width * 0.5f, size.height * 0.48f),
            )
        }
    }
}

private fun formatHms(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val hours = clamped / 3600
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
