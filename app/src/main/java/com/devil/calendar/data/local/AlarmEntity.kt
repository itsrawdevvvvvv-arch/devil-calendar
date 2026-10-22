package com.devil.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

/** A saved alarm. Stored so BOOT_COMPLETED can reschedule every one. */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 24-hour. The picker converts to/from 12-hour with AM/PM. */
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    /** Comma-separated [DayOfWeek] values (1 = Monday .. 7 = Sunday). Empty = one-shot. */
    val repeatDays: String = "",
    /** Epoch millis of the next trigger, or 0 when disabled. */
    val triggerAtMillis: Long = 0L,
) {
    val time: LocalTime get() = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    val days: Set<DayOfWeek>
        get() = repeatDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .map { DayOfWeek.of(it) }
            .toSet()

    val isRepeating: Boolean get() = days.isNotEmpty()

    companion object {
        fun encodeDays(days: Set<DayOfWeek>): String =
            days.sortedBy { it.value }.joinToString(",") { it.value.toString() }
    }
}
