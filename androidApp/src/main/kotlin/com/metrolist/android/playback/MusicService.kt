package com.metrolist.android.playback

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.metrolist.android.App
import com.metrolist.android.MainActivity
import com.metrolist.android.R
import com.metrolist.android.cast.AndroidMdnsCastDiscovery
import com.metrolist.android.constants.MediaSessionConstants
import com.metrolist.android.data.PlayHistoryRepository
import com.metrolist.android.deviceconnect.AndroidDeviceConnect
import com.metrolist.android.models.PersistPlayerState
import com.metrolist.android.models.PlaybackPersistenceWriter
import com.metrolist.android.models.decodePersistPlayerState
import com.metrolist.android.models.playbackPersistenceJson
import com.metrolist.android.widget.EXTRA_WIDGET_COMMAND_TOKEN
import com.metrolist.android.widget.MetrolistWidgetManager
import com.metrolist.android.widget.MusicWidgetReceiver
import com.metrolist.android.widget.WIDGET_COMMAND_TOKEN
import com.metrolist.android.widget.WidgetControls
import com.metrolist.android.widget.sampleWidgetProgress
import com.metrolist.android.widget.shouldSampleWidgetProgress
import com.metrolist.android.widget.widgetScreenInteractive
import com.metrolist.innertubex.extraction.StreamResolveException
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.sabr.findSabrProtocolException
import com.metrolist.shared.BuildConfig
import com.metrolist.shared.api.PlaylistPage
import com.metrolist.shared.cast.CastConnectionStatus
import com.metrolist.shared.cast.CastDevice
import com.metrolist.shared.cast.CastError
import com.metrolist.shared.cast.CastMedia
import com.metrolist.shared.cast.CastProtocol
import com.metrolist.shared.cast.JvmCastManager
import com.metrolist.shared.cast.castAudioQuality
import com.metrolist.shared.cast.forQueueEntry
import com.metrolist.shared.cast.isPlayableOn
import com.metrolist.shared.cast.shouldHandleCastPlaybackEnd
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.deviceconnect.DcPlaybackAction
import com.metrolist.shared.deviceconnect.DcPlaybackCommand
import com.metrolist.shared.deviceconnect.DcSyncError
import com.metrolist.shared.deviceconnect.DeviceConnectManager
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.playback_unable_to_play_queued_track
import com.metrolist.shared.model.RadioQueueChip
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.isLive
import com.metrolist.shared.model.isPodcast
import com.metrolist.shared.model.previewThumbnailUrl
import com.metrolist.shared.playback.PauseOnMuteController
import com.metrolist.shared.playback.PlaybackErrorContext
import com.metrolist.shared.playback.PlaybackFailure
import com.metrolist.shared.playback.PlaybackFailureCategory
import com.metrolist.shared.playback.PlaybackFailureClassifier
import com.metrolist.shared.playback.PlaybackNetworkRecovery
import com.metrolist.shared.playback.PlaybackRecoveryAction
import com.metrolist.shared.playback.PlaybackRecoveryBudget
import com.metrolist.shared.playback.PlaybackRecoveryPolicy
import com.metrolist.shared.playback.PlaybackSeekSource
import com.metrolist.shared.playback.QueuePlaybackRequest
import com.metrolist.shared.playback.SleepTimerController
import com.metrolist.shared.playback.TokenlessStreamRenewal
import com.metrolist.shared.playback.asManuallyAddedQueueItem
import com.metrolist.shared.playback.enqueueOrStartQueue
import com.metrolist.shared.playback.findPlaybackHttpStatusCode
import com.metrolist.shared.playback.observePlaybackCheckpoints
import com.metrolist.shared.playback.shouldSkipAfterPlaybackFailure
import com.metrolist.shared.playback.sponsorblock.observeSponsorBlockSkipping
import com.metrolist.shared.settings.BlockedContent
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.PodcastLibrary
import com.metrolist.shared.state.PodcastPlaybackProgress
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.state.SongMetadataState
import com.metrolist.shared.utils.normalizePlaybackTempo
import com.metrolist.shared.youtube.benchmark.PlaybackBenchmarkRunner
import com.metrolist.shared.youtube.playback.PlaybackEventKind
import com.metrolist.shared.youtube.playback.PlaybackEventLog
import com.metrolist.shared.youtube.playback.PlaybackNetworkStateStore
import com.metrolist.shared.youtube.playback.PlaybackResource
import com.metrolist.shared.youtube.playback.StreamResolver
import com.metrolist.shared.youtube.playback.matchingLiveState
import com.metrolist.shared.youtube.playback.playbackErrorResource
import com.metrolist.shared.youtube.playback.toPlaybackLogDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeoutException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import android.provider.Settings as AndroidSettings
import com.metrolist.shared.generated.resources.Res as SharedRes
import com.metrolist.shared.generated.resources.shuffle as sharedShuffle
import com.metrolist.shared.generated.resources.start_radio as sharedStartRadio

private const val QUEUE_RESOLVE_RETRY_DELAY_MS = 350L

internal suspend fun observeAndroidScrobbles(
    player: AndroidPlaybackPlayer,
    onState: (SongItem?, Boolean, Boolean) -> Unit,
) {
    var previousInstance = player.currentPlaybackInstanceId
    var previousQueueKey = player.queueState.value.let { it.entries.getOrNull(it.currentIndex)?.key }
    var previousQueueIndex = player.queueState.value.currentIndex
    var previousRequestId =
        player.queuePlaybackRequests.replayCache
            .lastOrNull()
            ?.requestId
    merge(
        combine(
            player.currentSong,
            player.isPlaying,
            player.queueState,
            player.playbackInstanceId,
            player.duration,
        ) { _, _, _, _, _ -> Unit },
        player.queuePlaybackRequests.map { Unit },
    ).map {
        val queue = player.queueState.value
        val queueKey = queue.entries.getOrNull(queue.currentIndex)?.key
        Triple(
            player.currentSong.value?.let { song ->
                val duration =
                    player.duration.value
                        .takeIf { it > 0L }
                        ?.div(1000L)
                        ?.takeIf { it > 0L }
                if (!song.isLive && song.duration?.takeIf { it > 0L } == null && duration != null) song.copy(duration = duration) else song
            } to player.isPlaying.value,
            Triple(queueKey, queue.currentIndex, player.currentPlaybackInstanceId),
            player.queuePlaybackRequests.replayCache
                .lastOrNull()
                ?.requestId,
        )
    }.distinctUntilChanged()
        .collect { (songAndPlaying, queueAndInstance, requestId) ->
            val (song, playing) = songAndPlaying
            val (queueKey, queueIndex, instance) = queueAndInstance
            val queueOccurrenceChanged = queueIndex != previousQueueIndex && queueKey != previousQueueKey
            val newPlayback = requestId != previousRequestId || (queueOccurrenceChanged && instance != previousInstance)
            onState(song, playing, newPlayback)
            // Queue and instance flows may arrive in either order; retain the old occurrence until both change.
            if (!queueOccurrenceChanged || newPlayback) {
                if (queueKey != previousQueueKey || queueIndex != previousQueueIndex || requestId != previousRequestId) {
                    previousInstance = instance
                }
                previousQueueKey = queueKey
                previousQueueIndex = queueIndex
            }
            previousRequestId = requestId
        }
}

class MusicService :
    MediaSessionService(),
    KoinComponent {
    companion object {
        private val playbackPersistenceWriter =
            PlaybackPersistenceWriter(
                writePosition = { Settings.shared.persistentPlaybackPosition = it },
                writeStoredValue = { Settings.shared.persistentPlaybackState = it },
            )
        private var finalPlaybackPersistence: Job? = null
        private const val TAG = "MusicService"
        const val ACTION_OPEN_PLAYER = "action_open_player"
        private const val HEALTHY_PLAYBACK_THRESHOLD_MS = 30_000L
        private const val MAX_HEALTHY_REQUEST_HISTORY = 64
        private const val MAX_CONSECUTIVE_FAILURE_SKIPS = 3
        private const val MAX_RADIO_CONTINUATION_PAGES = 10

        internal val running = MutableStateFlow(false)

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val binder = MusicBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val taskRemoved = MutableStateFlow(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaNotificationProvider: PromotedMediaNotificationProvider? = null
    private var foregroundStartCleanupJob: Job? = null

    @Volatile private var isDestroying = false
    private val playbackBenchmark: PlaybackBenchmarkRunner by inject()
    private val streamResolver: StreamResolver by inject()
    private val tokenProvider: TokenProvider by inject()
    private val audioCacheManager: AudioCacheManager by inject()
    private val database: MusicDatabase by inject()
    private val widgetManager: MetrolistWidgetManager by inject()

    lateinit var player: AndroidPlaybackPlayer
        private set
    lateinit var castManager: JvmCastManager
        private set
    var media3Player: Player? = null
        private set
    private var mediaSession: MediaSession? = null
    private val playbackCommandEvents = MutableStateFlow(0L)
    internal val playbackCommands = playbackCommandEvents.asStateFlow()
    internal val playbackCommandGeneration: Long get() = playbackCommandEvents.value
    private lateinit var playbackRestoreJob: Job

    private lateinit var syncPlayback: AndroidDeviceConnectPlayback
    private val podcastWriterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val podcastProgress by lazy {
        PodcastPlaybackProgress(
            writerScope = podcastWriterScope,
            save = { song, position, duration, metadata ->
                PodcastLibrary.updatePlaybackPosition(database, song, position, duration, metadata)
            },
            onError = { Timber.e(it, "Failed to persist podcast progress") },
        )
    }
    private lateinit var listeningHistory: AndroidListeningHistory
    lateinit var sleepTimerController: SleepTimerController
        private set

    private lateinit var queueCoordinator: AndroidQueueCoordinator
    private lateinit var transitions: AndroidQueueTransitions
    internal val crossfadeState: StateFlow<AndroidCrossfadeState?> get() = transitions.crossfadeState
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    val isQueueLoading: StateFlow<Boolean> get() = queueCoordinator.isQueueLoading
    val isReconnecting = MutableStateFlow(false)
    val isPlaybackLoading: StateFlow<Boolean> get() = queueCoordinator.isPlaybackLoading
    val queueTitle: StateFlow<String?> get() = queueCoordinator.queueTitle

    internal fun setQueueTitleFromRemote(title: String?) = queueCoordinator.setTitle(title)

    val queueChips: StateFlow<List<RadioQueueChip>> get() = queueCoordinator.queueChips

    private val castDisconnectSuppression = CastDisconnectSuppression()
    private var streamRecoveryVideoId: String? = null
    private var streamRecoveryBudget: PlaybackRecoveryBudget? = null
    private var consecutiveFailureSkips = 0
    private val healthyQueueRequestIds = linkedSetOf<Long>()

    private var lastPersistenceState: PersistPlayerState? = null

    // The service-owned channel can be cancelled before its clear write runs; keep the request for the final checkpoint.
    @Volatile private var pendingPlaybackClear: PlaybackPersistenceWriter.Request? = null
    private val playbackPersistenceRequests = Channel<PlaybackPersistenceWriter.Request>(Channel.CONFLATED)

    private val playbackPersistenceLifecycle = PlaybackPersistenceLifecycle()

    private lateinit var audioManager: AudioManager
    private val connectedAudioOutputs = mutableSetOf<Int>()
    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                val newOutputs =
                    addedDevices.filter { it.isSink && connectedAudioOutputs.add(it.id) }.filter { device ->
                        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && device.type == AudioDeviceInfo.TYPE_HEARING_AID) ||
                            (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                            ) ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
                    }

                if (newOutputs.isEmpty() || !canResumeOnBluetoothConnect()) return
                resumeOnBluetoothConnect(
                    BluetoothResume(
                        newOutputs.mapTo(mutableSetOf()) { it.id },
                        player.latestQueuePlaybackRequestId,
                        playbackCommandGeneration,
                    ),
                )
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                removedDevices.forEach { connectedAudioOutputs.remove(it.id) }
                if (pendingBluetoothResume?.outputs?.none { it in connectedAudioOutputs } == true) pendingBluetoothResume = null
            }
        }

    private class BluetoothResume(
        val outputs: Set<Int>,
        val requestId: Long,
        val commandGeneration: Long,
    )

    private var pendingBluetoothResume: BluetoothResume? = null

    private fun canResumeOnBluetoothConnect() =
        !isDestroying && isRunning && Settings.shared.resumeOnBluetoothConnect &&
            playbackRestoreJob.isCompleted && player.playbackState == Player.STATE_READY && !player.wantsPlayback &&
            player.currentSong.value != null && castManager.state.value.status == CastConnectionStatus.DISCONNECTED &&
            !AndroidDeviceConnect.manager.remote.willStartRemotePlayback

    private fun BluetoothResume.isCurrent() =
        canResumeOnBluetoothConnect() && player.latestQueuePlaybackRequestId == requestId &&
            playbackCommandGeneration == commandGeneration && outputs.any { it in connectedAudioOutputs }

    private fun BluetoothResume.withPendingOutputs(): BluetoothResume {
        val pending = pendingBluetoothResume
        return if (pending != null && pending.requestId == requestId && pending.commandGeneration == commandGeneration) {
            BluetoothResume(outputs + pending.outputs.filter { it in connectedAudioOutputs }, requestId, commandGeneration)
        } else {
            this
        }
    }

    private fun resumeOnBluetoothConnect(request: BluetoothResume) {
        serviceScope.launch {
            if (!request.isCurrent()) return@launch
            val resume = request.withPendingOutputs()
            pendingBluetoothResume = null
            val lifecycle = ProcessLifecycleOwner.get().lifecycle
            val startedBeforePromotion = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && !promotePlaybackToForeground()) {
                // Android 15 grants audio focus only to a foreground service or the top app, so retry when the app is next started.
                if (resume.isCurrent()) {
                    val deferred = resume.withPendingOutputs()
                    pendingBluetoothResume = deferred
                    // The app can become started before its rejected promotion completes.
                    if (!startedBeforePromotion && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        resumeOnBluetoothConnect(deferred)
                    }
                }
                return@launch
            }
            if (resume.isCurrent()) media3Player?.play() else triggerNotificationUpdate()
        }
    }

    private val pauseOnMute = PauseOnMuteController()
    private val volumeMuteObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(
                selfChange: Boolean,
                uri: Uri?,
            ) {
                handleDeviceVolumeChanged()
            }
        }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder? =
        if (!::player.isInitialized) {
            null
        } else if (intent?.getBooleanExtra("internal_bind", false) == true) {
            binder
        } else {
            super.onBind(intent)
        }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onCreate() {
        super.onCreate()
        if (!(application as App).infrastructureReady) {
            stopSelf()
            return
        }
        isRunning = true

        setupNotificationChannel()
        // An idle session with a restored queue but no current song renders as a blank "is running" card.
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)

        mediaNotificationProvider =
            PromotedMediaNotificationProvider(
                this,
                DefaultMediaNotificationProvider
                    .Builder(this)
                    .setChannelId(PLAYBACK_NOTIFICATION_CHANNEL_ID)
                    .setChannelName(R.string.playback_notification_channel_name)
                    .build()
                    .apply {
                        setSmallIcon(R.drawable.ic_notification)
                    },
            )
        setMediaNotificationProvider(checkNotNull(mediaNotificationProvider))

        castManager =
            JvmCastManager(
                scope = serviceScope,
                discoveryFactory = { AndroidMdnsCastDiscovery(this) },
                localAddresses = ::castNetworkAddresses,
                appVersion = BuildConfig.APP_VERSION,
            )
        player =
            ExoPlaybackPlayer(
                this,
                onPlaybackCommand = ::onPlaybackCommand,
                isVideoTransitionPending = { false },
                isCasting = { castManager.state.value.isConnected },
                sabrPlaybackCache = audioCacheManager.sabrPlaybackCache,
            )
        restorePlaybackParameters()
        queueCoordinator =
            AndroidQueueCoordinator(
                context = this,
                player = player,
                scope = serviceScope,
                _repeatMode = _repeatMode,
                _shuffleModeEnabled = _shuffleModeEnabled,
                onSave = ::savePlaybackState,
                onSessionReset = {
                    streamRecoveryVideoId = null
                    streamRecoveryBudget = null
                },
                castState = { castManager.state.value },
            )
        transitions =
            AndroidQueueTransitions(
                player = player,
                scope = serviceScope,
                audioCacheManager = audioCacheManager,
                queue = queueCoordinator,
                fetchPlaybackResource = { song, _ -> fetchPlaybackResource(song).resource },
                episodeResumePosition = { song, duration ->
                    withContext(Dispatchers.IO) { PodcastLibrary.resumePosition(database, song, durationMs = duration) }
                },
                onPreloadFailure = { resource, song, _, error ->
                    streamResolver.reportPlaybackFailure(
                        videoId = resource.videoId,
                        clientName = resource.clientName,
                        responseCode = error.findPlaybackHttpStatusCode(),
                        profileId = resource.profileId,
                        hints = song.toAndroidContentHints(wantVideo = false),
                        category = PlaybackFailureClassifier.fromThrowable(error),
                    )
                },
                onAdvance = { advanceQueueAfterCurrent() },
            )
        queueCoordinator.transitions = transitions
        media3Player =
            MediaSessionPlayer(
                artworkUri = { mediaArtworkUri(this, it) },
                currentArtworkUri = { mediaArtworkUri(this, it, isCurrent = true) },
                currentArtworkAuthority = "$packageName.media-artwork",
                musicPlayer = player,
                repeatMode = _repeatMode,
                shuffleModeEnabled = _shuffleModeEnabled,
                remotePlaybackState = castManager.state,
                onRemotePlaybackChange = ::setCastPlayback,
                onRemoteStop = ::stopCastPlayback,
                onRemoteSeek = ::seekCastTo,
                onRemoteVolumeChange = ::setCastVolume,
                onQueueChanged = ::synchronizeMediaSessionQueue,
                onQueueCleared = ::clearPersistedPlaybackState,
                extendPlaylistBeforeSkip = ::extendPlaylistBeforeSkip,
                deviceConnectInterceptor = AndroidDeviceConnect::intercept,
                isDeviceConnectFollowing = { AndroidDeviceConnect.manager.remote.isFollowing },
                scope = serviceScope,
                awaitLikes = { (application as App).startup.await() },
            )
        listeningHistory = AndroidListeningHistory(player, serviceScope, getKoin().get<PlayHistoryRepository>()::recordLocalPlay)
        syncPlayback =
            AndroidDeviceConnectPlayback(
                context = this,
                player = player,
                queue = queueCoordinator,
                transitions = transitions,
                history = listeningHistory,
                repeatMode = _repeatMode,
                shuffleModeEnabled = _shuffleModeEnabled,
                fetchPlaybackResource = { song, _ -> fetchPlaybackResource(song).resource },
                isCasting = { castManager.state.value.status != CastConnectionStatus.DISCONNECTED },
                onSave = ::savePlaybackState,
                onQueueCleared = ::clearPersistedPlaybackState,
            )
        sleepTimerController =
            SleepTimerController(
                scope = serviceScope,
                player =
                    MusicSleepTimerPlayer(
                        player,
                        pausePlayback = { media3Player?.pause() },
                        hasRemotePlaybackRoute = { castManager.state.value.isConnected || AndroidDeviceConnect.manager.remote.isFollowing },
                    ) {
                        transitions.cancelAndroidCrossfade()
                        transitions.cancelNextTrackPreload()
                    },
                elapsedRealtimeMs = SystemClock::elapsedRealtime,
            )
        player.startPreloadedNextAtEof = { completedInstance ->
            if (!sleepTimerController.isStopAfterCurrentSong) queueCoordinator.tryStartPreloadedNextAtEof(completedInstance)
        }

        registerVolumeMuteObserver()
        setupMediaSession()
        observePlaybackEvents()
        observeCastState()
        listeningHistory.start()

        playbackRestoreJob =
            serviceScope.launch {
                (application as App).startup.await()
                if (Settings.shared.hasSavedVolume) player.setVolume(Settings.shared.volume)
                playbackPersistenceLifecycle.restore { restorePersistedPlayback() }
                savePlaybackState()
                AndroidDeviceConnect.manager.remote.attach(syncPlayback.port)
            }
        startPlaybackPersistence()
        startWidgetUpdates()
        startWidgetProgressUpdates()
        observeSettingsChanges()

        running.value = true
        observeServiceLifetime()
        // Registration reports existing devices too; only a later connection should resume playback.
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapTo(connectedAudioOutputs) { it.id }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        Timber.tag(TAG).d("MusicService created backend=Media3")
    }

    private fun observeServiceLifetime() {
        serviceScope.launch {
            ProcessLifecycleOwner.get().lifecycle.currentStateFlow.collect { state ->
                if (state.isAtLeast(Lifecycle.State.STARTED)) {
                    taskRemoved.value = false
                    pendingBluetoothResume?.let(::resumeOnBluetoothConnect)
                }
            }
        }
        serviceScope.launch {
            awaitPlaybackRestore()
            val localPlaybackRequired =
                combine(player.wantsPlaybackState, player.playbackStateFlow) { requested, state ->
                    requested && state != Player.STATE_IDLE && state != Player.STATE_ENDED
                }
            val playbackRequired =
                combine(
                    localPlaybackRequired,
                    isPlaybackLoading,
                    castManager.state,
                    AndroidDeviceConnect.manager.state,
                ) { requested, loading, cast, devices ->
                    requested || loading || cast.isConnected ||
                        devices.peers.any { it.state == DeviceConnectManager.PeerState.CONNECTED }
                }
            observePlaybackServiceLifetime(taskRemoved, playbackRequired, snapshotFlow { Settings.shared.stopMusicOnTaskClear }) {
                saveFinalPlaybackState()?.join()
                isRunning = false
                running.value = false
                pauseAllPlayersAndStopSelf()
            }
        }
    }

    internal fun restorePlaybackParameters() {
        val speed = normalizePlaybackTempo(Settings.shared.playbackSpeed) ?: 1f
        val pitch = normalizePlaybackTempo(Settings.shared.playbackPitch) ?: 1f
        Settings.shared.playbackSpeed = speed
        Settings.shared.playbackPitch = pitch
        player.setTempoPitch(speed, pitch)
    }

    internal fun onPlaybackCommand() {
        playbackCommandEvents.update { it + 1 }
        playbackBenchmark.cancel()
        pauseOnMute.cancelResume()
    }

    private fun registerVolumeMuteObserver() {
        audioManager = getSystemService(AudioManager::class.java)
        contentResolver.registerContentObserver(
            AndroidSettings.System.CONTENT_URI,
            true,
            volumeMuteObserver,
        )
        handleDeviceVolumeChanged()
    }

    private fun handleDeviceVolumeChanged() {
        fun ownsLocalPlayback() =
            !castManager.state.value.isConnected &&
                !AndroidDeviceConnect.manager.remote.isFollowing

        val muted =
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 ||
                audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
        pauseOnMute.onVolumeChanged(
            muted = muted,
            enabled = Settings.shared.pauseOnMute,
            playing =
                playbackActiveForDeviceMute(
                    isPlaying = player.isPlaying.value,
                    wantsPlayback = player.wantsPlayback,
                    playbackState = player.playbackState,
                    hasSong = player.currentSong.value != null,
                    isLocalPlayback = ::ownsLocalPlayback,
                ),
            pause = player::pause,
            resume = { if (ownsLocalPlayback()) player.resume() },
        )
    }

    private fun castNetworkAddresses(): List<InetAddress> {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val network = connectivityManager.activeNetwork ?: return emptyList()
        return connectivityManager
            .getLinkProperties(network)
            ?.linkAddresses
            ?.map { it.address }
            ?.filterNot { it.isAnyLocalAddress || it.isLoopbackAddress || it.isMulticastAddress }
            .orEmpty()
    }

    private fun observeCastState() {
        serviceScope.launch {
            var wasConnected = false
            var handledEndGeneration: Long? = null
            castManager.state.collect { state ->
                val suppressConnectionLoss = castDisconnectSuppression.onConnectionState(state.isConnected)
                val connectionLost = wasConnected && !state.isConnected && !suppressConnectionLoss
                wasConnected = state.isConnected
                if (!state.isConnected) {
                    handledEndGeneration = null
                }
                if (connectionLost) restoreLocalPlaybackAfterCast(state.positionMs, state.isPlaying)
                if (shouldHandleCastPlaybackEnd(state, handledEndGeneration)) {
                    handledEndGeneration = state.mediaGeneration
                    handlePlaybackCompletion()
                }
            }
        }
    }

    fun startCastDiscovery() = castManager.startDiscovery()

    fun stopCastDiscovery() = castManager.stopDiscovery()

    private fun canStartCast(): Boolean {
        val sync = AndroidDeviceConnect.manager.remote
        if (!sync.isActive) return true
        sync.reportError(DcSyncError.CONFLICT)
        return false
    }

    fun castTo(device: CastDevice) {
        if (!Settings.shared.castEnabled) return
        if (!canStartCast()) return
        val song = player.currentSong.value
        if (song == null) {
            castManager.showError(CastError.NO_MEDIA)
            return
        }
        val sessionId = queueCoordinator.queueSessionId
        val requestId = player.latestQueuePlaybackRequestId
        val playbackInstanceId = player.currentPlaybackInstanceId

        fun isCurrentHandoff(): Boolean =
            isCurrentQueuePlaybackTarget(
                sessionId,
                queueCoordinator.queueSessionId,
                requestId,
                player.latestQueuePlaybackRequestId,
                song.id,
                player.currentSong.value?.id,
            ) && player.currentPlaybackInstanceId == playbackInstanceId
        serviceScope.launch {
            val resource = resolveCastResource(song, device.protocol)
            if (!isCurrentHandoff()) return@launch
            if (!canStartCast()) return@launch
            if (resource == null) {
                castManager.showError(CastError.UNSUPPORTED_STREAM)
                return@launch
            }
            val previousCastState = castManager.state.value.takeIf { it.isConnected }
            val position = previousCastState?.positionMs ?: player.getRealtimePositionMs()
            val playing = previousCastState?.isPlaying ?: player.isPlaying.value
            val duration = previousCastState?.durationMs?.takeIf { it > 0L } ?: player.duration.value
            val volume = previousCastState?.volume ?: player.volume.value
            if (previousCastState == null && playing) player.pause()
            previousCastState?.let { castDisconnectSuppression.beginReplacement() }
            val connected =
                try {
                    castManager.cast(
                        device = device,
                        media =
                            CastMedia(
                                song,
                                song.previewThumbnailUrl,
                                resource,
                                duration,
                                player.queueState.value.let {
                                    it.entries.getOrNull(it.currentIndex)
                                },
                            ),
                        positionMs = position,
                        autoplay = playing,
                        volume = volume,
                        shouldCommit = ::isCurrentHandoff,
                    )
                } finally {
                    previousCastState?.let {
                        castDisconnectSuppression.finishReplacement(
                            disconnected = !castManager.state.value.isConnected,
                        )
                    }
                }
            if (!connected && isCurrentHandoff()) {
                restoreLocalPlaybackAfterCast(position, playing)
            }
        }
    }

    private suspend fun resolveCastResource(
        song: SongItem,
        protocol: CastProtocol?,
    ): PlaybackResource? =
        player.currentPlaybackResource.value?.takeIf {
            it.videoId == song.id && it.clientName != "local" && !it.isSabr && it.contentLengthBytes != null && it.mimeType != null &&
                it.isPlayableOn(protocol)
        } ?: withContext(Dispatchers.IO) {
            runCatching {
                streamResolver.resolveRemote(
                    song.id,
                    song.toAndroidContentHints(wantVideo = false).copy(
                        playbackClientOverrideId = null,
                        sabrFirst = false,
                    ),
                    castAudioQuality(protocol) ?: playbackAudioQuality(),
                    sourceSong = song,
                )
            }.getOrNull()
        }

    fun disconnectCast() {
        serviceScope.launch {
            val remoteState = castManager.state.value
            castDisconnectSuppression.beginExplicitDisconnect()
            try {
                castManager.disconnect()
            } finally {
                castDisconnectSuppression.finishExplicitDisconnect(
                    disconnected = !castManager.state.value.isConnected,
                )
            }
            restoreLocalPlaybackAfterCast(remoteState.positionMs, remoteState.isPlaying)
        }
    }

    private fun shouldPauseLocalPlaybackInBackground(): Boolean = false

    private fun restoreLocalPlaybackAfterCast(
        positionMs: Long,
        resumePlayback: Boolean,
    ) {
        // Returning from Cast while the TV app is in the background must not undo its pause-on-background rule.
        val resume = resumePlayback && !shouldPauseLocalPlaybackInBackground()
        if (player.retryPendingQueuePlayback(positionMs, resume)) return
        player.seekTo(positionMs, PlaybackSeekSource("queue_restore", mapOf("reason" to "cast_disconnect")))
        if (resume) player.resume() else player.pause()
    }

    fun toggleCastPlayback() = setCastPlayback(!castManager.state.value.isPlaying)

    fun setCastPlayback(playing: Boolean) {
        serviceScope.launch { if (playing) castManager.play() else castManager.pause() }
    }

    fun stopCastPlayback() {
        serviceScope.launch { castManager.stop() }
    }

    fun seekCastTo(positionMs: Long) {
        serviceScope.launch {
            castManager.seekTo(positionMs)
            updateWidgetProgress()
        }
    }

    fun seekTo(
        positionMs: Long,
        source: PlaybackSeekSource = PlaybackSeekSource.Other,
    ) {
        if (player.currentSong.value?.isLive == true) return
        if (transitions.isCrossfading) transitions.cancelAndroidCrossfade()
        player.seekTo(positionMs, source)
    }

    fun setCastVolume(volume: Float) {
        serviceScope.launch { castManager.setVolume(volume) }
    }

    private fun observeSettingsChanges() {
        serviceScope.launch {
            snapshotFlow { Settings.shared.pauseOnMute }.collect {
                pauseOnMute.reset()
                handleDeviceVolumeChanged()
            }
        }
    }

    private fun setupNotificationChannel() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat
                .Builder(PLAYBACK_NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.playback_notification_channel_name))
                .build(),
        )
    }

    private fun setupMediaSession() {
        val sessionActivityPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_OPEN_PLAYER
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        mediaSession =
            MediaSession
                .Builder(this, media3Player!!)
                .setCallback(PlaybackSessionCallback())
                .setSessionActivity(sessionActivityPendingIntent)
                .setMediaButtonPreferences(
                    ImmutableList.of(createLikeButton(), createRepeatButton(), createShuffleButton()),
                ).build()
                .also(::addSession)

        serviceScope.launch {
            (application as App).startup.await()
            val startRadioLabel = getString(SharedRes.string.sharedStartRadio)
            combine(player.currentSong, SongLikeState.likedSongsFlow, _repeatMode, _shuffleModeEnabled) { song, _, repeatMode, shuffle ->
                listOf(song?.id, song?.id?.let(SongLikeState::isLiked), repeatMode, shuffle)
            }.distinctUntilChanged().collect {
                mediaSession?.setMediaButtonPreferences(
                    ImmutableList.copyOf(
                        listOf(
                            createLikeButton(),
                            createRepeatButton(),
                            createShuffleButton(),
                            createRadioButton(startRadioLabel),
                        ),
                    ),
                )
            }
        }
    }

    private fun observePlaybackEvents() {
        serviceScope.launch {
            combine(
                player.isPlaying,
                player.wantsPlaybackState,
                player.playbackStateFlow,
                player.currentSong,
                player.duration,
            ) { _, _, _, _, _ -> Unit }.collect { AndroidDeviceConnect.manager.remote.localStateChanged() }
        }
        serviceScope.launch {
            merge(
                player.currentSong.map { Unit },
                player.currentPosition.map { Unit },
                player.isPlaying.map { Unit },
            ).collect { updatePodcastProgress() }
        }
        serviceScope.launch {
            observeSponsorBlockSkipping(
                currentSong = player.currentSong,
                currentPosition = player.currentPosition,
                duration = player.duration,
                isPlaying = player.isPlaying,
                enabled = snapshotFlow { Settings.shared.sponsorBlockEnabled },
                canSeek = {
                    Settings.shared.sponsorBlockEnabled && !castManager.state.value.isConnected &&
                        !AndroidDeviceConnect.manager.remote.isActive
                },
                seekTo = { position ->
                    updatePendingRestorePosition(position)
                    player.seekTo(position, PlaybackSeekSource("sponsorblock"))
                },
                onSkipped = { PlaybackSnackbarEvents.emit(getString(R.string.sponsorblock_skipped)) },
            )
        }
        serviceScope.launch {
            releaseIdleTokenProvider(player.wantsPlaybackState, isPlaybackLoading, tokenProvider)
        }
        serviceScope.launch {
            player.isPlaying.collectLatest { isPlaying ->
                if (isPlaying) {
                    Settings.shared.pendingContinuePlayback = false
                    pauseOnMute.cancelResume()
                }
            }
        }

        serviceScope.launch {
            player.queuePlaybackRequests.collectLatest { request ->
                try {
                    playQueueRequest(request)
                } finally {
                    player.finishPlaybackTransition(request.requestId)
                }
            }
        }

        serviceScope.launch {
            PlaybackNetworkStateStore.state.collect { network ->
                if (network.connected) player.invalidateActiveStreamForNetworkChange()
            }
        }

        serviceScope.launch {
            TokenlessStreamRenewal.observe(
                player.currentPlaybackResource,
                player.currentPosition,
                isActive = {
                    player.wantsPlayback && !player.playbackTransitionPending.value && !transitions.isCrossfading &&
                        !castManager.state.value.isConnected && !AndroidDeviceConnect.manager.remote.isFollowing
                },
            ) { resource ->
                val song = player.currentSong.value ?: return@observe false
                val instance = player.currentPlaybackInstanceId
                val request = player.latestQueuePlaybackRequestId
                val isCurrent = {
                    player.currentPlaybackInstanceId == instance && player.latestQueuePlaybackRequestId == request &&
                        player.currentPlaybackResource.value === resource && !player.playbackTransitionPending.value &&
                        !transitions.isCrossfading && !castManager.state.value.isConnected &&
                        !AndroidDeviceConnect.manager.remote.isFollowing
                }
                TokenlessStreamRenewal.renew(
                    resource,
                    player::getRealtimePositionMs,
                    isCurrent,
                    resolve = { excluded ->
                        streamResolver.resolveRemote(
                            song.id,
                            song
                                .toAndroidContentHints(wantVideo = false)
                                .copy(playbackClientOverrideId = null),
                            playbackAudioQuality(),
                            song,
                            refresh = true,
                            excludedProfiles = excluded,
                        )
                    },
                ) { fresh ->
                    player.prepareAndPlay(
                        song,
                        fresh,
                        positionMs = player.getRealtimePositionMs(),
                        resumePlayback = player.wantsPlayback,
                        positionMsProvider = player::getRealtimePositionMs,
                        playWhenReadyProvider = { player.wantsPlayback },
                        shouldCommit = isCurrent,
                        recoveryPlaybackInstanceId = instance,
                        seekSource = PlaybackSeekSource("recovery", mapOf("reason" to "tokenless_renewal")),
                    )
                }
            }
        }

        serviceScope.launch {
            player.streamFailure.collectLatest { request ->
                try {
                    (media3Player as? MediaSessionPlayer)?.setTerminalError(null)
                    handleStreamFailure(request.failure, request.freshAudioSession)
                } finally {
                    player.finishPlaybackRecovery(request.transitionId)
                }
            }
        }

        serviceScope.launch {
            player.onEOFPlaybackInstance.collect(::handlePlaybackCompletion)
        }
        serviceScope.launch {
            combine(player.currentSong, player.currentPlaybackResource) { _, _ ->
                player.currentSong.value to player.currentPlaybackResource.value
            }.distinctUntilChanged().collect { (song, resource) ->
                when {
                    song == null -> {
                        PlaybackErrorReporter.clearActivePlayback()
                    }

                    resource?.videoId == song.id -> {
                        PlaybackErrorReporter.setActivePlayback(song, resource)
                    }

                    resource != null -> {
                        PlaybackEventLog.warn(
                            PlaybackEventKind.PLAYER,
                            "Android playback media and metadata IDs differ.",
                            videoId = resource.videoId,
                            details =
                                mapOf(
                                    "mediaId" to resource.videoId,
                                    "metadataId" to song.id,
                                    "queueRequestId" to player.latestQueuePlaybackRequestId.toString(),
                                    "playbackInstanceId" to player.currentPlaybackInstanceId.toString(),
                                    "isPlaying" to player.isPlaying.value.toString(),
                                    "transitionPending" to player.playbackTransitionPending.value.toString(),
                                ),
                        )
                    }
                }
            }
        }
        transitions.observe(sleepTimerController)
        queueCoordinator.observeAutoLoadMore()
        serviceScope.launch {
            combine(
                player.currentPosition
                    .map {
                        it >= HEALTHY_PLAYBACK_THRESHOLD_MS
                    }.distinctUntilChanged(),
                player.currentPlaybackResource,
                player.currentSong,
            ) { healthy, resource, song ->
                Triple(healthy, resource, song)
            }.collectLatest { (healthy, resource, song) ->
                val requestId = player.latestQueuePlaybackRequestId
                if (
                    healthy &&
                    resource != null &&
                    song != null &&
                    requestId !in healthyQueueRequestIds
                ) {
                    consecutiveFailureSkips = 0
                    healthyQueueRequestIds += requestId
                    while (healthyQueueRequestIds.size > MAX_HEALTHY_REQUEST_HISTORY) {
                        healthyQueueRequestIds.remove(healthyQueueRequestIds.first())
                    }
                    withContext(Dispatchers.IO) {
                        streamResolver.reportPlaybackSuccess(
                            profileId = resource.profileId,
                            hints = song.toAndroidContentHints(wantVideo = false),
                        )
                    }
                }
            }
        }
    }

    private fun playbackErrorCode(error: Throwable?): Int {
        val requiresLogin =
            generateSequence(error) { it.cause?.takeUnless { cause -> cause === it } }
                .filterIsInstance<StreamResolveException>()
                .any { failure -> failure.diagnostics?.attempts?.any { it.outcome == "playability:LOGIN_REQUIRED" } == true }
        if (requiresLogin) return PlaybackException.ERROR_CODE_AUTHENTICATION_EXPIRED
        return when (PlaybackFailureClassifier.fromThrowable(error)) {
            PlaybackFailureCategory.NETWORK -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            PlaybackFailureCategory.TIMEOUT -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            PlaybackFailureCategory.DECODER -> PlaybackException.ERROR_CODE_DECODING_FAILED
            PlaybackFailureCategory.UNAVAILABLE -> PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            else -> PlaybackException.ERROR_CODE_UNSPECIFIED
        }
    }

    private fun publishTerminalPlaybackError(error: Throwable?) {
        val code = playbackErrorCode(error)
        val needsLogin = code == PlaybackException.ERROR_CODE_AUTHENTICATION_EXPIRED
        player.pause()
        val extras =
            Bundle().apply {
                if (needsLogin) {
                    putString(MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL_COMPAT, getString(R.string.sign_in))
                    putParcelable(
                        MediaConstants.EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT_COMPAT,
                        PendingIntent.getActivity(
                            this@MusicService,
                            0,
                            Intent(this@MusicService, MainActivity::class.java).setAction(Intent.ACTION_APPLICATION_PREFERENCES),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
            }
        val message = getString(if (needsLogin) R.string.sign_in_to_youtube_music else R.string.playback_unable_to_play_queued_track)
        (media3Player as? MediaSessionPlayer)?.setTerminalError(PlaybackException(message, error, code, extras))
    }

    private suspend fun skipFailedTrackOrStop(error: Throwable?) {
        if (AndroidDeviceConnect.manager.remote.isFollowing ||
            consecutiveFailureSkips >= MAX_CONSECUTIVE_FAILURE_SKIPS
        ) {
            publishTerminalPlaybackError(error)
            return
        }
        consecutiveFailureSkips++
        // A terminal error would idle the session while the next track plays and drop its notification.
        advanceQueueAfterCurrent()
    }

    private suspend fun handleStreamFailure(
        failure: PlaybackFailure,
        freshAudioSession: Boolean = false,
    ) {
        Timber.tag(TAG).e("Playback failure stage=%s category=%s", failure.stage, failure.category)
        val song = player.currentSong.value ?: return
        val videoId = failure.videoId ?: song.id
        val failedPlaybackInstanceId = failure.playbackInstanceId ?: return
        val queueRequestId = player.latestQueuePlaybackRequestId
        if (
            videoId != song.id ||
            failedPlaybackInstanceId != player.currentPlaybackInstanceId
        ) {
            return
        }
        if (freshAudioSession) {
            val resource = player.currentPlaybackResource.value ?: return
            if (transitions.crossfadeState.value != null) return
            transitions.cancelNextTrackPreload()
            val previousSessionId = player.getAudioSessionId()
            val recovered =
                try {
                    player.prepareAndPlay(
                        song = song,
                        resource = resource,
                        positionMs = player.getRealtimePositionMs(),
                        resumePlayback = player.wantsPlayback,
                        positionMsProvider = player::getRealtimePositionMs,
                        playWhenReadyProvider = { player.wantsPlayback },
                        shouldCommit = {
                            player.currentPlaybackInstanceId == failedPlaybackInstanceId &&
                                player.latestQueuePlaybackRequestId == queueRequestId &&
                                player.currentPlaybackResource.value === resource &&
                                transitions.crossfadeState.value == null
                        },
                        recoveryPlaybackInstanceId = failedPlaybackInstanceId,
                        freshAudioSession = true,
                        seekSource = PlaybackSeekSource("recovery", mapOf("reason" to failure.message)),
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    PlaybackEventLog.warn(PlaybackEventKind.RECOVERY, "Android Bluetooth output replacement failed.", error = error)
                    false
                }
            PlaybackEventLog.info(
                PlaybackEventKind.RECOVERY,
                "Android Bluetooth output replacement finished.",
                videoId = videoId,
                details =
                    mapOf(
                        "recovered" to recovered.toString(),
                        "previousAudioSessionId" to previousSessionId.toString(),
                        "audioSessionId" to player.getAudioSessionId().toString(),
                        "playbackPositionMs" to player.getRealtimePositionMs().toString(),
                    ),
            )
            return
        }
        if (isCorruptCachedAudioFailure(player.currentPlaybackResource.value, failure.cause)) {
            val removed = audioCacheManager.removeCorruptCache(videoId)
            PlaybackEventLog.warn(
                PlaybackEventKind.RECOVERY,
                "Android cached audio could not be parsed.",
                videoId = videoId,
                details = mapOf("cacheRemoved" to removed.toString()),
                error = failure.cause,
            )
        }
        transitions.cancelAndroidCrossfade()
        transitions.cancelNextTrackPreload()
        if (!PlaybackRecoveryPolicy.canRecover(failure)) {
            failure.cause?.let { cause ->
                PlaybackErrorReporter.report(
                    cause,
                    PlaybackErrorContext(stage = failure.stage, videoId = videoId, recovered = false, song = song),
                )
            }
            player.finishInterruptedPlayback(failedPlaybackInstanceId, failure.playbackPositionMs)
            if (shouldAdvanceAfterPlaybackFailure(
                    player.wantsPlayback,
                    failure.category,
                    PlaybackNetworkStateStore.current.connected,
                    repeatMode = repeatMode.value,
                    autoSkipNextOnError = Settings.shared.autoSkipNextOnError,
                )
            ) {
                skipFailedTrackOrStop(failure.cause)
            } else {
                publishTerminalPlaybackError(failure.cause)
            }
            return
        }

        if (streamRecoveryVideoId != videoId) {
            streamRecoveryVideoId = videoId
            streamRecoveryBudget = PlaybackRecoveryBudget.starting(SystemClock.elapsedRealtime())
        }

        var failedResource = player.currentPlaybackResource.value
        var hints = song.toAndroidContentHints(wantVideo = false)
        var lastFailure = failure.cause ?: IllegalStateException(failure.message)
        var lastHttpStatusCode = failure.httpStatusCode ?: lastFailure.findMedia3HttpStatusCode()
        var failureCategory =
            failure.category.takeUnless { it == PlaybackFailureCategory.UNKNOWN }
                ?: lastHttpStatusCode?.let(PlaybackFailureClassifier::fromHttpStatus)
                ?: lastFailure.media3PlaybackFailureCategory()
        val excludedProfiles =
            PlaybackRecoveryPolicy
                .recoveryExclusions(
                    failureCategory,
                    failedResource,
                    failure.playbackPositionMs,
                ).toMutableSet()
        var budget =
            (streamRecoveryBudget ?: PlaybackRecoveryBudget.starting(SystemClock.elapsedRealtime()))
                .forPlaybackFailure(failure.playbackPositionMs, SystemClock.elapsedRealtime(), failureCategory)
        var retryNetworkResource = true
        PlaybackEventLog.warn(
            kind = PlaybackEventKind.RECOVERY,
            message = "Android playback recovery started.",
            videoId = videoId,
            clientName = failedResource?.clientName,
            profileId = failedResource?.profileId,
            details = failure.toPlaybackLogDetails(),
            error = failure.cause,
        )
        while (
            !(failure.isMediaSourceFailure && failureCategory == PlaybackFailureCategory.NETWORK) &&
            budget.canStart(SystemClock.elapsedRealtime()) &&
            player.currentPlaybackInstanceId == failedPlaybackInstanceId &&
            player.latestQueuePlaybackRequestId == queueRequestId &&
            player.currentSong.value?.id == videoId
        ) {
            val decision =
                PlaybackRecoveryPolicy.decide(
                    category = failureCategory,
                    budget = budget,
                    nowMs = SystemClock.elapsedRealtime(),
                    resource = failedResource,
                    positionMs = failure.playbackPositionMs,
                )
            budget = decision.budget
            if (decision.action == PlaybackRecoveryAction.NONE) break
            excludedProfiles += PlaybackRecoveryPolicy.recoveryExclusions(failureCategory, failedResource, failure.playbackPositionMs)
            PlaybackEventLog.info(
                kind = PlaybackEventKind.RECOVERY,
                message = "Trying Android playback recovery action ${decision.action}.",
                videoId = videoId,
                clientName = failedResource?.clientName,
                profileId = failedResource?.profileId,
                details =
                    mapOf(
                        "recoveryAction" to decision.action.name,
                        "failureCategory" to failureCategory.name,
                        "httpStatus" to (lastHttpStatusCode?.toString() ?: "unknown"),
                        "attemptsRemaining" to budget.attemptsRemaining.toString(),
                        "requestsRemaining" to budget.requestsRemaining.toString(),
                    ),
            )

            failedResource
                ?.takeIf {
                    decision.action == PlaybackRecoveryAction.RETRY_CLIENT ||
                        failureCategory == PlaybackFailureCategory.CDN_FORBIDDEN ||
                        failureCategory == PlaybackFailureCategory.SABR_PROTOCOL ||
                        failureCategory == PlaybackFailureCategory.SABR_ATTESTATION
                }?.let { resource ->
                    streamResolver.reportPlaybackFailure(
                        videoId = videoId,
                        clientName = resource.clientName,
                        responseCode = lastHttpStatusCode ?: lastFailure.findPlaybackHttpStatusCode(),
                        profileId = resource.profileId,
                        hints = hints,
                        category = failureCategory,
                    )
                }

            val recoveryResource =
                recoverPlaybackResource(
                    streamResolver = streamResolver,
                    retryNetworkResource = retryNetworkResource,
                    now = Clock.System.now(),
                    action = decision.action,
                    currentResource = failedResource,
                    failure = lastFailure,
                    videoId = videoId,
                    hints = hints,
                    sourceSong = song,
                    budget = budget,
                    positionMs = failure.playbackPositionMs,
                    excludedProfiles = excludedProfiles,
                    category = failureCategory,
                )
            if (decision.action == PlaybackRecoveryAction.INVALIDATE_NETWORK) retryNetworkResource = false
            budget = recoveryResource.budget
            streamRecoveryBudget = budget
            val freshResource = recoveryResource.resource
            if (freshResource == null) {
                lastFailure = recoveryResource.failure ?: lastFailure
                failureCategory = lastFailure.media3PlaybackFailureCategory()
                break
            }

            var prepareFailure: Throwable? = null
            val recovered =
                player.prepareAndPlay(
                    song = song,
                    resource = freshResource,
                    positionMs = player.getRealtimePositionMs(),
                    resumePlayback = player.wantsPlayback,
                    positionMsProvider = player::getRealtimePositionMs,
                    playWhenReadyProvider = { player.wantsPlayback },
                    shouldCommit = {
                        player.currentPlaybackInstanceId == failedPlaybackInstanceId &&
                            player.latestQueuePlaybackRequestId == queueRequestId &&
                            player.currentSong.value?.id == videoId
                    },
                    onPrepareFailure = { prepareFailure = it },
                    recoveryPlaybackInstanceId = failedPlaybackInstanceId,
                    seekSource = PlaybackSeekSource("recovery", mapOf("reason" to failure.stage, "category" to failure.category.name)),
                )
            if (recovered) {
                PlaybackEventLog.info(
                    kind = PlaybackEventKind.RECOVERY,
                    message = "Android playback recovered.",
                    videoId = videoId,
                    clientName = freshResource.clientName,
                    profileId = freshResource.profileId,
                    details =
                        mapOf(
                            "recoveryAction" to decision.action.name,
                            "playbackPositionMs" to failure.playbackPositionMs.toString(),
                            "transport" to if (freshResource.isSabr) "SABR" else "Direct",
                        ),
                )
                return
            }

            failedResource = freshResource
            hints = song.toAndroidContentHints(wantVideo = false)
            lastFailure = prepareFailure ?: IllegalStateException("Refreshed stream could not be prepared")
            lastHttpStatusCode = lastFailure.findMedia3HttpStatusCode() ?: lastFailure.findPlaybackHttpStatusCode()
            failureCategory =
                lastHttpStatusCode?.let(PlaybackFailureClassifier::fromHttpStatus)
                    ?: lastFailure.media3PlaybackFailureCategory()
            if (PlaybackRecoveryPolicy.requiresFreshUrl(failureCategory)) {
                budget = budget.forPlaybackFailure(failure.playbackPositionMs, SystemClock.elapsedRealtime(), failureCategory)
            }
        }

        val networkFailure = PlaybackNetworkRecovery.handles(failure, failureCategory)
        if (networkFailure) {
            val recovered =
                PlaybackNetworkRecovery.recover(
                    isCurrent = {
                        player.currentPlaybackInstanceId == failedPlaybackInstanceId &&
                            player.latestQueuePlaybackRequestId == queueRequestId && player.currentSong.value?.id == videoId
                    },
                    connected = { PlaybackNetworkStateStore.current.connected },
                    wantsPlayback = { player.wantsPlayback },
                    positionMs = player::getRealtimePositionMs,
                    onReconnecting = { isReconnecting.value = it },
                    stateChanges = merge(player.wantsPlaybackState.map { Unit }, PlaybackNetworkStateStore.state.map { Unit }),
                ) {
                    val recoveryContext = currentCoroutineContext()
                    try {
                        streamResolver.invalidateNetworkScope(PlaybackNetworkStateStore.current)
                        val excluded =
                            excludedProfiles +
                                PlaybackRecoveryPolicy.recoveryExclusions(failureCategory, failedResource, failure.playbackPositionMs)
                        val fresh =
                            withContext(Dispatchers.IO) {
                                streamResolver.resolveRemote(
                                    videoId,
                                    if (excluded.isEmpty()) hints else hints.copy(playbackClientOverrideId = null),
                                    playbackAudioQuality(),
                                    sourceSong = song,
                                    refresh = true,
                                    excludedProfiles = excluded,
                                )
                            }
                        val committed =
                            player.prepareAndPlay(
                                song = song,
                                resource = fresh,
                                positionMs = player.getRealtimePositionMs(),
                                resumePlayback = player.wantsPlayback,
                                positionMsProvider = player::getRealtimePositionMs,
                                playWhenReadyProvider = { player.wantsPlayback },
                                shouldCommit = {
                                    recoveryContext.isActive && player.currentPlaybackInstanceId == failedPlaybackInstanceId &&
                                        player.latestQueuePlaybackRequestId == queueRequestId && player.currentSong.value?.id == videoId
                                },
                                onPrepareFailure = { error ->
                                    error?.let { lastFailure = it }
                                    hints = song.toAndroidContentHints(wantVideo = false)
                                },
                                recoveryPlaybackInstanceId = failedPlaybackInstanceId,
                                seekSource =
                                    PlaybackSeekSource(
                                        "recovery",
                                        mapOf("reason" to failure.stage, "category" to failure.category.name),
                                    ),
                            )
                        committed
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        lastFailure = error
                        false
                    }
                }
            if (recovered) {
                streamRecoveryBudget =
                    PlaybackRecoveryBudget
                        .starting(
                            SystemClock.elapsedRealtime(),
                        ).copy(urlRefreshesRemaining = budget.urlRefreshesRemaining)
                return
            }
        }

        if (
            player.currentPlaybackInstanceId == failedPlaybackInstanceId &&
            player.latestQueuePlaybackRequestId == queueRequestId &&
            player.currentSong.value?.id == videoId
        ) {
            Timber.tag(TAG).e("Stream recovery exhausted type=%s", lastFailure::class.simpleName)
            PlaybackErrorReporter.report(
                lastFailure,
                PlaybackErrorContext(
                    stage = "stream_retry_exhausted",
                    videoId = videoId,
                    clientName = failedResource?.clientName,
                    profileId = failedResource?.profileId,
                    recovered = false,
                    song = song,
                    resource = failedResource,
                ),
            )
            PlaybackEventLog.error(
                kind = PlaybackEventKind.RECOVERY,
                message = "Android playback recovery exhausted.",
                videoId = videoId,
                clientName = failedResource?.clientName,
                profileId = failedResource?.profileId,
                details =
                    mapOf(
                        "failureCategory" to failureCategory.name,
                        "httpStatus" to (lastHttpStatusCode?.toString() ?: "unknown"),
                        "attemptsRemaining" to budget.attemptsRemaining.toString(),
                        "requestsRemaining" to budget.requestsRemaining.toString(),
                    ),
                error = lastFailure,
            )
            if (networkFailure) player.pause()
            player.finishInterruptedPlayback(failedPlaybackInstanceId, player.getRealtimePositionMs())
            streamRecoveryBudget = null
            if (shouldAdvanceAfterPlaybackFailure(
                    player.wantsPlayback,
                    failureCategory,
                    PlaybackNetworkStateStore.current.connected,
                    originalCategory = failure.category,
                    repeatMode = repeatMode.value,
                    autoSkipNextOnError = Settings.shared.autoSkipNextOnError,
                )
            ) {
                skipFailedTrackOrStop(lastFailure)
            } else {
                publishTerminalPlaybackError(lastFailure)
            }
        }
    }

    private suspend fun playQueueRequest(
        request: QueuePlaybackRequest,
        stagedShouldCommit: (() -> Boolean)? = null,
    ): Boolean {
        playbackBenchmark.cancel()
        (media3Player as? MediaSessionPlayer)?.setTerminalError(null)
        transitions.cancelAndroidCrossfade()
        transitions.cancelNextTrackPreload()
        val remoteState = castManager.state.value.takeIf { it.isConnected }
        val sessionId = queueCoordinator.queueSessionId

        fun isCurrentRequest(
            queueSongId: String = request.song.id,
            expectedWantVideo: Boolean? = null,
        ): Boolean =
            if (stagedShouldCommit == null) {
                isCurrentQueueRequest(sessionId, request, queueSongId, expectedWantVideo)
            } else {
                stagedShouldCommit() && (expectedWantVideo == null || !expectedWantVideo)
            }
        podcastProgress.flush()?.join()
        if (stagedShouldCommit == null && !isCurrentRequest()) return false
        val requestedPosition =
            if (stagedShouldCommit == null) queueCoordinator.takeRestorePosition(request.positionMs) else request.positionMs
        val targetPosition =
            withContext(Dispatchers.IO) {
                PodcastLibrary.resumePosition(
                    database,
                    request.song,
                    requestedPosition.takeIf {
                        it > 0L || AndroidDeviceConnect.manager.remote.isActive
                    },
                )
            }
        if (!isCurrentRequest()) return false
        if (remoteState != null) {
            val resource = withQueueLoading(isPlaybackLoading = true) { resolveCastResource(request.song, remoteState.device?.protocol) }
            if (!isCurrentRequest()) return false
            if (resource == null) castManager.showError(CastError.UNSUPPORTED_STREAM)
            val remoteAutoplay = remoteState.isPlaying || remoteState.hasEnded
            when (
                routeCastQueueReplacement(
                    suppression = castDisconnectSuppression,
                    isCurrentRequest = { isCurrentRequest() },
                    isConnected = { castManager.state.value.isConnected },
                    replace = {
                        resource != null &&
                            castManager.replaceMedia(
                                media =
                                    CastMedia(
                                        request.song,
                                        request.song.previewThumbnailUrl,
                                        resource,
                                        player.duration.value,
                                        player.queueState.value.let { it.entries.getOrNull(it.currentIndex) },
                                    ),
                                positionMs = targetPosition,
                                autoplay = remoteAutoplay,
                                volume = remoteState.volume,
                            )
                    },
                    disconnect = { castManager.disconnect() },
                    onFailure = { error ->
                        Timber.tag(TAG).w("Cast queue replacement failed type=%s", error::class.simpleName)
                    },
                )
            ) {
                CastQueueReplacementResult.REMOTE -> {
                    streamRecoveryVideoId = null
                    streamRecoveryBudget = null
                    if ((!queueCoordinator.isFiniteQueue || Settings.shared.autoLoadMore) &&
                        queueCoordinator.recommendationBootstrapJob == null
                    ) {
                        bootstrapRecommendations(queueCoordinator.recommendationSeedSongId ?: request.song.id)
                    }
                    return true
                }

                CastQueueReplacementResult.STALE -> {
                    return false
                }

                CastQueueReplacementResult.LOCAL -> {
                    player.setPendingPlaybackIntent(request.requestId, remoteAutoplay)
                }
            }
        }
        if (!PlaybackNetworkStateStore.current.connected && !request.song.isLocal &&
            !request.song.id.startsWith("local_") && audioCacheManager.getCachedPlaybackResource(request.song, touch = false) == null
        ) {
            if (stagedShouldCommit == null) skipUnavailableOfflineSong(sessionId, request)
            return false
        }
        val wantVideo = false
        PlaybackEventLog.info(
            PlaybackEventKind.QUEUE,
            "Android queue playback requested.",
            videoId = request.song.id,
            details =
                request.song.toPlaybackLogDetails() +
                    mapOf(
                        "wantVideo" to "false",
                        "queueRequestId" to request.requestId.toString(),
                        "playbackPositionMs" to targetPosition.toString(),
                    ),
        )
        val sourceSong = SongMetadataState.apply(request.song)
        val sync = AndroidDeviceConnect.manager.remote
        var playbackSong = sourceSong
        var playbackPosition = targetPosition

        fun canCommitSelection(): Boolean =
            isCurrentRequest(expectedWantVideo = wantVideo) &&
                canCommitAudioTrackSelection(
                    sourceSongId = sourceSong.id,
                    selectedSongId = playbackSong.id,
                    deviceConnectActive = sync.isActive,
                    listenTogetherGuestPlaybackLocked = false,
                )
        var errorReason: String? = null
        var playbackFailure: Throwable? = null
        var urlRecoveryBudget: PlaybackRecoveryBudget? = null
        val started =
            withQueueLoading(isPlaybackLoading = true) {
                val cachedAudio = audioCacheManager.getCachedPlaybackResource(sourceSong)
                if (!isCurrentRequest(expectedWantVideo = wantVideo)) return@withQueueLoading false
                var hints = playbackSong.toAndroidContentHints(wantVideo = false)
                var resolveBudget = queuePlaybackResolveBudget(SystemClock.elapsedRealtime(), hints.playbackClientOverrideId)
                val resolveTimeoutMessage = getString(Res.string.playback_unable_to_play_queued_track)

                suspend fun resolveWithinBudget(): PlaybackFetchResult =
                    withQueuePlaybackResolveDeadline(resolveBudget.deadlineAtMs, SystemClock::elapsedRealtime) {
                        fetchPlaybackResource(playbackSong, wantVideo = false)
                    } ?: PlaybackFetchResult(
                        errorReason = resolveTimeoutMessage,
                        failure = TimeoutException("Queue stream resolution exceeded its 30000ms deadline"),
                    )
                var result =
                    cachedAudio?.let { PlaybackFetchResult(resource = it) }
                        ?: resolveWithinBudget()
                while (
                    result.resource == null &&
                    result.failure != null &&
                    PlaybackNetworkStateStore.current.connected &&
                    resolveBudget.canStart(SystemClock.elapsedRealtime())
                ) {
                    if (!canCommitSelection()) return@withQueueLoading false
                    val category = PlaybackFailureClassifier.fromThrowable(result.failure)
                    val decision =
                        PlaybackRecoveryPolicy.decide(
                            category = category,
                            budget = resolveBudget,
                            nowMs = SystemClock.elapsedRealtime(),
                        )
                    resolveBudget = decision.budget
                    if (decision.action == PlaybackRecoveryAction.NONE) break

                    delay(QUEUE_RESOLVE_RETRY_DELAY_MS.milliseconds)
                    if (!canCommitSelection()) return@withQueueLoading false
                    result =
                        if (decision.action == PlaybackRecoveryAction.RETRY_SAME_URL ||
                            decision.action == PlaybackRecoveryAction.REBUILD_RENDERER
                        ) {
                            resolveWithinBudget()
                        } else {
                            val recovery =
                                withQueuePlaybackResolveDeadline(resolveBudget.deadlineAtMs, SystemClock::elapsedRealtime) {
                                    recoverPlaybackResource(
                                        streamResolver = streamResolver,
                                        retryNetworkResource = false,
                                        now = Clock.System.now(),
                                        action = decision.action,
                                        currentResource = null,
                                        videoId = playbackSong.id,
                                        hints = hints,
                                        sourceSong = playbackSong,
                                        budget = resolveBudget,
                                        category = category,
                                    )
                                } ?: RecoveryResourceResult(
                                    resource = null,
                                    budget = resolveBudget.consumeRequest(),
                                    failure =
                                        TimeoutException(
                                            "Queue stream recovery exceeded its 30000ms deadline during ${decision.action}",
                                        ).apply { initCause(result.failure) },
                                )
                            resolveBudget = recovery.budget
                            PlaybackFetchResult(
                                resource = recovery.resource,
                                failure = recovery.failure,
                                errorReason =
                                    recovery.failure?.let {
                                        getString(
                                            it.playbackErrorResource(Settings.shared.innerTubeCookie.isNotBlank()),
                                        )
                                    }
                                        ?: resolveTimeoutMessage,
                            )
                        }
                }
                errorReason = result.errorReason
                playbackFailure = result.failure
                var resource = result.resource ?: return@withQueueLoading false
                if (playbackSong.isPodcast && playbackSong.duration == null) {
                    playbackPosition =
                        withContext(Dispatchers.IO) {
                            PodcastLibrary.resumePosition(
                                database,
                                playbackSong,
                                requestedPosition.takeIf { it > 0L || sync.isActive },
                                durationMs = resource.durationSeconds?.times(1000L) ?: 0L,
                            )
                        }
                }
                var budget = PlaybackRecoveryBudget.starting(SystemClock.elapsedRealtime())
                while (budget.canStart(SystemClock.elapsedRealtime())) {
                    if (!canCommitSelection()) return@withQueueLoading false
                    val currentQueueSong =
                        selectionQueueSong(stagedQueueReplacement = stagedShouldCommit != null, sourceSong = sourceSong) {
                            player.songQueue.value
                                .getOrNull(player.currentMediaItemIndex.value)
                                ?.let(SongMetadataState::apply)
                        } ?: return@withQueueLoading false
                    val mergedSong = mergeSelectedQueueSong(playbackSong, sourceSong, currentQueueSong)
                    playbackSong = mergedSong.matchingLiveState(resource)
                    var prepareFailure: Throwable? = null
                    if (
                        player.prepareAndPlay(
                            song = playbackSong,
                            resource = resource,
                            positionMs = playbackPosition,
                            resumePlayback = request.resumePlayback,
                            shouldCommit = ::canCommitSelection,
                            playWhenReadyProvider = { if (stagedShouldCommit == null) player.wantsPlayback else false },
                            positionMsProvider = if (stagedShouldCommit == null) null else ({ playbackPosition }),
                            stageQueueReplacement = stagedShouldCommit != null,
                            onPrepareFailure = { prepareFailure = it },
                            replaceCurrentQueueItem = playbackSong.id != request.song.id,
                        )
                    ) {
                        if (budget.urlRefreshesRemaining < 2) {
                            urlRecoveryBudget =
                                PlaybackRecoveryBudget.starting(SystemClock.elapsedRealtime()).copy(
                                    urlRefreshesRemaining = budget.urlRefreshesRemaining,
                                )
                        }
                        return@withQueueLoading true
                    }
                    if (!canCommitSelection()) return@withQueueLoading false

                    val currentFailure = prepareFailure ?: IllegalStateException("Prepared stream could not start")
                    playbackFailure = currentFailure
                    val httpStatusCode =
                        currentFailure.findMedia3HttpStatusCode() ?: currentFailure.findPlaybackHttpStatusCode()
                    val category =
                        httpStatusCode?.let(PlaybackFailureClassifier::fromHttpStatus)
                            ?: currentFailure.media3PlaybackFailureCategory()
                    if (PlaybackRecoveryPolicy.requiresFreshUrl(category)) {
                        budget = budget.forPlaybackFailure(playbackPosition, SystemClock.elapsedRealtime(), category)
                    }
                    val decision =
                        PlaybackRecoveryPolicy.decide(
                            category = category,
                            budget = budget,
                            nowMs = SystemClock.elapsedRealtime(),
                        )
                    budget = decision.budget
                    val action = decision.action
                    if (action == PlaybackRecoveryAction.NONE) break

                    if (
                        action == PlaybackRecoveryAction.RETRY_CLIENT ||
                        (
                            currentFailure.isMediaTransportFailure() &&
                                (
                                    category == PlaybackFailureCategory.CDN_FORBIDDEN ||
                                        category == PlaybackFailureCategory.SABR_PROTOCOL ||
                                        category == PlaybackFailureCategory.SABR_ATTESTATION
                                )
                        )
                    ) {
                        streamResolver.reportPlaybackFailure(
                            videoId = playbackSong.id,
                            clientName = resource.clientName,
                            responseCode = httpStatusCode,
                            profileId = resource.profileId,
                            hints = hints,
                            category = category,
                        )
                    }
                    val recovery =
                        recoverPlaybackResource(
                            streamResolver = streamResolver,
                            retryNetworkResource = false,
                            now = Clock.System.now(),
                            action = action,
                            currentResource = resource,
                            failure = currentFailure,
                            videoId = playbackSong.id,
                            hints = hints,
                            sourceSong = playbackSong,
                            budget = budget,
                            category = category,
                        )
                    budget = recovery.budget
                    resource = recovery.resource ?: run {
                        playbackFailure = recovery.failure ?: currentFailure
                        errorReason =
                            recovery.failure?.let { getString(it.playbackErrorResource(Settings.shared.innerTubeCookie.isNotBlank())) }
                        return@withQueueLoading false
                    }
                }
                false
            }

        if (!isCurrentRequest(playbackSong.id, expectedWantVideo = wantVideo)) return false
        if (!started) {
            if (stagedShouldCommit != null) return false
            if (!PlaybackNetworkStateStore.current.connected) {
                if (!playbackSong.isLocal && !playbackSong.id.startsWith("local_") &&
                    audioCacheManager.getCachedPlaybackResource(playbackSong, touch = false) == null
                ) {
                    skipUnavailableOfflineSong(sessionId, request)
                } else {
                    player.pause()
                }
                return false
            }
            playbackFailure?.let { error ->
                PlaybackErrorReporter.report(
                    error,
                    PlaybackErrorContext(stage = "queue_start", videoId = playbackSong.id, recovered = false, song = playbackSong),
                )
            }
            PlaybackEventLog.error(
                kind = PlaybackEventKind.PLAYER,
                message = "Android queue playback could not start.",
                videoId = playbackSong.id,
                details =
                    mapOf(
                        "requestedVideo" to wantVideo.toString(),
                        "playbackPositionMs" to playbackPosition.toString(),
                    ),
                error = playbackFailure,
            )
            showPlaybackErrorToast(
                errorReason ?: getString(Res.string.playback_unable_to_play_queued_track),
            )
            if (shouldAdvanceAfterPlaybackFailure(
                    player.wantsPlayback,
                    PlaybackFailureClassifier.fromThrowable(playbackFailure),
                    PlaybackNetworkStateStore.current.connected,
                    repeatMode = repeatMode.value,
                    autoSkipNextOnError = Settings.shared.autoSkipNextOnError,
                )
            ) {
                skipFailedTrackOrStop(playbackFailure)
            } else {
                publishTerminalPlaybackError(playbackFailure)
            }
            return false
        }

        streamRecoveryVideoId = playbackSong.id.takeIf { urlRecoveryBudget != null }
        streamRecoveryBudget = urlRecoveryBudget
        if (stagedShouldCommit != null) return true
        syncPlayback.onLocalTrackLoaded()
        AndroidDeviceConnect.manager.remote.localStateChanged(trackChanged = true)
        if ((!queueCoordinator.isFiniteQueue || Settings.shared.autoLoadMore) && queueCoordinator.recommendationBootstrapJob == null) {
            bootstrapRecommendations(queueCoordinator.recommendationSeedSongId ?: request.song.id)
        }
        return true
    }

    private suspend fun offlineAvailability(): suspend (String) -> Boolean {
        val ids =
            if (PlaybackNetworkStateStore.current.connected) {
                emptySet()
            } else {
                audioCacheManager.cachedSongIds()
            }
        return { it in ids }
    }

    private suspend fun skipUnavailableOfflineSong(
        sessionId: Long,
        request: QueuePlaybackRequest,
    ) {
        val available = audioCacheManager.cachedSongIds(player.songQueue.value.mapTo(mutableSetOf()) { it.id })
        val next =
            nextOfflineQueueIndex(
                player.songQueue.value,
                player.currentMediaItemIndex.value,
                _repeatMode.value == Player.REPEAT_MODE_ALL,
            ) { song ->
                song.isLocal || song.id.startsWith("local_") || song.id in available
            }
        if (!isCurrentQueueRequest(sessionId, request)) return
        if (next != null && !AndroidDeviceConnect.manager.remote.isActive) {
            player.skipToQueueItem(next, resumePlayback = player.wantsPlayback)
        } else {
            player.pause()
        }
    }

    private fun isCurrentQueueRequest(
        sessionId: Long,
        request: QueuePlaybackRequest,
        queueSongId: String = request.song.id,
        expectedWantVideo: Boolean? = null,
    ): Boolean =
        isCurrentQueuePlaybackTarget(
            expectedSessionId = sessionId,
            currentSessionId = queueCoordinator.queueSessionId,
            expectedRequestId = request.requestId,
            currentRequestId = player.latestQueuePlaybackRequestId,
            expectedQueueSongId = queueSongId,
            currentQueueSongId =
                player.songQueue.value
                    .getOrNull(player.currentMediaItemIndex.value)
                    ?.id,
        ) && (expectedWantVideo == null || !expectedWantVideo)

    private fun Throwable.isMediaTransportFailure(): Boolean {
        if (findSabrProtocolException() != null) return true
        val visited = mutableSetOf<Throwable>()
        var current: Throwable? = this
        while (current != null && visited.add(current)) {
            if (current is IOException) return true
            current = current.cause
        }
        return false
    }

    private fun createLikeButton(): CommandButton {
        val song = player.currentSong.value
        val isLiked = song?.id?.let(SongLikeState::isLiked) == true
        return CommandButton
            .Builder(if (isLiked) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
            .setCustomIconResId(if (isLiked) R.drawable.ic_notification_heart else R.drawable.ic_notification_heart_outline)
            .setDisplayName(getString(if (isLiked) R.string.remove_from_favorites else R.string.add_to_favorites))
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .setSlots(CommandButton.SLOT_BACK_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .setEnabled(song != null)
            .build()
    }

    private fun createShuffleButton(): CommandButton =
        CommandButton
            .Builder(if (_shuffleModeEnabled.value) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF)
            .setCustomIconResId(if (_shuffleModeEnabled.value) R.drawable.ic_notification_shuffle_on else R.drawable.shuffle)
            .setDisplayName(getString(R.string.shuffle))
            .setSessionCommand(MediaSessionConstants.CommandToggleShuffle)
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .setEnabled(player.currentSong.value != null)
            .build()

    private fun createRepeatButton(): CommandButton {
        val mode = _repeatMode.value
        val icon =
            when (mode) {
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                else -> CommandButton.ICON_REPEAT_OFF
            }
        val label =
            when (mode) {
                Player.REPEAT_MODE_ALL -> R.string.repeat_mode_all
                Player.REPEAT_MODE_ONE -> R.string.repeat_mode_one
                else -> R.string.repeat_mode_off
            }
        val iconRes =
            when (mode) {
                Player.REPEAT_MODE_ALL -> R.drawable.ic_notification_repeat
                Player.REPEAT_MODE_ONE -> R.drawable.ic_notification_repeat_one
                else -> R.drawable.ic_notification_repeat_off
            }
        return CommandButton
            .Builder(icon)
            .setCustomIconResId(iconRes)
            .setDisplayName(getString(label))
            .setSessionCommand(MediaSessionConstants.CommandToggleRepeat)
            .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
            .setEnabled(player.currentSong.value != null)
            .build()
    }

    private fun createRadioButton(label: String): CommandButton =
        CommandButton
            .Builder(CommandButton.ICON_RADIO)
            .setCustomIconResId(R.drawable.ic_notification_radio)
            .setDisplayName(label)
            .setSessionCommand(MediaSessionConstants.CommandToggleStartRadio)
            .setEnabled(player.currentSong.value != null)
            .build()

    @UnstableApi
    override fun onUpdateNotificationAsync(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ): ListenableFuture<Void?> = super.onUpdateNotificationAsync(session, startInForegroundRequired)

    internal suspend fun promotePlaybackToForeground(): Boolean {
        val session = mediaSession ?: return false
        return try {
            onUpdateNotificationAsync(session, true).await()
            isPlaybackOngoing
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Playback foreground promotion failed")
            false
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val foregroundStart = requiresPlaybackForegroundStart(intent)
        if (foregroundStart) {
            try {
                setupNotificationChannel()
                // Media buttons and paused notification actions can arrive before a queue is restored.
                promotePlaybackServiceStart(mediaNotificationProvider?.latestNotification)
            } catch (error: RuntimeException) {
                Timber.w(error, "Playback service foreground start failed")
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
        if (!::player.isInitialized) {
            if (foregroundStart) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (foregroundStart) {
            foregroundStartCleanupJob?.cancel()
            foregroundStartCleanupJob =
                serviceScope.launch {
                    delay(10_000L)
                    // A late self-start must not restore a dismissed or empty notification.
                    if (shouldReconcilePlaybackServiceStart(intent, media3Player)) {
                        triggerNotificationUpdate()
                    }
                }
        }
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == MusicWidgetReceiver.ACTION_UPDATE_WIDGET) updateWidgetsNow()
        if (intent?.getStringExtra(EXTRA_WIDGET_COMMAND_TOKEN) == WIDGET_COMMAND_TOKEN) {
            when (intent.action) {
                MusicWidgetReceiver.ACTION_PLAY_PAUSE -> {
                    if (media3Player?.playWhenReady == true) media3Player?.pause() else media3Player?.play()
                }

                MusicWidgetReceiver.ACTION_LIKE -> {
                    toggleFavorite()
                }

                MusicWidgetReceiver.ACTION_NEXT -> {
                    media3Player?.seekToNext()
                }

                MusicWidgetReceiver.ACTION_PREVIOUS -> {
                    media3Player?.seekToPrevious()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun toggleFavorite() {
        val song = player.currentSong.value ?: return
        SongLikeState.toggleLike(song)
        updateWidgetsNow()
    }

    private fun startWidgetUpdates() {
        serviceScope.launch {
            combine(
                combine(
                    combine(
                        widgetSongState(player.currentSong, SongLikeState.likedSongsFlow),
                        widgetPlaybackRequested(player.wantsPlaybackState, castManager.state),
                    ) { (song, liked), requested -> Triple(song, liked, requested) },
                    player.queueSnapshots,
                    _repeatMode,
                    combine(
                        player.currentPlaybackResource,
                        player.playbackInstanceId,
                        queueCoordinator.playlistPageState,
                        castManager.state.map { Triple(it.isConnected, it.isPlaying, it.durationMs) }.distinctUntilChanged(),
                    ) { resource, _, _, _ -> resource },
                ) { songState, _, _, _ -> songState },
                player.isPlaying,
                player.duration,
                SongMetadataState.revision,
                snapshotFlow {
                    Settings.shared.artistNameAliasesJson
                },
            ) { songState, _, _, _, _ -> songState }
                .collectLatest { (song, liked, requested) -> updateWidgetsSnapshot(song, liked, requested) }
        }
    }

    private fun updateWidgetsNow() {
        serviceScope.launch { updateWidgetsSnapshot() }
    }

    private suspend fun updateWidgetsSnapshot(
        currentSong: SongItem? = player.currentSong.value,
        isLiked: Boolean = currentSong?.let { SongLikeState.isLiked(it.id) } == true,
        playbackRequested: Boolean = media3Player?.playWhenReady == true,
    ) {
        val state =
            playbackWidgetState(
                song = currentSong?.let(SongMetadataState::apply),
                isPlaying = media3Player?.isPlaying == true,
                isLiked = isLiked,
                duration = widgetDuration(),
                currentPosition = media3Player?.currentPosition ?: player.getRealtimePositionMs(),
                idleTitle = getString(R.string.not_playing),
                idleArtist = getString(R.string.tap_to_play),
                unknownArtist = getString(R.string.unknown_artist),
            )
        widgetManager.updateWidgets(
            title = state.title,
            artist = state.artist,
            artworkUri = state.artworkUri,
            isPlaying = state.isPlaying,
            isLiked = state.isLiked,
            duration = state.duration,
            currentPosition = state.currentPosition,
            songId = currentSong?.id,
            playbackInstanceId = player.currentPlaybackInstanceId,
            controls =
                WidgetControls(
                    canPlayPause = true,
                    canPrevious =
                        currentSong != null &&
                            canSkipToPrevious(
                                true,
                                false,
                                player.queueSnapshot(),
                                _repeatMode.value,
                                false,
                                currentSong.isLive || player.currentPlaybackResource.value?.isLive == true,
                            ),
                    canNext =
                        canSkipToNext(
                            true,
                            false,
                            player.queueSnapshot(),
                            _repeatMode.value,
                            false,
                            hasPlaylistContinuation = queueCoordinator.playlistPage?.hasMore == true,
                        ),
                    canLike = currentSong != null,
                    playbackRequested = playbackRequested,
                ),
        )
    }

    private fun widgetDuration(): Long =
        if (player.currentSong.value?.isLive == true || player.currentPlaybackResource.value?.isLive == true) {
            0
        } else {
            media3Player?.duration ?: player.duration.value
        }

    private fun startWidgetProgressUpdates() {
        val interactive =
            widgetScreenInteractive(this)
                .onEach { if (it) updateWidgetProgressNow() }
                .stateIn(serviceScope, SharingStarted.Eagerly, false)
        val playback =
            combine(
                player.isPlaying,
                combine(player.playbackStateFlow, player.currentPlaybackResource) { state, _ -> state },
                player.currentSong,
                player.duration,
                castManager.state.map { Triple(it.isConnected, it.isPlaying, it.durationMs) }.distinctUntilChanged(),
            ) { _, _, _, _, _ ->
                val session = media3Player
                Triple(session?.isPlaying == true, session?.playbackState == Player.STATE_READY, player.currentSong.value != null)
            }
        val active =
            combine(playback, interactive, widgetManager.hasProgressWidgets) { (playing, ready, hasSong), screenOn, hasWidget ->
                shouldSampleWidgetProgress(playing, ready, hasSong, widgetDuration(), screenOn, hasWidget)
            }
        serviceScope.launch {
            sampleWidgetProgress(active) { updateWidgetProgress() }
        }
        serviceScope.launch {
            castManager.state
                .map { it.seekGeneration }
                .distinctUntilChanged()
                .collect { updateWidgetProgress() }
        }
        player.addPlayerListener(
            object : Player.Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    updateWidgetProgressNow()
                }
            },
        )
    }

    private fun updateWidgetProgressNow() {
        serviceScope.launch { updateWidgetProgress() }
    }

    private suspend fun updateWidgetProgress() {
        val power = requireNotNull(getSystemService(PowerManager::class.java))
        val keyguard = requireNotNull(getSystemService(KeyguardManager::class.java))
        if (!power.isInteractive || keyguard.isKeyguardLocked || !widgetManager.hasProgressWidgets.value) return
        val song = player.currentSong.value ?: return
        val duration = widgetDuration()
        if (duration <= 0) return
        widgetManager.updateProgress(
            song.id,
            player.currentPlaybackInstanceId,
            duration,
            media3Player?.currentPosition ?: player.getRealtimePositionMs(),
        )
    }

    private fun updatePodcastProgress() {
        if (player.currentPlaybackInstanceId == 0L) return
        podcastProgress.update(
            player.currentSong.value,
            player.getRealtimePositionMs(),
            player.duration.value,
            player.isPlaying.value,
        )
    }

    private fun flushPodcastProgress(): Job? {
        updatePodcastProgress()
        return podcastProgress.flush()
    }

    private fun startPlaybackPersistence() {
        player.addPlayerListener(
            object : Player.Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (reason != Player.DISCONTINUITY_REASON_SEEK && reason != Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) return
                    updatePodcastProgress()
                    podcastProgress.flush(force = true)
                    savePlaybackState()
                }
            },
        )
        serviceScope.launch {
            ProcessLifecycleOwner
                .get()
                .lifecycle.currentStateFlow
                .map { it.isAtLeast(Lifecycle.State.STARTED) }
                .distinctUntilChanged()
                .collect { foreground ->
                    if (foreground) {
                        // Requests fail while Android restricts background network; that is not a YouTube outage.
                        streamResolver.resetExtractionBackoff()
                    } else {
                        flushPodcastProgress()
                        saveFinalPlaybackState()
                    }
                }
        }
        serviceScope.launch(Dispatchers.IO) {
            writePendingPlaybackCheckpoints(playbackPersistenceRequests, playbackPersistenceWriter::write) { error ->
                Timber.e(error, "Failed to persist playback")
            }
        }
        serviceScope.launch {
            val playing =
                combine(player.isPlaying, castManager.state, player.queueState) { localPlaying, remote, queueState ->
                    remote.forQueueEntry(queueState)?.isPlaying ?: localPlaying
                }
            observePlaybackCheckpoints(playing, snapshotFlow { Settings.shared.persistentQueue }) {
                savePlaybackState()
            }
        }
        serviceScope.launch {
            combine(castManager.state, player.queueState) { remote, queue ->
                remote.forQueueEntry(queue)?.let { Triple(it.isPlaying, it.positionMs.takeUnless { _ -> it.isPlaying }, it.seekGeneration) }
            }.distinctUntilChanged().collect { if (it != null) savePlaybackState() }
        }
        serviceScope.launch {
            combine(player.isPlaying, player.currentPosition) { playing, position -> position.takeUnless { playing } }
                .distinctUntilChanged()
                .collect { position -> if (position != null) savePlaybackState() }
        }
        serviceScope.launch {
            combine(player.queueSnapshots, repeatMode) { queue, repeat -> queue to repeat }
                .distinctUntilChanged()
                .collect { savePlaybackState() }
        }
        serviceScope.launch {
            snapshotFlow { Settings.shared.persistentQueue }
                .distinctUntilChanged()
                .collect { saveFinalPlaybackState() }
        }
    }

    private fun buildPersistPlayerState(): PersistPlayerState? =
        if (playbackPersistenceLifecycle.restoring) null else queueCoordinator.buildPersistPlayerState()

    private fun savePlaybackState() {
        if (!playbackPersistenceLifecycle.restoring) queueCoordinator.saveShuffleMode()
        val decision =
            decidePlaybackCheckpoint(
                writer = playbackPersistenceWriter,
                lifecycle = playbackPersistenceLifecycle,
                persistentQueue = Settings.shared.persistentQueue,
                previous = lastPersistenceState,
                state = ::buildPersistPlayerState,
            )
        val request = decision.request ?: return
        pendingPlaybackClear = request.takeIf { it.clear }
        lastPersistenceState = decision.state
        playbackPersistenceRequests.trySend(request)
    }

    private fun saveFinalPlaybackState(): Job? {
        if (!playbackPersistenceLifecycle.restoring) queueCoordinator.saveShuffleMode()
        if (!playbackPersistenceLifecycle.canSave(Settings.shared.persistentQueue)) return finalPlaybackPersistence
        if (!Settings.shared.persistentQueue) return clearFinalPersistedPlaybackState()
        val state = buildPersistPlayerState()
        if (state == null) {
            val clear = pendingPlaybackClear
            if (clear != null) {
                pendingPlaybackClear = null
                return launchFinalPersistence(clear)
            }
            // A transient empty queue can still have a queued non-empty checkpoint; flush it before the scope is cancelled.
            val pendingState = lastPersistenceState ?: return finalPlaybackPersistence
            return launchFinalPersistence(playbackPersistenceWriter.request(pendingState))
        }
        pendingPlaybackClear = null
        lastPersistenceState = state
        return launchFinalPersistence(playbackPersistenceWriter.request(state))
    }

    internal fun clearPersistedPlaybackState() {
        lastPersistenceState = null
        val clear = playbackPersistenceWriter.requestClear()
        pendingPlaybackClear = clear
        playbackPersistenceRequests.trySend(clear)
    }

    private fun clearFinalPersistedPlaybackState(): Job? {
        lastPersistenceState = null
        pendingPlaybackClear = null
        return launchFinalPersistence(playbackPersistenceWriter.requestClear())
    }

    private fun launchFinalPersistence(request: PlaybackPersistenceWriter.Request): Job? {
        finalPlaybackPersistence =
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { playbackPersistenceWriter.write(request) }
                    .onFailure { Timber.e(it, "Failed to persist playback") }
            }
        return finalPlaybackPersistence
    }

    internal suspend fun awaitPlaybackRestore() = playbackRestoreJob.join()

    private suspend fun restorePersistedPlayback() {
        queueCoordinator.restoreShuffleMode()
        if (!Settings.shared.persistentQueue) {
            Settings.shared.pendingContinuePlayback = false
            return
        }
        val expectedQueueSessionId = queueCoordinator.queueSessionId
        try {
            finalPlaybackPersistence?.join()
            val raw = withContext(Dispatchers.IO) { Settings.shared.persistentPlaybackState }
            if (raw.isBlank()) {
                Settings.shared.pendingContinuePlayback = false
                return
            }
            val restoreModes = Settings.shared.rememberShuffleAndRepeat
            val blocked = BlockedContent.snapshot()
            val position = withContext(Dispatchers.IO) { Settings.shared.persistentPlaybackPosition }
            val (state, restoredQueue) =
                withContext(Dispatchers.Default) {
                    decodePersistPlayerState(raw, position).restoredPlaybackState(restoreModes, blocked::isBlocked)
                }
            val restoredSongs = restoredQueue.displayedItems
            if (restoredSongs.isEmpty()) {
                Settings.shared.pendingContinuePlayback = false
                clearPersistedPlaybackState()
                return
            }
            val restored = queueCoordinator.restorePersistedQueue(state, restoredQueue, expectedQueueSessionId, restoreModes)
            if (restored) {
                Settings.shared.pendingContinuePlayback = Settings.shared.promptContinuePlayback
                updateWidgetsSnapshot()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore playback")
            Settings.shared.pendingContinuePlayback = false
            clearPersistedPlaybackState()
        }
    }

    private suspend fun handlePlaybackCompletion(completedPlaybackInstanceId: Long? = null) {
        if (completedPlaybackInstanceId != null && completedPlaybackInstanceId != player.currentPlaybackInstanceId) {
            transitions.consumeCrossfadeCompletion(completedPlaybackInstanceId)
            return
        }
        flushPodcastProgress()
        savePlaybackState()
        if (sleepTimerController.isStopAfterCurrentSong) {
            sleepTimerController.onEndOfMediaItem()
        } else {
            advanceQueueAfterCurrent(completedPlaybackInstanceId)
        }
    }

    private fun resetQueueSession(
        seedSongId: String? = null,
        queueTitle: String? = null,
        finite: Boolean = false,
        sessionId: Long? = null,
    ) {
        queueCoordinator.resetQueueSession(seedSongId, queueTitle, finite, sessionId)
    }

    private fun synchronizeMediaSessionQueue(replacesQueue: Boolean) = queueCoordinator.synchronizeMediaSessionQueue(replacesQueue)

    private fun replaceQueueSession(
        songs: List<SongItem>,
        startIndex: Int,
        queueTitle: String?,
        finite: Boolean,
        replacement: () -> Unit,
    ) = queueCoordinator.replaceQueueSession(songs, startIndex, queueTitle, finite, replacement = replacement)

    fun addToQueue(
        songs: List<SongItem>,
        playNext: Boolean,
    ) {
        enqueueOrStartQueue(
            songs = songs,
            currentSong = player.currentSong.value,
            preventDuplicates = Settings.shared.preventDuplicateTracksInQueue,
            startQueue = { additions, startPlaying ->
                val prepared = additions.map(SongItem::asManuallyAddedQueueItem)
                replaceQueueSession(prepared, 0, null, finite = true) {
                    player.setQueue(prepared, startPlaying = startPlaying, preparePlayback = true)
                    _shuffleModeEnabled.value = player.isShuffleEnabled
                }
            },
            enqueue = { additions ->
                player.addToQueue(additions, playNext)
            },
        )
    }

    fun playQueue(
        songs: List<SongItem>,
        startIndex: Int = 0,
        queueTitle: String? = null,
        finite: Boolean? = null,
        fromRemote: Boolean = false,
        shuffleModeEnabled: Boolean? = null,
        playlistPage: PlaylistPage? = null,
        songsOnly: Boolean = false,
        startPositionMs: Long = 0L,
        startPlaying: Boolean = true,
        startVideo: Boolean = false,
    ) {
        playbackBenchmark.cancel()
        queueCoordinator.playQueue(
            songs,
            startIndex,
            queueTitle,
            finite,
            fromRemote,
            shuffleModeEnabled,
            playlistPage,
            songsOnly,
            startPositionMs,
            startPlaying,
            startVideo,
        )
    }

    suspend fun playQueueFromRemote(
        songs: List<SongItem>,
        startIndex: Int,
        title: String?,
    ): Boolean {
        if (castManager.state.value.isConnected) {
            playQueue(songs, startIndex, queueTitle = title, fromRemote = true, startPlaying = false)
            return true
        }
        player.pause()
        player.cancelPendingQueuePlayback()
        val requestId = player.latestQueuePlaybackRequestId
        val loaded =
            queueCoordinator.playQueueFromRemote(songs, startIndex, title) { song, isCurrent ->
                playQueueRequest(QueuePlaybackRequest(requestId, song, resumePlayback = false)) {
                    isCurrent() && requestId == player.latestQueuePlaybackRequestId && !castManager.state.value.isConnected
                }
            }
        if (loaded) {
            syncPlayback.onLocalTrackLoaded()
            if (Settings.shared.autoLoadMore && queueCoordinator.recommendationBootstrapJob == null) {
                bootstrapRecommendations(queueCoordinator.recommendationSeedSongId ?: songs.getOrNull(startIndex)?.id ?: return true)
            }
        }
        return loaded
    }

    fun extendPlaylistBeforeSkip(resumePlayback: Boolean? = null): Boolean = queueCoordinator.extendPlaylistBeforeSkip(resumePlayback)

    fun playMix(
        seed: SongItem,
        startPlaying: Boolean,
    ) {
        playbackBenchmark.cancel()
        queueCoordinator.playMix(seed, startPlaying)
    }

    fun updatePendingRestorePosition(position: Long) = queueCoordinator.updatePendingRestorePosition(position)

    private fun bootstrapRecommendations(seedSongId: String) = queueCoordinator.bootstrapRecommendations(seedSongId)

    fun selectQueueChip(chipId: String) = queueCoordinator.selectQueueChip(chipId)

    private suspend fun advanceQueueAfterCurrent(completedPlaybackInstanceId: Long? = null) =
        queueCoordinator.advanceQueueAfterCurrent(completedPlaybackInstanceId)

    private suspend fun extendRadioQueue(shouldCommit: () -> Boolean): Boolean = queueCoordinator.extendRadioQueue(shouldCommit)

    private suspend fun <T> withQueueLoading(
        isPlaybackLoading: Boolean = false,
        block: suspend () -> T,
    ): T = queueCoordinator.withQueueLoading(isPlaybackLoading, block)

    private suspend fun fetchPlaybackResource(
        song: SongItem,
        @Suppress("UNUSED_PARAMETER") wantVideo: Boolean = false,
    ): PlaybackFetchResult =
        withContext(Dispatchers.IO) {
            val connected = PlaybackNetworkStateStore.current.connected
            cachedAudioResource { audioCacheManager.getCachedPlaybackResource(song) }
                ?.let { return@withContext PlaybackFetchResult(resource = it) }
            if (!connected && !song.isLocal && !song.id.startsWith("local_")) return@withContext PlaybackFetchResult()
            val hints = song.toAndroidContentHints(wantVideo = false)
            try {
                val resource =
                    streamResolver.resolve(
                        song.id,
                        hints,
                        playbackAudioQuality(),
                        sourceSong = song,
                    )
                PlaybackFetchResult(resource = resource)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PlaybackFetchResult(
                    failure = e,
                    errorReason = getString(e.playbackErrorResource(Settings.shared.innerTubeCookie.isNotBlank())),
                )
            }
        }

    private fun showPlaybackErrorToast(reason: String) {
        PlaybackSnackbarEvents.emit(getString(R.string.playback_error_message, reason))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::player.isInitialized) {
            widgetManager.refreshProgressDemand()
            updateWidgetsNow()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // The lifetime observer retains loading and remote playback; Media3 stops non-playing sessions immediately.
        if (!::player.isInitialized) return
        playbackBenchmark.cancel()
        saveFinalPlaybackState()
        flushPodcastProgress()
        taskRemoved.value = true
    }

    override fun onDestroy() {
        isDestroying = true
        isRunning = false
        if (::audioManager.isInitialized) audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        if (!::player.isInitialized) {
            podcastWriterScope.cancel()
            serviceScope.cancel()
            super.onDestroy()
            return
        }
        playbackBenchmark.cancel()
        // A failure while saving state must not leak the session, or the next service fails with a duplicate session ID.
        try {
            releasePlaybackState()
        } finally {
            mediaSession?.release()
            MediaArtworkProvider.currentArtwork = null
            player.release()
            super.onDestroy()
        }
    }

    private fun releasePlaybackState() {
        val checkpoint = saveFinalPlaybackState()
        updatePodcastProgress()
        val podcastWrite = podcastProgress.flush()
        if (podcastWrite == null) podcastWriterScope.cancel() else podcastWrite.invokeOnCompletion { podcastWriterScope.cancel() }
        val historyWrite = listeningHistory.flush()
        runBlocking {
            awaitFinalPlaybackCheckpoint(checkpoint)
            awaitFinalPlaybackCheckpoint(historyWrite)
            awaitFinalPlaybackCheckpoint(podcastWrite)
        }
        isRunning = false
        running.value = false
        syncPlayback.release()
        AndroidDeviceConnect.manager.remote.detach(syncPlayback.port)
        castManager.closeImmediately()
        serviceScope.cancel()
        contentResolver.unregisterContentObserver(volumeMuteObserver)
    }

    private data class PlaybackFetchResult(
        val resource: PlaybackResource? = null,
        val errorReason: String? = null,
        val failure: Throwable? = null,
    )

    private fun isAuthorizedExternalMediaController(controller: MediaSession.ControllerInfo): Boolean =
        controller.isPackageNameVerified ||
            packageManager.getPackagesForUid(controller.uid)?.contains(controller.packageName) == true

    private inner class PlaybackSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            Timber.tag(TAG).d(
                "Media controller connected trusted=%s version=%d",
                controller.isTrusted,
                controller.controllerVersion,
            )
            val isLocalOrNotification = controller.isTrusted || session.isMediaNotificationController(controller)
            if (!isLocalOrNotification) {
                val authorized = isAuthorizedExternalMediaController(controller)
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                if (authorized) {
                    commands.add(SessionCommand.COMMAND_CODE_SESSION_SET_RATING)
                    commands.add(MediaSessionConstants.CommandToggleLike)
                    commands.add(MediaSessionConstants.CommandToggleShuffle)
                    commands.add(MediaSessionConstants.CommandToggleRepeat)
                    commands.add(MediaSessionConstants.CommandToggleStartRadio)
                    MediaArtworkProvider.allowedPackages += controller.packageName
                }
                return MediaSession.ConnectionResult.accept(
                    commands.build(),
                    externalTransportPlayerCommands(),
                )
            }
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(MediaSessionConstants.CommandToggleLike)
                    .add(MediaSessionConstants.CommandToggleRepeat)
                    .add(MediaSessionConstants.CommandToggleShuffle)
                    .add(MediaSessionConstants.CommandToggleStartRadio)
                    .build()
            MediaArtworkProvider.allowedPackages += controller.packageName
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            if (session.connectedControllers.none { it != controller && it.packageName == controller.packageName }) {
                MediaArtworkProvider.allowedPackages -= controller.packageName
            }
            Timber.tag(TAG).d("Media controller disconnected")
            super.onDisconnected(session, controller)
        }

        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            if (playerCommand in
                setOf(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_STOP, Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_CHANGE_MEDIA_ITEMS)
            ) {
                playbackBenchmark.cancel()
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }

        override fun onSetRating(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            rating: Rating,
        ): ListenableFuture<SessionResult> = setSongRating(player.currentSong.value, rating)

        override fun onSetRating(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaId: String,
            rating: Rating,
        ): ListenableFuture<SessionResult> {
            val song = player.songQueue.value.firstOrNull { it.id == mediaId } ?: player.currentSong.value?.takeIf { it.id == mediaId }
            return setSongRating(song, rating)
        }

        private fun setSongRating(
            song: SongItem?,
            rating: Rating,
        ): ListenableFuture<SessionResult> {
            val result = setMediaSessionRating(song, rating)
            if (result == SessionResult.RESULT_SUCCESS) updateWidgetsNow()
            return Futures.immediateFuture(SessionResult(result))
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleFavorite()
                MediaSessionConstants.ACTION_TOGGLE_REPEAT -> media3Player?.repeatMode = nextMediaSessionRepeatMode(_repeatMode.value)
                MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> media3Player?.shuffleModeEnabled = !player.isShuffleEnabled
                MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> player.currentSong.value?.let { playMix(it, startPlaying = true) }
                else -> return super.onCustomCommand(session, controller, customCommand, args)
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}

internal fun shouldAdvanceAfterPlaybackFailure(
    wantsPlayback: Boolean,
    category: PlaybackFailureCategory,
    networkConnected: Boolean,
    originalCategory: PlaybackFailureCategory = category,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    autoSkipNextOnError: Boolean,
): Boolean =
    shouldSkipAfterPlaybackFailure(
        autoSkipNextOnError = autoSkipNextOnError,
        wantsPlayback = wantsPlayback,
        networkConnected = networkConnected,
        repeatOne = repeatMode == Player.REPEAT_MODE_ONE,
        category = category,
        originalCategory = originalCategory,
    )

internal fun nextMediaSessionRepeatMode(mode: Int): Int =
    when (mode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }

internal suspend fun nextOfflineQueueIndex(
    songs: List<SongItem>,
    currentIndex: Int,
    repeatAll: Boolean,
    isPlayable: suspend (SongItem) -> Boolean,
): Int? {
    if (currentIndex !in songs.indices) return null
    val indices = (currentIndex + 1 until songs.size) + if (repeatAll) (0 until currentIndex).toList() else emptyList()
    return indices.firstOrNull { isPlayable(songs[it]) }
}
