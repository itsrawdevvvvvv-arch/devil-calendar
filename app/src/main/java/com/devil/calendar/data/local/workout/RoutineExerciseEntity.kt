package com.devil.calendar.data.local.workout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One exercise inside a routine, in display order.
 *
 * Deleting the routine or the exercise removes the line; it never removes
 * workout history (session rows deliberately carry no foreign key to either).
 */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineId: Long,
    val exerciseId: Long,
    /** "order" is a SQL keyword, so the column is named sort_order. */
    @ColumnInfo(name = "sort_order") val order: Int,
    /** 1..n groups exercises into a superset round; null means standalone. */
    val supersetGroupId: Int? = null,
    val restSeconds: Int = 90,
)
