package com.devil.calendar.data.local.workout

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A named, reusable workout template. */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Epoch millis. */
    val createdAt: Long,
    /** ROUTINE for a multi-exercise template, SINGLE for a quick log. */
    val kind: RoutineKind = RoutineKind.ROUTINE,
)
