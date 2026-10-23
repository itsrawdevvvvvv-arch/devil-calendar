package com.devil.calendar.util

import java.time.DayOfWeek

/** 12/24-hour formatting and the AM/PM conversion used by the alarm pickers. */
object TimeFormat {

    /** "13:45" or "1:45 PM". */
    fun format(hour: Int, minute: Int, use24Hour: Boolean): String =
        if (use24Hour) {
            "%02d:%02d".format(hour, minute)
        } else {
            "%d:%02d %s".format(displayHour(hour), minute, meridiem(hour))
        }

    fun meridiem(hour: Int): String = if (hour < 12) "AM" else "PM"

    /** 24-hour to the 1..12 shown on the wheel. */
    fun displayHour(hour: Int): Int = ((hour + 11) % 12) + 1

    /** 1..12 plus AM/PM back to 24-hour. 12 AM -> 0, 12 PM -> 12. */
    fun to24Hour(displayHour: Int, isPm: Boolean): Int {
        val base = displayHour % 12
        return if (isPm) base + 12 else base
    }

    /** "10:00" or "1:05:00". Used for the timer subtitle and notification text. */
    fun duration(seconds: Long): String {
        val total = seconds.coerceAtLeast(0L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

    /** "Once", "Every day", "Weekdays", "Weekends", or "M W F". */
    fun repeatSummary(days: Set<DayOfWeek>): String = when {
        days.isEmpty() -> "Once"
        days.size == 7 -> "Every day"
        days == setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        ) -> "Weekdays"
        days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
        else -> days.sortedBy { it.value }
            .joinToString(" ") { dayLetters[it.value - 1] }
    }

    val dayOrder: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
}
