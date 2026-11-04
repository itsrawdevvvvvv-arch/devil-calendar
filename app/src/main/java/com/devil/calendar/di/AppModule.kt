package com.devil.calendar.di

import android.content.Context
import androidx.room.Room
import com.devil.calendar.data.local.AlarmDao
import com.devil.calendar.data.local.AppDatabase
import com.devil.calendar.data.local.DayLogDao
import com.devil.calendar.data.local.MIGRATION_2_3
import com.devil.calendar.data.local.MIGRATION_3_4
import com.devil.calendar.data.local.MIGRATION_4_5
import com.devil.calendar.data.local.MIGRATION_5_6
import com.devil.calendar.data.local.MIGRATION_6_7
import com.devil.calendar.data.local.MIGRATION_7_8
import com.devil.calendar.data.local.workout.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Real migrations from 2 onward. Only a pre-1.1 install (schema 1)
            // may be rebuilt from scratch - that build already wiped on upgrade.
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigrationFrom(1)
            .build()

    @Provides
    fun provideDayLogDao(db: AppDatabase): DayLogDao = db.dayLogDao()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun provideCoachDao(db: AppDatabase): com.devil.calendar.data.local.coach.CoachDao =
        db.coachDao()

    @Provides
    fun provideMoodDao(db: AppDatabase): com.devil.calendar.data.local.MoodDao = db.moodDao()

    @Provides
    fun provideWalkDao(db: AppDatabase): com.devil.calendar.data.local.walk.WalkDao = db.walkDao()
}
