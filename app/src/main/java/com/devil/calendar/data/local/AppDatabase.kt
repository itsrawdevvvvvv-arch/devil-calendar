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
import com.devil.calendar.data.local.coach.CoachConfigEntity
import com.devil.calendar.data.local.MoodDao
import com.devil.calendar.data.local.MoodEntity
import com.devil.calendar.data.local.coach.CoachDao

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
        CoachConfigEntity::class,
        MoodEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(WorkoutConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao
    abstract fun alarmDao(): AlarmDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun coachDao(): CoachDao
    abstract fun moodDao(): MoodDao

    companion object {
        const val NAME = "devil_calendar.db"
    }
}
