package com.devil.calendar.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.devil.calendar.R
import com.devil.calendar.TakeoverActivity
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.ui.takeover.TakeoverScreen
import com.devil.calendar.ui.theme.DevilTheme
import com.devil.calendar.util.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shows an alert when a timer, alarm or focus session ends.
 *
 * Sound and vibration belong to [AlertController] alone. This service only
 * posts notifications, draws the overlay and launches the takeover.
 */
@AndroidEntryPoint
class AlertService : Service() {

    @Inject lateinit var overlay: OverlayController
    @Inject lateinit var controller: AlertController
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var prefs: UserPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlert()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                snooze(intent.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty())
                return START_NOT_STICKY
            }
            else -> beginAlert(intent)
        }
        return START_STICKY
    }

    private data class Snapshot(
        val kind: AlertKind,
        val alertId: Long,
        val label: String,
        val hour: Int,
        val minute: Int,
        val durationSeconds: Long,
    )

    private fun beginAlert(intent: Intent?) {
        val snapshot = intent?.let {
            Snapshot(
                kind = AlertKind.from(it.getStringExtra(AlarmReceiver.EXTRA_KIND)),
                alertId = it.getLongExtra(AlarmReceiver.EXTRA_ALERT_ID, 0L)
                    .takeIf { id -> id != 0L } ?: System.currentTimeMillis(),
                label = it.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty(),
                hour = it.getIntExtra(AlarmReceiver.EXTRA_HOUR, -1),
                minute = it.getIntExtra(AlarmReceiver.EXTRA_MINUTE, -1),
                durationSeconds = it.getLongExtra(AlarmReceiver.EXTRA_DURATION_SECONDS, 0L),
            )
        } ?: return

        // The foreground service must be up within a few seconds of start.
        ServiceCompat.startForeground(
            this,
            FGS_NOTIFICATION_ID,
            buildForegroundNotification(snapshot.kind),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        scope.launch {
            val use24Hour = runCatching { prefs.use24Hour.first() }.getOrDefault(false)
            val goal = runCatching { prefs.profile.first().goal }.getOrNull()
                ?.takeIf { it.isNotBlank() }

            val state = buildState(snapshot, use24Hour, goal)
            controller.start(state)

            notifyAlert(state)

            overlay.show { insets ->
                DevilTheme(darkTheme = true) {
                    val live by controller.state.collectAsState()
                    TakeoverScreen(
                        state = live,
                        onDismiss = ::stopAlert,
                        onSnooze = { snooze(snapshot.label) },
                        contentInsets = insets,
                    )
                }
            }

            runCatching { startActivity(TakeoverActivity.intent(this@AlertService, state.kind)) }

            // The heads-up has done its job once the takeover is on screen.
            delay(1200)
            cancelHeadsUp()
        }
    }

    private fun buildState(
        snapshot: Snapshot,
        use24Hour: Boolean,
        goal: String?,
    ): AlertController.AlertState {
        val headline: String
        val subtitle: String
        when (snapshot.kind) {
            AlertKind.ALARM -> {
                headline = if (snapshot.hour >= 0) {
                    TimeFormat.format(snapshot.hour, snapshot.minute, use24Hour)
                } else {
                    snapshot.label.ifBlank { "Alarm" }
                }
                subtitle = snapshot.label
            }
            AlertKind.TIMER -> {
                headline = "Time's up"
                subtitle = "Timer ${TimeFormat.duration(snapshot.durationSeconds)} finished"
            }
            AlertKind.FOCUS -> {
                headline = "Session complete"
                val minutes = (snapshot.durationSeconds / 60).coerceAtLeast(1)
                subtitle = "$minutes min focused"
            }
        }
        return AlertController.AlertState(
            alertId = snapshot.alertId,
            kind = snapshot.kind,
            headline = headline,
            subtitle = subtitle,
            goal = goal,
            showSnooze = snapshot.kind == AlertKind.ALARM,
            active = true,
        )
    }

    /** Low importance and silent: the only notification left while an alert is up. */
    private fun buildForegroundNotification(kind: AlertKind): Notification =
        NotificationCompat.Builder(this, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(kind.titleText)
            .setContentText("Alert active")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .build()

    /** The heads-up. Silent - AlertController plays the sound. */
    private fun notifyAlert(state: AlertController.AlertState) {
        val fullScreen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TakeoverActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(TakeoverActivity.EXTRA_KIND, state.kind.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, AlertService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, NotificationChannels.ALARM_ALERT_V2)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(state.kind.titleText)
            .setContentText(state.subtitle.ifBlank { state.headline })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .addAction(0, "Stop", stop)
            .setContentIntent(fullScreen)
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify(ALERT_NOTIFICATION_ID, notification)
        }
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.devil.calendar.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun cancelHeadsUp() {
        runCatching { NotificationManagerCompat.from(this).cancel(ALERT_NOTIFICATION_ID) }
    }

    /** Snooze reschedules a one-off alarm five minutes out, then stops. */
    private fun snooze(label: String) {
        scheduler.schedule(
            AlertKind.ALARM,
            System.currentTimeMillis() + SNOOZE_MILLIS,
            label,
        )
        stopAlert()
    }

    /** The single stop path: hold, notification Stop and snooze all end here. */
    private fun stopAlert() {
        controller.stop()
        overlay.hide()
        cancelHeadsUp()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        controller.stop()
        overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val FGS_NOTIFICATION_ID = 41
        const val ALERT_NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.devil.calendar.action.STOP_ALERT"
        const val ACTION_SNOOZE = "com.devil.calendar.action.SNOOZE"
        private const val SNOOZE_MILLIS = 5 * 60 * 1000L

        /** Copies the firing receiver's extras straight through. */
        fun fireIntent(context: Context, source: Intent): Intent =
            Intent(context, AlertService::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_KIND, source.getStringExtra(AlarmReceiver.EXTRA_KIND))
                putExtra(AlarmReceiver.EXTRA_LABEL, source.getStringExtra(AlarmReceiver.EXTRA_LABEL))
                putExtra(AlarmReceiver.EXTRA_ALERT_ID, source.getLongExtra(AlarmReceiver.EXTRA_ALERT_ID, 0L))
                putExtra(AlarmReceiver.EXTRA_HOUR, source.getIntExtra(AlarmReceiver.EXTRA_HOUR, -1))
                putExtra(AlarmReceiver.EXTRA_MINUTE, source.getIntExtra(AlarmReceiver.EXTRA_MINUTE, -1))
                putExtra(
                    AlarmReceiver.EXTRA_DURATION_SECONDS,
                    source.getLongExtra(AlarmReceiver.EXTRA_DURATION_SECONDS, 0L),
                )
            }

        /** Snooze from the takeover: reschedules five minutes out and stops. */
        fun snoozeAlert(context: Context, label: String) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlertService::class.java)
                        .setAction(ACTION_SNOOZE)
                        .putExtra(AlarmReceiver.EXTRA_LABEL, label),
                )
            }
        }

        fun stopAlert(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlertService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}
