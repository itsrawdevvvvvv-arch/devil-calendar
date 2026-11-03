package com.devil.calendar.data.local.workout

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An entry in the exercise library. Seeded defaults have [isCustom] false. */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean = false,
)
