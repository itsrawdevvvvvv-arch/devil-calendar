package com.devil.calendar.data.local.workout

import androidx.room.TypeConverter

/**
 * Enums are stored by name. Doing this explicitly (rather than relying on
 * Room's built-in enum support) keeps the on-disk format pinned to the
 * migration SQL in Migrations.kt.
 */
class WorkoutConverters {

    @TypeConverter
    fun toSetType(value: String): SetType = SetType.entries.firstOrNull { it.name == value } ?: SetType.NORMAL

    @TypeConverter
    fun fromSetType(value: SetType): String = value.name

    @TypeConverter
    fun toWeightUnit(value: String): WeightUnit = WeightUnit.entries.firstOrNull { it.name == value } ?: WeightUnit.KG

    @TypeConverter
    fun fromWeightUnit(value: WeightUnit): String = value.name

    @TypeConverter
    fun toExerciseMode(value: String): ExerciseMode =
        ExerciseMode.entries.firstOrNull { it.name == value } ?: ExerciseMode.REPS

    @TypeConverter
    fun fromExerciseMode(value: ExerciseMode): String = value.name

    @TypeConverter
    fun toRoutineKind(value: String): RoutineKind =
        RoutineKind.entries.firstOrNull { it.name == value } ?: RoutineKind.ROUTINE

    @TypeConverter
    fun fromRoutineKind(value: RoutineKind): String = value.name
}
