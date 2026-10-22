package com.devil.calendar.domain

import com.devil.calendar.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

/** Pure, side-effect-free statistics — mirrors the prototype's JS logic. */
object StatsCalculator {

    /**
     * Consecutive crossed days ending today or yesterday. Counting from
     * yesterday keeps a streak alive until the current day is missed.
     */
    fun streak(crossed: Set<String>, today: LocalDate = DateUtils.today()): Int {
        var cursor = today
        if (DateUtils.key(cursor) !in crossed) cursor = cursor.minusDays(1)
        var count = 0
        while (DateUtils.key(cursor) in crossed) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Monday..Sunday of the current week with crossed flags. */
    fun weekBars(crossed: Set<String>, today: LocalDate = DateUtils.today()): List<Pair<LocalDate, Boolean>> =
        DateUtils.weekOf(today).map { it to (DateUtils.key(it) in crossed) }

    fun weekCount(crossed: Set<String>, today: LocalDate = DateUtils.today()): Int =
        weekBars(crossed, today).count { it.second }

    fun monthCount(crossed: Set<String>, month: YearMonth = YearMonth.from(DateUtils.today())): Int =
        (1..month.lengthOfMonth()).count { DateUtils.key(month.atDay(it)) in crossed }

    fun monthTotal(month: YearMonth = YearMonth.from(DateUtils.today())): Int = month.lengthOfMonth()

    fun isCrossedToday(crossed: Set<String>, today: LocalDate = DateUtils.today()): Boolean =
        DateUtils.key(today) in crossed
}
