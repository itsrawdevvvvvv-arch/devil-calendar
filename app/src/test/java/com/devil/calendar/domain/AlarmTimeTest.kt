package com.devil.calendar.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(y, m, d, h, min), zone)

    private fun next(time: LocalTime, days: Set<DayOfWeek>, now: ZonedDateTime) =
        AlarmTime.nextTrigger(time, days, now)

    // ---- one-shot ---------------------------------------------------------

    @Test
    fun `fires later today when the time is still ahead`() {
        val now = at(2026, 10, 22, 6, 30)
        val result = next(LocalTime.of(7, 0), emptySet(), now)
        assertEquals(at(2026, 10, 22, 7, 0), result)
    }

    @Test
    fun `rolls to tomorrow when the time has already passed`() {
        val now = at(2026, 10, 22, 8, 0)
        val result = next(LocalTime.of(7, 0), emptySet(), now)
        assertEquals(at(2026, 10, 23, 7, 0), result)
    }

    @Test
    fun `rolls to tomorrow when the time is exactly now`() {
        val now = at(2026, 10, 22, 7, 0)
        val result = next(LocalTime.of(7, 0), emptySet(), now)
        assertEquals(at(2026, 10, 23, 7, 0), result)
    }

    // ---- AM/PM edge cases -------------------------------------------------

    @Test
    fun `12 AM is midnight and fires the next midnight`() {
        val now = at(2026, 10, 22, 20, 0)
        val result = next(LocalTime.of(0, 0), emptySet(), now)
        assertEquals(at(2026, 10, 23, 0, 0), result)
    }

    @Test
    fun `12 PM is noon and fires today when it is still ahead`() {
        val now = at(2026, 10, 22, 9, 0)
        val result = next(LocalTime.of(12, 0), emptySet(), now)
        assertEquals(at(2026, 10, 22, 12, 0), result)
    }

    @Test
    fun `12 PM rolls to tomorrow once noon has passed`() {
        val now = at(2026, 10, 22, 12, 1)
        val result = next(LocalTime.of(12, 0), emptySet(), now)
        assertEquals(at(2026, 10, 23, 12, 0), result)
    }

    // ---- repeats ----------------------------------------------------------

    @Test
    fun `repeating alarm picks the next matching weekday`() {
        // 2026-10-22 is a Thursday; repeat Monday only.
        val now = at(2026, 10, 22, 9, 0)
        val result = next(LocalTime.of(7, 0), setOf(DayOfWeek.MONDAY), now)
        assertEquals(at(2026, 10, 26, 7, 0), result)
    }

    @Test
    fun `repeating alarm fires today when today matches and the time is ahead`() {
        val now = at(2026, 10, 22, 6, 0) // Thursday
        val result = next(LocalTime.of(7, 0), setOf(DayOfWeek.THURSDAY), now)
        assertEquals(at(2026, 10, 22, 7, 0), result)
    }

    @Test
    fun `repeating alarm skips to next week when today matches but the time has passed`() {
        val now = at(2026, 10, 22, 8, 0) // Thursday, 07:00 already gone
        val result = next(LocalTime.of(7, 0), setOf(DayOfWeek.THURSDAY), now)
        assertEquals(at(2026, 10, 29, 7, 0), result)
    }

    @Test
    fun `weekday repeats skip the weekend`() {
        val now = at(2026, 10, 23, 20, 0) // Friday evening
        val result = next(
            LocalTime.of(7, 0),
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            now,
        )
        assertEquals(at(2026, 10, 26, 7, 0), result) // Monday
    }

    // ---- DST --------------------------------------------------------------

    @Test
    fun `handles the spring-forward gap without throwing`() {
        // New York springs forward at 02:00 on 2026-03-08; 02:30 does not exist.
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 3, 7, 12, 0), ny)
        val result = AlarmTime.nextTrigger(LocalTime.of(2, 30), emptySet(), now)
        assertEquals(LocalDate.of(2026, 3, 8), result.toLocalDate())
        // 02:30 does not exist, so it lands on the instant the clock jumps to.
        assertEquals(LocalTime.of(3, 30), result.toLocalTime())
    }

    @Test
    fun `handles the autumn fall-back`() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 11, 1, 0, 30), ny)
        val result = AlarmTime.nextTrigger(LocalTime.of(1, 30), emptySet(), now)
        assertEquals(LocalDate.of(2026, 11, 1), result.toLocalDate())
    }

    // ---- helper text ------------------------------------------------------

    @Test
    fun `describeDelay formats hours and minutes`() {
        val now = at(2026, 10, 22, 6, 8)
        assertEquals("7h 52m", AlarmTime.describeDelay(at(2026, 10, 22, 14, 0), now))
        assertEquals("52m", AlarmTime.describeDelay(at(2026, 10, 22, 7, 0), now))
        assertEquals("less than a minute", AlarmTime.describeDelay(at(2026, 10, 22, 6, 8), now))
    }
}
