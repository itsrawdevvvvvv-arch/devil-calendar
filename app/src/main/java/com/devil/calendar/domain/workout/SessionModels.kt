package com.devil.calendar.domain.workout

import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.local.workout.SetType
import com.devil.calendar.data.local.workout.WeightUnit

/** Immutable models for the live session screen. */

data class SessionSet(
    val id: Long,
    val index: Int,
    val type: SetType,
    val reps: Int,
    /** Seconds held for a timed set; 0 for rep sets. */
    val holdSeconds: Int,
    val weight: Double,
    val unit: WeightUnit,
    val restSeconds: Int,
    val completed: Boolean,
)

data class SessionExerciseItem(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val order: Int,
    val supersetGroupId: Int?,
    /** "A1", "B2" ... or null when the exercise is not in a superset. */
    val supersetLabel: String?,
    /** REPS or HOLD, copied from the routine. */
    val mode: ExerciseMode,
    val holdSeconds: Int,
    val note: String,
    val sets: List<SessionSet>,
) {
    val completedSets: Int get() = sets.count { it.completed }
}

/** What this exercise looked like last time, shown faintly as a target. */
data class GhostSet(
    val reps: Int,
    val holdSeconds: Int,
    val weight: Double,
)

data class ActiveSession(
    val id: Long,
    val routineId: Long?,
    val startTime: Long,
    val exercises: List<SessionExerciseItem>,
    val totalSets: Int,
    val completedSets: Int,
)
