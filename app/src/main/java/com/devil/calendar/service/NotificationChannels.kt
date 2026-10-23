package com.devil.calendar.service

object NotificationChannels {
    /** Low importance, silent: the running countdown and the alert's foreground service. */
    const val TIMER_RUNNING = "timer_running"

    /**
     * The alert channel. Silent on purpose - AlertController owns the sound, so
     * the notification must never make its own. HIGH importance is still needed
     * for the full-screen intent.
     *
     * Versioned because channel settings cannot be changed after creation.
     */
    const val ALARM_ALERT_V2 = "alarm_alert_v2"

    /** Superseded by [ALARM_ALERT_V2]; deleted at startup. */
    const val ALARM_ALERT_OLD = "alarm_alert"
}
