package com.devil.calendar.data.local.workout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** A single logged set. */
@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionExerciseId: Long,
    /** Position within the exercise. "index" is a SQL keyword. */
    @ColumnInfo(name = "set_index") val index: Int,
    val type: SetType = SetType.NORMAL,
    val reps: Int = 0,
    /** Seconds held when the exercise is a timed hold; ignored for reps. */
    val holdSeconds: Int = 0,
    val weight: Double = 0.0,
    val unit: WeightUnit = WeightUnit.KG,
    /** Rate of perceived exertion, 1..10. */
    val rpe: Double? = null,
    val restSeconds: Int = 90,
    val completed: Boolean = false,
    /** Epoch millis when the set was ticked off, or null. */
    val completedAt: Long? = null,
)
