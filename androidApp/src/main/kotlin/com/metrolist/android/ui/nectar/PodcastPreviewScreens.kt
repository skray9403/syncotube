@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.android.ui.nectar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class PreviewEpisode(
    val id: String,
    val title: String,
    val details: String,
)

private val PlaybackSpeeds = listOf(1.0f, 1.2f, 1.5f, 2.0f)

private fun <T> expressiveSpatialSpring() = spring<T>(
    dampingRatio = 0.8f,
    stiffness = 380f,
)

private fun <T> expressiveColorTween() = tween<T>(
    durationMillis = 220,
    easing = FastOutSlowInEasing,
)

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
        val scale = 1f - (abs(pageOffset) * 0.03f)
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
    val scrollState = rememberScrollState()
    val fadeRangePx = with(LocalDensity.current) { 150.dp.toPx() }
    val scrollFade by remember {
        derivedStateOf { (scrollState.value / fadeRangePx).coerceIn(0f, 1f) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val heroHeight = (maxHeight * 0.44f).coerceAtLeast(300.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight),
            ) {
                EpisodeHeroArtwork(
                    title = stringResource(Res.string.preview_episode_poster_title),
                    badge = stringResource(Res.string.preview_episode_poster_badge),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - (scrollFade * 0.35f)
                            translationY = scrollState.value * 0.25f
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight * 0.42f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.55f to colors.surface.copy(alpha = 0.68f),
                                    1.0f to colors.surface,
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
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

                Spacer(Modifier.height(4.dp))

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
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                    color = colors.onSurface,
                )

                Spacer(Modifier.height(28.dp))
            }
        }

        // Floating translucent top controls
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
                    containerColor = colors.primaryContainer.copy(alpha = 0.88f),
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
                containerColor = colors.primaryContainer.copy(alpha = 0.88f),
                contentColor = colors.onPrimaryContainer,
                checkedContainerColor = colors.primary,
                checkedContentColor = colors.onPrimary,
            )
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

    var isScrubbing by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "scrubberProgress",
    )
    val displayedProgress = if (isScrubbing) progress else animatedProgress

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val heroHeight = (maxHeight * 0.45f).coerceAtLeast(290.dp)

        // Full-bleed top artwork extending behind floating top controls, fading smoothly into slate-blue surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        ) {
            PlayerPosterArtwork(
                title = stringResource(Res.string.preview_player_poster),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight * 0.46f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.55f to colors.surfaceVariant.copy(alpha = 0.78f),
                                1.0f to colors.surface,
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Floating translucent top controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp),
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = colors.primaryContainer.copy(alpha = 0.85f),
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
                                    containerColor = colors.primaryContainer.copy(alpha = 0.85f),
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
                                    containerColor = colors.primaryContainer.copy(alpha = 0.85f),
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
                                    containerColor = colors.primaryContainer.copy(alpha = 0.85f),
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
                                    containerColor = colors.primaryContainer.copy(alpha = 0.85f),
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

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                            (fadeIn(tween(150)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically { -it / 4 })
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        label = "chapterInfo",
                    ) { currentChapter ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(Res.string.preview_player_chapter_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.5.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
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
                            animationSpec = expressiveSpatialSpring(),
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

                Spacer(Modifier.height(14.dp))

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

                Spacer(Modifier.height(16.dp))

                ExpressiveWavyScrubber(
                    progress = displayedProgress,
                    playing = playing,
                    activeColor = colors.primary,
                    inactiveColor = colors.onSurfaceVariant.copy(alpha = 0.35f),
                    onScrubStart = { isScrubbing = true },
                    onScrubEnd = { isScrubbing = false },
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

                Spacer(Modifier.height(16.dp))

                val replayInteraction = remember { MutableInteractionSource() }
                val replayPressed by replayInteraction.collectIsPressedAsState()
                val replayScale by animateFloatAsState(
                    targetValue = if (replayPressed) 0.94f else 1f,
                    animationSpec = expressiveSpatialSpring(),
                    label = "replayScale",
                )

                val playInteraction = remember { MutableInteractionSource() }
                val playPressed by playInteraction.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (playPressed) 0.94f else 1f,
                    animationSpec = expressiveSpatialSpring(),
                    label = "playScale",
                )
                val playCorner by animateDpAsState(
                    targetValue = if (playing) 18.dp else 36.dp,
                    animationSpec = expressiveSpatialSpring(),
                    label = "playCorner",
                )

                val forwardInteraction = remember { MutableInteractionSource() }
                val forwardPressed by forwardInteraction.collectIsPressedAsState()
                val forwardScale by animateFloatAsState(
                    targetValue = if (forwardPressed) 0.94f else 1f,
                    animationSpec = expressiveSpatialSpring(),
                    label = "forwardScale",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onReplay30,
                        interactionSource = replayInteraction,
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 12.dp, bottomEnd = 12.dp),
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
                                    (scaleIn(initialScale = 0.78f, animationSpec = expressiveSpatialSpring()) + fadeIn(tween(150))) togetherWith
                                        (scaleOut(targetScale = 0.78f) + fadeOut(tween(150)))
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
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 20.dp, bottomEnd = 20.dp),
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

    val fadeRangePx = with(LocalDensity.current) { 150.dp.toPx() }
    val heroScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else fadeRangePx
        }
    }
    val heroFade = (heroScrollOffset / fadeRangePx).coerceIn(0f, 1f)

    val filterLabels = listOf(
        stringResource(Res.string.preview_episodes),
        stringResource(Res.string.preview_in_progress),
        stringResource(Res.string.preview_liked),
        stringResource(Res.string.preview_completed),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        val heroHeight = (maxHeight * 0.36f).coerceAtLeast(270.dp)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item(key = "show_hero", contentType = "show_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight),
                ) {
                    ShowHeroArtwork(
                        title = stringResource(Res.string.preview_poster_title),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 1f - (heroFade * 0.35f)
                                translationY = heroScrollOffset * 0.28f
                            },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight * 0.42f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.55f to colors.surface.copy(alpha = 0.72f),
                                        1.0f to colors.surface,
                                    ),
                                ),
                            ),
                    )
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
                        color = colors.onSurfaceVariant.copy(alpha = 0.90f),
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
                            animationSpec = expressiveColorTween(),
                            label = "filterPill",
                        )
                        val labelColor by animateColorAsState(
                            targetValue = if (selected) colors.onPrimary else colors.onPrimaryContainer,
                            animationSpec = expressiveColorTween(),
                            label = "filterText",
                        )
                        val cornerRadius by animateDpAsState(
                            targetValue = if (selected) 20.dp else 14.dp,
                            animationSpec = expressiveSpatialSpring(),
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
                        title = stringResource(Res.string.preview_poster_title),
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
                        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = expressiveSpatialSpring()),
                        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.85f),
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
                        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.88f, animationSpec = expressiveSpatialSpring()),
                        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.88f),
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

        // Floating top controls over full-bleed banner
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
                    containerColor = colors.primaryContainer.copy(alpha = 0.90f),
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
                containerColor = colors.primaryContainer.copy(alpha = 0.90f),
                contentColor = colors.onPrimaryContainer,
                checkedContainerColor = colors.primary,
                checkedContentColor = colors.onPrimary,
            )
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
        animationSpec = expressiveSpatialSpring(),
        label = "pillScale",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.secondaryContainer,
        animationSpec = expressiveColorTween(),
        label = "pillBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.onSecondaryContainer,
        animationSpec = expressiveColorTween(),
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
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
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
        targetValue = if (playing) 1f else 0.10f,
        animationSpec = expressiveSpatialSpring(),
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
                detectHorizontalDragGestures(
                    onDragStart = { onScrubStart() },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    },
                )
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
        modifier = Modifier
            .size(34.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.2.dp.toPx()
            val inset = 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // Draw a ~280 deg circular arc leaving a gap at the top-center for the arrowhead
            if (forward) {
                drawArc(
                    color = tint,
                    startAngle = -65f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Clockwise arrowhead at top-right (-65 deg)
                val angleRad = (-65f * PI / 180f).toFloat()
                val r = arcSize.width / 2f
                val tip = Offset(
                    size.width / 2f + r * cos(angleRad),
                    size.height / 2f + r * sin(angleRad),
                )
                val arrow = Path().apply {
                    moveTo(tip.x + 1.dp.toPx(), tip.y + 1.dp.toPx())
                    lineTo(tip.x - 5.dp.toPx(), tip.y - 3.dp.toPx())
                    moveTo(tip.x + 1.dp.toPx(), tip.y + 1.dp.toPx())
                    lineTo(tip.x - 1.dp.toPx(), tip.y - 6.dp.toPx())
                }
                drawPath(arrow, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawArc(
                    color = tint,
                    startAngle = -115f,
                    sweepAngle = -285f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Counter-clockwise arrowhead at top-left (-115 deg)
                val angleRad = (-115f * PI / 180f).toFloat()
                val r = arcSize.width / 2f
                val tip = Offset(
                    size.width / 2f + r * cos(angleRad),
                    size.height / 2f + r * sin(angleRad),
                )
                val arrow = Path().apply {
                    moveTo(tip.x - 1.dp.toPx(), tip.y + 1.dp.toPx())
                    lineTo(tip.x + 5.dp.toPx(), tip.y - 3.dp.toPx())
                    moveTo(tip.x - 1.dp.toPx(), tip.y + 1.dp.toPx())
                    lineTo(tip.x + 1.dp.toPx(), tip.y - 6.dp.toPx())
                }
                drawPath(arrow, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Text(
            text = seconds.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
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
    Box(
        modifier = modifier.background(Color(0xFF47BBE2)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF39B5E6),
                        0.56f to Color(0xFFA7DFDF),
                        0.74f to Color(0xFFE7CC82),
                        1.0f to Color(0xFFE8D57A),
                    ),
                ),
            )
            // Neighborhood houses
            val houseColors = listOf(Color(0xFFB36542), Color(0xFFD2A765), Color(0xFF6B8A9C), Color(0xFFC47A53))
            val widths = listOf(0.22f, 0.24f, 0.25f, 0.29f)
            var curX = 0f
            widths.forEachIndexed { idx, wFrac ->
                val hw = size.width * wFrac
                val hh = size.height * (0.19f + (idx % 2) * 0.05f)
                val topY = size.height * 0.68f - hh
                drawRect(
                    color = houseColors[idx],
                    topLeft = Offset(curX, topY),
                    size = Size(hw - 4.dp.toPx(), hh),
                )
                val roof = Path().apply {
                    moveTo(curX - 4.dp.toPx(), topY)
                    lineTo(curX + hw * 0.5f, topY - size.height * 0.07f)
                    lineTo(curX + hw, topY)
                    close()
                }
                drawPath(roof, color = Color(0xFF51382A))
                curX += hw
            }
            // Sidewalk
            drawRect(
                color = Color(0xFFDFC78B),
                topLeft = Offset(0f, size.height * 0.68f),
                size = Size(size.width, size.height * 0.32f),
            )
            // Character with headphones
            drawCircle(
                color = Color(0xFFF3C4A4),
                radius = size.minDimension * 0.10f,
                center = Offset(size.width * 0.49f, size.height * 0.49f),
            )
            drawArc(
                color = Color(0xFF1E2832),
                startAngle = 190f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(size.width * 0.37f, size.height * 0.36f),
                size = Size(size.width * 0.24f, size.height * 0.20f),
                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawRoundRect(
                color = Color(0xFFD62B2B),
                topLeft = Offset(size.width * 0.42f, size.height * 0.58f),
                size = Size(size.width * 0.14f, size.height * 0.15f),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0xFF28303D),
                topLeft = Offset(size.width * 0.43f, size.height * 0.71f),
                size = Size(size.width * 0.055f, size.height * 0.14f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0xFF28303D),
                topLeft = Offset(size.width * 0.50f, size.height * 0.71f),
                size = Size(size.width * 0.055f, size.height * 0.13f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.sp,
            ),
            color = Color(0xFFF6D638),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 60.dp, start = 16.dp, end = 16.dp),
        )
        Surface(
            color = Color(0xFF56A9D4),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 22.dp),
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
        modifier = modifier.background(Color(0xFF10212B)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0C1C26),
                        0.56f to Color(0xFF1F3847),
                        0.72f to Color(0xFF6E8596),
                        1.0f to Color(0xFF7C92A3),
                    ),
                ),
            )
            // Ground shadow
            drawOval(
                color = Color(0xAA0A161E),
                topLeft = Offset(size.width * 0.14f, size.height * 0.69f),
                size = Size(size.width * 0.72f, size.height * 0.065f),
            )
            // Ferrari F40-inspired silhouette facing left
            val carBody = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.68f)
                lineTo(size.width * 0.14f, size.height * 0.62f)
                lineTo(size.width * 0.34f, size.height * 0.58f)
                lineTo(size.width * 0.45f, size.height * 0.51f)
                lineTo(size.width * 0.63f, size.height * 0.51f)
                lineTo(size.width * 0.76f, size.height * 0.57f)
                lineTo(size.width * 0.80f, size.height * 0.47f)
                lineTo(size.width * 0.86f, size.height * 0.47f)
                lineTo(size.width * 0.87f, size.height * 0.67f)
                close()
            }
            drawPath(carBody, color = Color(0xFFC81E1E))
            val cabin = Path().apply {
                moveTo(size.width * 0.38f, size.height * 0.57f)
                lineTo(size.width * 0.46f, size.height * 0.52f)
                lineTo(size.width * 0.60f, size.height * 0.52f)
                lineTo(size.width * 0.66f, size.height * 0.57f)
                close()
            }
            drawPath(cabin, color = Color(0xFF1A252D))
            val wheelRadius = size.minDimension * 0.065f
            listOf(0.28f, 0.72f).forEach { cx ->
                drawCircle(
                    color = Color(0xFF121619),
                    radius = wheelRadius,
                    center = Offset(size.width * cx, size.height * 0.68f),
                )
                drawCircle(
                    color = Color(0xFFC5CFD5),
                    radius = wheelRadius * 0.58f,
                    center = Offset(size.width * cx, size.height * 0.68f),
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 66.dp),
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
                        0.0f to Color(0xFF2CB2E8),
                        0.54f to Color(0xFF5DCBF3),
                        1.0f to colors.surface,
                    ),
                ),
            )
            // Clouds
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = size.minDimension * 0.12f,
                center = Offset(size.width * 0.06f, size.height * 0.25f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = size.minDimension * 0.095f,
                center = Offset(size.width * 0.91f, size.height * 0.22f),
            )
            // Rolling emerald hill blending seamlessly into surface
            val hill = Path().apply {
                moveTo(0f, size.height * 0.64f)
                quadraticTo(size.width * 0.45f, size.height * 0.50f, size.width, size.height * 0.66f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hill, color = colors.surface)
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
                .padding(top = 62.dp, start = 20.dp, end = 20.dp),
        )
    }
}

@Composable
private fun ShowEpisodeThumbnail(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(Color(0xFF2CB2E8)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2CB2E8), Color(0xFF5DCBF3)),
                ),
            )
            val hill = Path().apply {
                moveTo(0f, size.height * 0.62f)
                quadraticTo(size.width * 0.5f, size.height * 0.48f, size.width, size.height * 0.64f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hill, color = colors.surface)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = size.minDimension * 0.14f,
                center = Offset(size.width * 0.20f, size.height * 0.24f),
            )
        }
        Text(
            text = title.take(4),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
            ),
            color = Color(0xFFF6D96B),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
        )
    }
}

private fun formatHms(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val hours = clamped / 3600
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
