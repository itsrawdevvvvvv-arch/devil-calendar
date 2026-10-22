package com.devil.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single crossed-out day. `date` is `yyyy-MM-dd`. */
@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey val date: String,
)
