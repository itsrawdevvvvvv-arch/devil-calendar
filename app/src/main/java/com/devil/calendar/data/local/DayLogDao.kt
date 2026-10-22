package com.devil.calendar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayLogDao {
    @Query("SELECT date FROM day_log ORDER BY date")
    fun observeAll(): Flow<List<String>>

    @Query("SELECT date FROM day_log")
    suspend fun allOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: DayLogEntity)

    @Query("DELETE FROM day_log WHERE date = :date")
    suspend fun delete(date: String)

    @Query("SELECT EXISTS(SELECT 1 FROM day_log WHERE date = :date)")
    suspend fun contains(date: String): Boolean
}
