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

    private val owner = ServiceLifecycleOwner()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: IslandOverlay? = null

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
            if (!created.show { IslandPillHost(controller) }) {
                // Overlay permission was revoked; do not sit in the foreground
                // pretending to work.
                stopSelf()
                return START_NOT_STICKY
            }
            overlay = created
        }

        controller.setRunning(true)

        scope.launch {
            restTimer.state.collect { rest -> publishRest(rest) }
        }
        scope.launch {
            // The pill changes size between states; re-measure the window.
            controller.state.collect { overlay?.refresh() }
        }

        return START_STICKY
    }

    private fun publishRest(rest: RestTimerController.RestState) {
        val sources = buildList {
            if (rest.active) {
                val remaining = (rest.endMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                add(
                    IslandSource(
                        id = "rest",
                        kind = IslandKind.REST,
                        title = "Rest",
                        subtitle = rest.exerciseName.ifBlank { "Recover before the next set" },
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
        controller.setSources(sources)
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
        controller.setRunning(false)
        overlay?.hide()
        overlay = null
        owner.onDestroy()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 61
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
