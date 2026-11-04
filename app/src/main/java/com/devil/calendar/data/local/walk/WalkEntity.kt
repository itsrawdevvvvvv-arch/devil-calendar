package com.devil.calendar.data.local.walk

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One finished walk. The path is stored as "x,y;x,y;..." in metres, relative to
 * the start, so it can be redrawn without any GPS.
 */
@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,
    val endedAt: Long,
    val steps: Int,
    val distanceMetres: Double,
    val polyline: String,
    /** Generative style index, so a walk looks the same every time it is redrawn. */
    val style: Int,
)

/** Steps per calendar day, so the goal survives reboots and timezone changes. */
@Entity(tableName = "step_days")
data class StepDayEntity(
    @PrimaryKey val dayKey: String,
    val steps: Int,
    /** Raw sensor value at the start of the day, for re-baselining. */
    val sensorBaseline: Long,
)

@Dao
interface WalkDao {

    @Query("SELECT * FROM walks ORDER BY startedAt DESC LIMIT 30")
    fun observeWalks(): Flow<List<WalkEntity>>

    @Insert
    suspend fun insertWalk(walk: WalkEntity): Long

    @Query("SELECT * FROM step_days WHERE dayKey = :dayKey LIMIT 1")
    suspend fun day(dayKey: String): StepDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDay(day: StepDayEntity)

    @Query("SELECT * FROM step_days ORDER BY dayKey DESC LIMIT 7")
    fun observeWeek(): Flow<List<StepDayEntity>>
}
