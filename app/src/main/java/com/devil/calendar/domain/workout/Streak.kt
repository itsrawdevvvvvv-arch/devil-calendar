package com.devil.calendar.domain.workout

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Consecutive training days, counted back from today. A streak survives if the
 * last workout was today or yesterday - missing today should not erase it
 * before the day is over.
 */
object Streak {

    fun current(days: Set<LocalDate>, today: LocalDate): Int {
        if (days.isEmpty()) return 0
        val start = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        var cursor = start
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Days since the last workout, or null when there are none. */
    fun daysSinceLast(days: Set<LocalDate>, today: LocalDate): Long? {
        val last = days.maxOrNull() ?: return null
        return ChronoUnit.DAYS.between(last, today)
    }

    /**
     * 0..1 flame strength: full while the streak is alive, fading over a week
     * of missed days.
     */
    fun flameStrength(days: Set<LocalDate>, today: LocalDate): Float {
        val since = daysSinceLast(days, today) ?: return 0f
        return when {
            since <= 1 -> 1f
            since >= 7 -> 0f
            else -> 1f - (since - 1f) / 6f
        }
    }
}
