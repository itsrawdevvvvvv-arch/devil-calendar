package com.devil.calendar.domain

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure alarm scheduling maths. Kept free of Android types so it can be unit
 * tested, including the AM/PM and DST edge cases.
 */
object AlarmTime {

    /**
     * The next moment [time] occurs, respecting [days].
     *
     * - An empty [days] means a one-shot alarm: today if [time] is still ahead,
     *   otherwise tomorrow.
     * - A non-empty [days] means repeat: the next matching weekday whose
     *   occurrence is strictly after [now]. If today matches but the time has
     *   passed, the following matching weekday is used.
     *
     * A time that does not exist because of a DST spring-forward resolves to
     * the instant the clock jumps to, which is what users expect.
     */
    fun nextTrigger(time: LocalTime, days: Set<DayOfWeek>, now: ZonedDateTime): ZonedDateTime {
        if (days.isEmpty()) {
            val today = ZonedDateTime.of(now.toLocalDate(), time, now.zone)
            return if (today.isAfter(now)) {
                today
            } else {
                ZonedDateTime.of(now.toLocalDate().plusDays(1), time, now.zone)
            }
        }

        // Look ahead a full week; one of those days must match.
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek !in days) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }

        // Unreachable in practice, but keeps the function total.
        return ZonedDateTime.of(now.toLocalDate().plusDays(7), time, now.zone)
    }

    /** "7h 52m", "52m", "1m", or "less than a minute". */
    fun describeDelay(target: ZonedDateTime, now: ZonedDateTime): String {
        val minutes = java.time.Duration.between(now, target).toMinutes().coerceAtLeast(0)
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins}m"
            else -> "less than a minute"
        }
    }
}
