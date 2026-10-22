package com.devil.calendar.service

/** What fired. Drives the takeover label and the countdown copy. */
enum class AlertKind {
    TIMER,
    ALARM,
    FOCUS;

    val takeoverLabel: String
        get() = when (this) {
            TIMER -> "TIMER FINISHED"
            ALARM -> "ALARM"
            FOCUS -> "FOCUS SESSION COMPLETE"
        }

    val title: String
        get() = when (this) {
            TIMER -> "TIME'S UP"
            ALARM -> "ALARM"
            FOCUS -> "FOCUS COMPLETE"
        }

    companion object {
        fun from(name: String?): AlertKind =
            entries.firstOrNull { it.name == name } ?: TIMER
    }
}
