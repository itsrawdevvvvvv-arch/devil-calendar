package com.devil.calendar.domain.workout

/** Immutable models the UI renders. Entities never leave the data layer. */

data class RoutineSummary(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
)

data class RoutineExerciseItem(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val order: Int,
    val restSeconds: Int,
    val supersetGroupId: Int?,
)

data class RoutineDetail(
    val id: Long,
    val name: String,
    val exercises: List<RoutineExerciseItem>,
)

data class ExerciseItem(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean,
)

/** The muscle groups the seeded library uses, also offered for custom exercises. */
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
