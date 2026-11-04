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
import androidx.core.content.ContextCompat
import com.devil.calendar.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps the rest timer alive in the background and exposes +15s / Skip actions
 * on its notification.
 */
@AndroidEntryPoint
class RestTimerService : Service() {

    @Inject lateinit var controller: RestTimerController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                controller.skip()
                return START_NOT_STICKY
            }
            ACTION_ADD -> {
                controller.adjust(15)
                return START_STICKY
            }
            ACTION_SUB -> {
                controller.adjust(-15)
                return START_STICKY
            }
            else -> {
                val end = intent?.getLongExtra(EXTRA_END, 0L) ?: 0L
                val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
                if (end <= System.currentTimeMillis()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(end, name),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    },
                )
            }
        }
        return START_STICKY
    }

    private fun buildNotification(endMillis: Long, exerciseName: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.devil.calendar.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val add = PendingIntent.getService(
            this,
            1,
            Intent(this, RestTimerService::class.java).setAction(ACTION_ADD),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val sub = PendingIntent.getService(
            this,
            2,
            Intent(this, RestTimerService::class.java).setAction(ACTION_SUB),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val skip = PendingIntent.getService(
            this,
            3,
            Intent(this, RestTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("Rest")
            .setContentText(exerciseName.ifBlank { "Recover before the next set" })
            .setWhen(endMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, "-15s", sub)
            .addAction(0, "+15s", add)
            .addAction(0, "Skip", skip)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 51
        const val ACTION_STOP = "com.devil.calendar.action.REST_STOP"
        const val ACTION_ADD = "com.devil.calendar.action.REST_ADD"
        const val ACTION_SUB = "com.devil.calendar.action.REST_SUB"
        const val EXTRA_END = "extra_rest_end"
        const val EXTRA_NAME = "extra_rest_name"

        fun start(context: Context, endMillis: Long, exerciseName: String, totalSeconds: Int) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                putExtra(EXTRA_END, endMillis)
                putExtra(EXTRA_NAME, exerciseName)
                putExtra("extra_rest_total", totalSeconds)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, RestTimerService::class.java).setAction(ACTION_STOP),
                )
            }
        }
    }
}
