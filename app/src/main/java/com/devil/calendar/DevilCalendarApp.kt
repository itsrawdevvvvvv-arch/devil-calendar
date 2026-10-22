package com.devil.calendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.getSystemService
import com.devil.calendar.service.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DevilCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        val manager = getSystemService<NotificationManager>() ?: return

        val timer = NotificationChannel(
            NotificationChannels.TIMER_RUNNING,
            "Running timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing countdown for timers and focus sessions"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarm = NotificationChannel(
            NotificationChannels.ALARM_ALERT,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarms, timers and focus sessions that need your attention"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(listOf(timer, alarm))
    }
}
