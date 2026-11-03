package com.devil.calendar.data.local.workout

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A performed workout.
 *
 * [routineId] is deliberately NOT a foreign key: history must survive the
 * routine being deleted.
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long? = null,
    /** Epoch millis. */
    val startTime: Long,
    /** Epoch millis, or null while the session is still running. */
    val endTime: Long? = null,
    val notes: String = "",
)
