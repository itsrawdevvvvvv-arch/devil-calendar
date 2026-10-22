package com.devil.calendar.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * All date handling is ISO-8601 (`yyyy-MM-dd`) in the device's default zone.
 * Weeks are Monday-first everywhere, matching the prototype.
 */
object DateUtils {
    private val iso: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    val mondayFirst: WeekFields = WeekFields.of(Locale.UK)

    fun key(date: LocalDate): String = date.format(iso)

    fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key, iso) }.getOrNull()

    fun today(): LocalDate = LocalDate.now()

    fun todayKey(): String = key(today())

    /** Index of the weekday with Monday = 0 … Sunday = 6. */
    fun mondayIndex(date: LocalDate): Int = date.dayOfWeek.value - 1

    fun monthTitle(month: YearMonth, today: LocalDate = today()): String {
        val name = month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
        return if (month.year == today.year) name else "$name ${month.year}"
    }

    /** All dates shown for [month], padded to whole Monday-first weeks. */
    fun gridDates(month: YearMonth): List<LocalDate?> {
        val first = month.atDay(1)
        val offset = mondayIndex(first)
        val days = month.lengthOfMonth()
        val cells = mutableListOf<LocalDate?>()
        repeat(offset) { cells.add(null) }
        for (d in 1..days) cells.add(month.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    /** Monday..Sunday of the week containing [date]. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val monday = date.minusDays(mondayIndex(date).toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
}
