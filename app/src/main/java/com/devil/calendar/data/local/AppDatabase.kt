package com.devil.calendar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DayLogEntity::class, AlarmEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        const val NAME = "devil_calendar.db"
    }
}
