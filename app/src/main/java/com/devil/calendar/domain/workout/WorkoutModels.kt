package com.devil.calendar.domain.workout

import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.local.workout.RoutineKind

/** Immutable models the UI renders. Entities never leave the data layer. */

data class RoutineSummary(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val kind: RoutineKind,
)

data class RoutineExerciseItem(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val order: Int,
    val restSeconds: Int,
    val supersetGroupId: Int?,
    val defaultSets: Int,
    val mode: ExerciseMode,
    val holdSeconds: Int,
    val note: String,
)

data class RoutineDetail(
    val id: Long,
    val name: String,
    val kind: RoutineKind,
    val exercises: List<RoutineExerciseItem>,
)

data class ExerciseItem(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean,
)

/** Rest presets offered in the builder. */
val RestPresets: List<Int> = listOf(30, 60, 90, 120)

/** Kept for older call sites; the library is gone, so this is only a fallback. */
val MuscleGroups: List<String> = listOf(
    "Chest",
    "Back",
    "Shoulders",
    "Biceps",
    "Triceps",
    "Legs",
    "Glutes",
    "Core",
    "Cardio",
    "Full body",
)
