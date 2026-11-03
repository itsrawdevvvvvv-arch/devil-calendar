package com.devil.calendar.data.local.workout

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A name the user has typed. There is no preset library any more: rows are
 * created on demand and only used to offer autocomplete. Rows seeded by older
 * versions keep [userCreated] false so they never appear in suggestions, but
 * they stay in the table because history references them by id.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean = false,
    val userCreated: Boolean = true,
)
