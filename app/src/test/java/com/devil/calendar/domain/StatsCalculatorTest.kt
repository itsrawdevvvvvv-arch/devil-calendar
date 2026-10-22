package com.devil.calendar.domain

import com.devil.calendar.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsCalculatorTest {

    private fun crossed(vararg dates: LocalDate): Set<String> =
        dates.map { DateUtils.key(it) }.toSet()

    @Test
    fun `streak counts consecutive days ending today`() {
        val today = LocalDate.of(2026, 3, 10)
        val set = crossed(
            today,
            today.minusDays(1),
            today.minusDays(2),
        )
        assertEquals(3, StatsCalculator.streak(set, today))
    }

    @Test
    fun `streak survives when today is not yet crossed`() {
        val today = LocalDate.of(2026, 3, 10)
        val set = crossed(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StatsCalculator.streak(set, today))
    }

    @Test
    fun `streak is zero when neither today nor yesterday is crossed`() {
        val today = LocalDate.of(2026, 3, 10)
        val set = crossed(today.minusDays(2))
        assertEquals(0, StatsCalculator.streak(set, today))
    }

    @Test
    fun `week count counts only the current monday-to-sunday week`() {
        // 2026-03-10 is a Tuesday; that week runs Mon 9th .. Sun 15th.
        val today = LocalDate.of(2026, 3, 10)
        val set = crossed(
            LocalDate.of(2026, 3, 9),
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 16), // next week, must not count
        )
        assertEquals(2, StatsCalculator.weekCount(set, today))
    }

    @Test
    fun `month count only includes the given month`() {
        val month = YearMonth.of(2026, 3)
        val set = crossed(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 2, 28),
        )
        assertEquals(2, StatsCalculator.monthCount(set, month))
        assertEquals(31, StatsCalculator.monthTotal(month))
    }

    @Test
    fun `grid is padded to whole monday-first weeks`() {
        val cells = DateUtils.gridDates(YearMonth.of(2026, 3))
        assertEquals(0, cells.size % 7)
        // March 2026 starts on a Sunday, so the first six cells are padding.
        assertEquals(null, cells[0])
        assertEquals(LocalDate.of(2026, 3, 1), cells[6])
    }
}
