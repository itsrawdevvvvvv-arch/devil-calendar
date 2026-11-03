package com.devil.calendar.data.ai

import com.devil.calendar.data.local.workout.ExerciseMode

/** A routine that has been parsed but not yet saved. */
data class RoutineDraft(
    val name: String,
    val exercises: List<DraftExercise>,
) {
    val isEmpty: Boolean get() = exercises.isEmpty()
}

data class DraftExercise(
    val name: String,
    val sets: Int,
    val mode: ExerciseMode,
    val reps: Int,
    val holdSeconds: Int,
    val restSeconds: Int,
)
