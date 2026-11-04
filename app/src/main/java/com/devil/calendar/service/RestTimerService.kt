package com.devil.calendar.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.devil.calendar.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps the rest timer alive in the background.
 *
 * Two things this gets right that the previous version did not:
 *  - it tears itself down when the rest reaches zero, so the notification can
 *    never sit there with a chronometer counting into negative hours;
 *  - every action ends with a rebuild or a teardown, so a stale notification is
 *    impossible and the buttons always do something.
 */
@AndroidEntryPoint
class RestTimerService : Service() {

    @Inject lateinit var controller: RestTimerController

    private val handler = Handler(Looper.getMainLooper())
    private var endAt = 0L
    private var name = ""

    private val teardown = Runnable { finishAndStop() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // "Next" - start the next set, then tear down. Going through the
                // controller means the in-app ring and the island agree.
                runCatching { controller.startNextSet() }
                finishAndStop()
                return START_NOT_STICKY
            }
            ACTION_ADD -> {
                runCatching { controller.adjust(15) }
                repost()
                return START_STICKY
            }
            ACTION_SUB -> {
                runCatching { controller.adjust(-15) }
                repost()
                return START_STICKY
            }
            ACTION_CANCEL -> {
                // Tear down only. Calling controller.finish() here would call
                // back into this service and recurse.
                finishAndStop()
                return START_NOT_STICKY
            }
            else -> {
                val end = intent?.getLongExtra(EXTRA_END, 0L) ?: 0L
                name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
                if (end <= System.currentTimeMillis()) {
                    finishAndStop()
                    return START_NOT_STICKY
                }
                endAt = end
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
                scheduleStop(end)
            }
        }
        return START_STICKY
    }

    /** Fires exactly when the rest ends. Without this the notification lives forever. */
    private fun scheduleStop(end: Long) {
        handler.removeCallbacks(teardown)
        val delay = (end - System.currentTimeMillis()).coerceAtLeast(0L)
        handler.postDelayed(teardown, delay + 1_000L)
    }

    /** Rebuilds the notification from the controller's current end time. */
    private fun repost() {
        val state = controller.state.value
        if (!state.active) {
            finishAndStop()
            return
        }
        endAt = state.endMillis
        if (state.exerciseName.isNotBlank()) name = state.exerciseName
        runCatching {
            getSystemService(android.app.NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, buildNotification(endAt, name))
        }
        scheduleStop(endAt)
    }

    private fun finishAndStop() {
        handler.removeCallbacks(teardown)
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            getSystemService(android.app.NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        finishAndStop()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(teardown)
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            getSystemService(android.app.NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        }
        super.onDestroy()
    }

    private fun buildNotification(endMillis: Long, exerciseName: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.devil.calendar.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(requestCode: Int, action: String): PendingIntent = PendingIntent.getService(
            this, requestCode,
            Intent(this, RestTimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val content = RemoteViews(packageName, R.layout.notification_rest).apply {
            setTextViewText(R.id.rest_exercise, exerciseName.ifBlank { "Recover" })
            setChronometer(R.id.rest_chronometer, endMillis, null, true)
            setChronometerCountDown(R.id.rest_chronometer, true)
            setOnClickPendingIntent(R.id.rest_minus, action(2, ACTION_SUB))
            setOnClickPendingIntent(R.id.rest_plus, action(1, ACTION_ADD))
            setOnClickPendingIntent(R.id.rest_skip, action(3, ACTION_STOP))
        }

        return NotificationCompat.Builder(this, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setCustomContentView(content)
            .setCustomBigContentView(content)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setWhen(endMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            // Safety net: even if the handler is killed, the notification goes.
            .setTimeoutAfter((endMillis - System.currentTimeMillis()).coerceAtLeast(1_000L) + 5_000L)
            .setContentIntent(open)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 51
        const val ACTION_STOP = "com.devil.calendar.action.REST_STOP"
        const val ACTION_ADD = "com.devil.calendar.action.REST_ADD"
        const val ACTION_SUB = "com.devil.calendar.action.REST_SUB"
        const val ACTION_CANCEL = "com.devil.calendar.action.REST_CANCEL"
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

        /** Hard cancel: stops the service and removes the notification. */
        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, RestTimerService::class.java).setAction(ACTION_CANCEL),
                )
            }
            runCatching {
                context.getSystemService(android.app.NotificationManager::class.java)
                    ?.cancel(NOTIFICATION_ID)
            }
        }
    }
}
