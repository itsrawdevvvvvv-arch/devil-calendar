package com.devil.calendar.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.devil.calendar.MainActivity
import com.devil.calendar.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The ongoing countdown notification for a running timer or focus session.
 *
 * It uses a chronometer (count-down) instead of a per-second tick, so the
 * notification costs nothing to keep updated and the process can be killed
 * without losing time — the real trigger is the AlarmManager alert scheduled
 * from an absolute end timestamp.
 */
@AndroidEntryPoint
class CountdownService : Service() {

    @Inject lateinit var scheduler: AlarmScheduler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scheduler.cancel(AlertKind.from(intent.getStringExtra(EXTRA_KIND)))
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val kind = AlertKind.from(intent?.getStringExtra(EXTRA_KIND))
                val end = intent?.getLongExtra(EXTRA_END, 0L) ?: 0L
                val label = intent?.getStringExtra(EXTRA_LABEL).orEmpty()
                val durationSeconds = intent?.getLongExtra(EXTRA_DURATION, 0L) ?: 0L
                if (end <= System.currentTimeMillis()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(kind, end, label),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    },
                )
                scheduler.schedule(kind, end, label, durationSeconds)
            }
        }
        return START_STICKY
    }

    private fun buildNotification(kind: AlertKind, endMillis: Long, label: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, CountdownService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_KIND, kind.name)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(if (kind == AlertKind.FOCUS) "Focus session" else "Timer")
            .setContentText(label.ifBlank { kind.titleText })
            .setWhen(endMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 24
        const val ACTION_STOP = "com.devil.calendar.action.STOP_COUNTDOWN"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_END = "extra_end"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_DURATION = "extra_duration"

        fun start(
            context: Context,
            kind: AlertKind,
            endMillis: Long,
            label: String,
            durationSeconds: Long,
        ) {
            val intent = Intent(context, CountdownService::class.java).apply {
                putExtra(EXTRA_KIND, kind.name)
                putExtra(EXTRA_END, endMillis)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_DURATION, durationSeconds)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context, kind: AlertKind) {
            val intent = Intent(context, CountdownService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_KIND, kind.name)
            }
            runCatching { context.startService(intent) }
        }
    }
}
