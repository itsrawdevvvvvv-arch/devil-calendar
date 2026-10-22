package com.devil.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved alarm. Stored so BOOT_COMPLETED can reschedule every one. */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    /** Epoch millis of the next trigger, or 0 when disabled. */
    val triggerAtMillis: Long = 0L,
)
