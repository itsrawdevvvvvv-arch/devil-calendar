package com.devil.calendar.data.local.workout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One exercise inside a performed session.
 *
 * Only [sessionId] is a foreign key: deleting the session removes its
 * exercises, but deleting an exercise from the library must not erase history,
 * so [exerciseId] is a plain column.
 */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long,
    @ColumnInfo(name = "sort_order") val order: Int,
    val supersetGroupId: Int? = null,
)
