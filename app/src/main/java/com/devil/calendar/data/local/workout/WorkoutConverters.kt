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
}
