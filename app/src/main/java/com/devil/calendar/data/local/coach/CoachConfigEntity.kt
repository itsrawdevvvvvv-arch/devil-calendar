package com.devil.calendar.data.local.coach

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * A coach config cached by normalised exercise name, so each exercise is
 * analysed once and then works offline forever.
 */
@Entity(tableName = "coach_configs")
data class CoachConfigEntity(
    @PrimaryKey val exerciseKey: String,
    val movement: String,
    val mode: String,
    val cameraAngle: String,
    val cameraDistance: String,
    val tip: String,
    val primaryJoint: String,
    val downAngle: Float,
    val upAngle: Float,
    val tolerance: Float,
    val minRepMillis: Long,
    val fromTemplate: Boolean,
    val createdAt: Long,
)

@Dao
interface CoachDao {

    @Query("SELECT * FROM coach_configs WHERE exerciseKey = :key LIMIT 1")
    suspend fun config(key: String): CoachConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: CoachConfigEntity)

    @Query("SELECT COUNT(*) FROM coach_configs")
    suspend fun count(): Int
}
