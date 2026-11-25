package com.metrolist.android.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.metrolist.android.App
import com.metrolist.android.playback.MusicService
import com.metrolist.android.playback.PlayerConnection
import com.metrolist.android.utils.launchAsync
import com.metrolist.shared.state.SongLikeState
import kotlinx.coroutines.CancellationException
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.util.UUID

internal const val EXTRA_WIDGET_COMMAND_TOKEN = "com.metrolist.android.widget.COMMAND_TOKEN"
internal val WIDGET_COMMAND_TOKEN: String = UUID.randomUUID().toString()

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val serviceAction =
            when (intent.action) {
                MusicWidgetReceiver.ACTION_PLAY_PAUSE,
                MusicWidgetReceiver.ACTION_LIKE,
                MusicWidgetReceiver.ACTION_NEXT,
                MusicWidgetReceiver.ACTION_PREVIOUS,
                MusicWidgetReceiver.ACTION_UPDATE_WIDGET,
                -> intent.action

                else -> null
            } ?: return

        launchAsync {
            try {
                val app = context.applicationContext as App
                app.startup.await()
                if (!app.onboardingManager.isComplete) return@launchAsync
                if (serviceAction == MusicWidgetReceiver.ACTION_UPDATE_WIDGET) {
                    if (MusicService.isRunning) {
                        context.startService(Intent(context, MusicService::class.java).setAction(serviceAction))
                    } else {
                        val widgets = AppWidgetManager.getInstance(context)
                        val manager = GlobalContext.get().get<MetrolistWidgetManager>()
                        manager.updateIdleMusicWidgets(widgets.getAppWidgetIds(ComponentName(context, MusicWidgetReceiver::class.java)))
                        if (MusicService.isRunning) {
                            context.startService(Intent(context, MusicService::class.java).setAction(serviceAction))
                        }
                    }
                    return@launchAsync
                }
                val connection = GlobalContext.get().get<PlayerConnection>()
                when (serviceAction) {
                    MusicWidgetReceiver.ACTION_PLAY_PAUSE -> {
                        connection.togglePlayPauseFromWidget()
                    }

                    MusicWidgetReceiver.ACTION_NEXT -> {
                        connection.skipToNext()
                    }

                    MusicWidgetReceiver.ACTION_PREVIOUS -> {
                        connection.skipToPrevious()
                    }

                    MusicWidgetReceiver.ACTION_LIKE -> {
                        connection.awaitConnected()
                        connection.currentSong.value?.let(SongLikeState::toggleLike)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Widget action could not start playback")
            }
        }
    }
}
