package com.devil.calendar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devil.calendar.data.local.workout.ExerciseEntity
import com.devil.calendar.data.local.workout.RoutineEntity
import com.devil.calendar.data.local.workout.RoutineExerciseEntity
import com.devil.calendar.data.local.workout.SessionExerciseEntity
import com.devil.calendar.data.local.workout.SetEntryEntity
import com.devil.calendar.data.local.workout.WorkoutConverters
import com.devil.calendar.data.local.workout.WorkoutDao
import com.devil.calendar.data.local.workout.WorkoutSessionEntity

@Database(
    entities = [
        DayLogEntity::class,
        AlarmEntity::class,
        RoutineEntity::class,
        ExerciseEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SetEntryEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(WorkoutConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun alarmDao(): AlarmDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "devil_calendar.db"
    }
}
