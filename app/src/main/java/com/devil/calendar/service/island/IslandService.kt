package com.devil.calendar.service.island

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.devil.calendar.R
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.service.NotificationChannels
import com.devil.calendar.service.RestTimerController
import com.devil.calendar.service.ServiceLifecycleOwner
import com.devil.calendar.ui.island.IslandPillHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the island overlay.
 *
 * **Security and privacy:** the island renders only what this app already
 * knows. Nothing is written to disk, nothing is sent anywhere, and notification
 * content (added in a later phase) is never logged. The source list lives in
 * memory and is cleared when the service stops.
 *
 * **Honest limits:** "always present" is best-effort. Doze, OEM battery
 * managers (Xiaomi/Oppo/Vivo/Samsung) and Android 12+ background-start rules
 * can still end this service; `START_STICKY` plus a boot receiver is the
 * ceiling, not a guarantee.
 */
@AndroidEntryPoint
class IslandService : Service() {

    @Inject lateinit var controller: IslandController
    @Inject lateinit var restTimer: RestTimerController
    @Inject lateinit var registry: IslandSourceRegistry
    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var tiltSensor: TiltSensor
    @Inject lateinit var mediaWatcher: IslandMediaWatcher

    private val owner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: IslandOverlay? = null
    private val appearance = kotlinx.coroutines.flow.MutableStateFlow(IslandAppearance())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        owner.onCreate()
        owner.onStart()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        if (overlay == null) {
            val created = IslandOverlay(this, owner)
            if (!created.show({
                    IslandPillHost(controller, appearance, tiltSensor.tilt)
                }, appearance.value.topOffsetDp)) {
                // Overlay permission was revoked; do not sit in the foreground
                // pretending to work.
                stopSelf()
                return START_NOT_STICKY
            }
            overlay = created
        }

        controller.setRunning(true)
        tiltSensor.start()
        controller.addActionHandler { sourceId, actionId ->
            if (sourceId == REST_ID) {
                when (actionId) {
                    "minus" -> restTimer.adjust(-15)
                    "plus" -> restTimer.adjust(15)
                    "next" -> restTimer.startNext()
                }
            }
        }

        scope.launch {
            restTimer.state.collect { rest -> publishRest(rest) }
        }
        scope.launch {
            // Own sources + other apps' notifications + media, merged.
            registry.merged.collect { controller.setSources(it) }
        }
        scope.launch {
            // Media sessions change without posting a notification (track
            // change, pause), so poll rather than waiting for an event.
            while (true) {
                mediaWatcher.refresh(android.content.ComponentName(this@IslandService, IslandNotificationListener::class.java))
                kotlinx.coroutines.delay(4_000)
            }
        }
        scope.launch {
            kotlinx.coroutines.flow.combine(
                prefs.islandHaptics,
                prefs.islandScale,
                prefs.islandCorner,
                prefs.islandTopOffset,
                prefs.islandTilt,
            ) { haptics, scale, corner, offset, tilt ->
                IslandAppearance(
                    scale = when (scale) {
                        "small" -> 0.85f
                        "large" -> 1.15f
                        else -> 1f
                    },
                    cornerDp = corner,
                    topOffsetDp = offset,
                    haptics = haptics,
                    tiltGlare = tilt,
                )
            }.collect { appearance.value = it }
        }
        scope.launch {
            // The pill changes size between states; re-measure the window.
            controller.state.collect { overlay?.refresh(appearance.value.topOffsetDp) }
        }

        return START_STICKY
    }

    private fun publishRest(rest: RestTimerController.RestState) {
        val sources = buildList {
            if (rest.active && rest.overtime) {
                add(
                    IslandSource(
                        id = "rest",
                        kind = IslandKind.REST,
                        title = "Rest over",
                        subtitle = rest.exerciseName.ifBlank { "Start the next set" },
                        value = "",
                    ),
                )
            } else if (rest.active) {
                val remaining = (rest.endMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                add(
                    IslandSource(
                        id = "rest",
                        kind = IslandKind.REST,
                        title = "Rest",
                        subtitle = rest.exerciseName.ifBlank { "Recover before the next set" },
                        actions = listOf(
                            IslandAction("minus", "-15"),
                            IslandAction("plus", "+15"),
                            IslandAction("next", "Next"),
                        ),
                        value = "%d:%02d".format(remaining / 60_000L, (remaining / 1_000L) % 60),
                        progress = if (rest.totalSeconds > 0) {
                            (remaining / 1_000f) / rest.totalSeconds
                        } else {
                            null
                        },
                        endMillis = rest.endMillis,
                    ),
                )
            }
        }
        registry.setOwn(sources)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.devil.calendar.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("Dynamic Island")
            .setContentText("Showing live activity")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        tiltSensor.stop()
        registry.clearAll()
        controller.setRunning(false)
        overlay?.hide()
        overlay = null
        owner.onDestroy()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 61
        private const val REST_ID = "rest"
        private const val ACTION_STOP = "com.devil.calendar.action.ISLAND_STOP"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, IslandService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, IslandService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}
