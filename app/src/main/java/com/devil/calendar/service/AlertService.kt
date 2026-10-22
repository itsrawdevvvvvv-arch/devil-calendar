package com.devil.calendar.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.devil.calendar.R
import com.devil.calendar.TakeoverActivity
import com.devil.calendar.ui.takeover.TakeoverScreen
import com.devil.calendar.ui.theme.DevilTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Posts the alert notification (with a full-screen intent), plays the looping
 * alarm sound, vibrates, draws the overlay and launches the TakeoverActivity.
 * Dismissal stops everything.
 */
@AndroidEntryPoint
class AlertService : Service() {

    @Inject lateinit var overlay: OverlayController

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                stopAlert()
                return START_NOT_STICKY
            }
            else -> {
                val kind = AlertKind.from(intent?.getStringExtra(AlarmReceiver.EXTRA_KIND))
                val label = intent?.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty()
                beginAlert(kind, label)
            }
        }
        return START_STICKY
    }

    private fun beginAlert(kind: AlertKind, label: String) {
        val notification = buildNotification(kind, label)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        startAlarmSound()
        startVibration()

        // Layer 1: the window-manager overlay, drawn above whatever app is open.
        overlay.show { insets ->
            DevilTheme(darkTheme = true) {
                TakeoverScreen(
                    label = kind.takeoverLabel,
                    onDismiss = ::stopAlert,
                    contentInsets = insets,
                )
            }
        }

        // Layer 2: launch the takeover activity (allowed from background
        // because the overlay permission is held).
        runCatching { startActivity(TakeoverActivity.intent(this, kind)) }
    }

    private fun buildNotification(kind: AlertKind, label: String): Notification {
        val fullScreen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, TakeoverActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(TakeoverActivity.EXTRA_KIND, kind.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val dismiss = PendingIntent.getService(
            this,
            1,
            Intent(this, AlertService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NotificationChannels.ALARM_ALERT)
            .setSmallIcon(R.drawable.ic_flame)
            .setContentTitle(kind.title)
            .setContentText(label.ifBlank { kind.takeoverLabel })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "DISMISS", dismiss)
            .setContentIntent(fullScreen)
            .build()
    }

    private fun startAlarmSound() {
        runCatching {
            // Raise alarm volume so the sound is actually audible.
            getSystemService<AudioManager>()?.let { am ->
                val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            }
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
        }
    }

    private fun startVibration() {
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService<Vibrator>()
            } ?: return
            vibrator = v
            val pattern = longArrayOf(0, 600, 400, 600, 400, 600)
            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun stopAlert() {
        runCatching { ringtone?.stop() }
        ringtone = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        overlay.hide()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
        overlay.hide()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_DISMISS = "com.devil.calendar.action.DISMISS"

        fun fireIntent(context: Context, kind: AlertKind, label: String): Intent =
            Intent(context, AlertService::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_KIND, kind.name)
                putExtra(AlarmReceiver.EXTRA_LABEL, label)
            }

        /** Immediately shows the takeover (used when a focus session ends). */
        fun fireNow(context: Context, kind: AlertKind, label: String) {
            ContextCompat.startForegroundService(context, fireIntent(context, kind, label))
        }

        fun dismiss(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlertService::class.java).setAction(ACTION_DISMISS),
            )
        }
    }
}
