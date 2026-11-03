package com.devil.calendar.service

/** What fired. Drives the takeover copy and the notification text. */
enum class AlertKind {
    TIMER,
    ALARM,
    FOCUS,
    /** The workout rest timer. Never reaches the takeover. */
    REST;

    /** Small tracked label at the top of the takeover. */
    val shortLabel: String
        get() = when (this) {
            TIMER -> "TIMER"
            ALARM -> "ALARM"
            FOCUS -> "FOCUS"
            REST -> "REST"
        }

    /** Notification title: what ended, once. */
    val titleText: String
        get() = when (this) {
            TIMER -> "Timer"
            ALARM -> "Alarm"
            FOCUS -> "Focus"
            REST -> "Rest"
        }

    companion object {
        fun from(name: String?): AlertKind =
            entries.firstOrNull { it.name == name } ?: TIMER
    }
}
