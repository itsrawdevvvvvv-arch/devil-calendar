package com.devil.calendar

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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

        val running = NotificationChannel(
            NotificationChannels.TIMER_RUNNING,
            "Running timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing countdown for timers and focus sessions"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        // Silent: AlertController owns the sound and vibration, so the
        // notification must not make its own. HIGH is still required for the
        // full-screen intent to launch the takeover.
        val alert = NotificationChannel(
            NotificationChannels.ALARM_ALERT_V2,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alarms, timers and focus sessions that need your attention"
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(listOf(running, alert))

        // The old channel was loud and would double up with AlertController.
        // Channel settings are immutable once created, hence the new id.
        runCatching { manager.deleteNotificationChannel(NotificationChannels.ALARM_ALERT_OLD) }
    }
}
