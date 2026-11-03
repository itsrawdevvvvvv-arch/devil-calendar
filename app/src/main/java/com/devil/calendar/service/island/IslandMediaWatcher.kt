package com.devil.calendar.service.island

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the active media session and exposes it as an island source with
 * transport controls.
 *
 * `getActiveSessions` throws `SecurityException` unless the notification
 * listener is granted access, so every call is guarded.
 */
@Singleton
class IslandMediaWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: IslandSourceRegistry,
    controller: IslandController,
) {
    private var session: MediaController? = null
    private var callback: MediaController.Callback? = null

    init {
        controller.addActionHandler { sourceId, actionId ->
            if (sourceId.startsWith(PREFIX)) perform(actionId)
        }
    }

    fun refresh(listenerComponent: ComponentName) {
        runCatching {
            val manager = context.getSystemService<MediaSessionManager>() ?: return
            val sessions = manager.getActiveSessions(listenerComponent)
            val playing = sessions.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            }
            bind(playing ?: sessions.firstOrNull())
        }.onFailure {
            unbind()
            registry.setMedia(emptyList())
        }
    }

    private fun bind(next: MediaController?) {
        if (next == null) {
            unbind()
            registry.setMedia(emptyList())
            return
        }
        if (session?.sessionToken == next.sessionToken) {
            publish()
            return
        }
        unbind()
        session = next
        val observer = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            override fun onSessionDestroyed() {
                unbind()
                registry.setMedia(emptyList())
            }
        }
        callback = observer
        runCatching { next.registerCallback(observer) }
        publish()
    }

    private fun unbind() {
        runCatching { callback?.let { session?.unregisterCallback(it) } }
        callback = null
        session = null
    }

    private fun publish() {
        val current = session ?: return
        val metadata = current.metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val state = current.playbackState
        val playing = state?.state == PlaybackState.STATE_PLAYING
        val position = state?.position ?: 0L

        val actions = listOf(
            IslandAction(if (playing) "pause" else "play", if (playing) "Pause" else "Play"),
            IslandAction("previous", "Previous"),
            IslandAction("next", "Next"),
        )

        registry.setMedia(
            listOf(
                IslandSource(
                    id = "$PREFIX${current.sessionToken}",
                    kind = IslandKind.MUSIC,
                    title = title,
                    subtitle = artist,
                    value = if (playing) "Playing" else "Paused",
                    progress = if (duration > 0) position.toFloat() / duration else null,
                    actions = actions,
                ),
            ),
        )
    }

    fun perform(actionId: String) {
        val transport = session?.transportControls ?: return
        runCatching {
            when (actionId) {
                "play" -> transport.play()
                "pause" -> transport.pause()
                "next" -> transport.skipToNext()
                "previous" -> transport.skipToPrevious()
            }
        }
    }

    private companion object {
        const val PREFIX = "m:"
    }
}
