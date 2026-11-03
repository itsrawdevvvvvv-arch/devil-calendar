package com.devil.calendar.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** How a session felt, 1 (drained) to 5 (strong). One row per day. */
@Entity(tableName = "mood_entries")
data class MoodEntity(
    @PrimaryKey val dayKey: String,
    val score: Int,
    val recordedAt: Long,
)

@Dao
interface MoodDao {

    @Query("SELECT * FROM mood_entries ORDER BY dayKey DESC LIMIT 30")
    fun observeRecent(): Flow<List<MoodEntity>>

    @Query("SELECT * FROM mood_entries WHERE dayKey = :dayKey LIMIT 1")
    suspend fun forDay(dayKey: String): MoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: MoodEntity)
}
