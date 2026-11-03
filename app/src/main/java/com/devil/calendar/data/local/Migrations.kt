package com.devil.calendar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema 2 -> 3: adds the workout tables.
 *
 * Strictly additive - no existing table is touched, so crossed days, alarms and
 * preferences survive the upgrade. The SQL mirrors exactly what Room derives
 * from the entities (column order, NOT NULL, foreign keys); changing one
 * without the other makes Room reject the schema at open time.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routines` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`muscleGroup` TEXT NOT NULL, " +
                "`isCustom` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routine_exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`routineId` INTEGER NOT NULL, " +
                "`exerciseId` INTEGER NOT NULL, " +
                "`sort_order` INTEGER NOT NULL, " +
                "`supersetGroupId` INTEGER, " +
                "`restSeconds` INTEGER NOT NULL, " +
                "FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`routineId` INTEGER, " +
                "`startTime` INTEGER NOT NULL, " +
                "`endTime` INTEGER, " +
                "`notes` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` INTEGER NOT NULL, " +
                "`exerciseId` INTEGER NOT NULL, " +
                "`sort_order` INTEGER NOT NULL, " +
                "`supersetGroupId` INTEGER, " +
                "FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `set_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionExerciseId` INTEGER NOT NULL, " +
                "`set_index` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`reps` INTEGER NOT NULL, " +
                "`weight` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL, " +
                "`rpe` REAL, " +
                "`restSeconds` INTEGER NOT NULL, " +
                "`completed` INTEGER NOT NULL, " +
                "`completedAt` INTEGER, " +
                "FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }

}

/**
 * Phase 1 of the calisthenics rework: routines gain a kind, exercise lines gain
 * sets / mode / hold seconds / a note, and the old seeded library is marked as
 * not-user-created so it disappears from autocomplete without deleting rows
 * that history still points at. Purely additive - no table is dropped.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routines ADD COLUMN kind TEXT NOT NULL DEFAULT 'ROUTINE'")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN defaultSets INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN mode TEXT NOT NULL DEFAULT 'REPS'")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN holdSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        // Existing rows are the old seeded library: hide them from suggestions.
        db.execSQL("ALTER TABLE exercises ADD COLUMN userCreated INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Phase 3: timed holds become first-class. A set can carry seconds as well as
 * reps, and a session line remembers whether it is a rep or hold exercise.
 * Additive only.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_entries ADD COLUMN holdSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE session_exercises ADD COLUMN mode TEXT NOT NULL DEFAULT 'REPS'")
        db.execSQL("ALTER TABLE session_exercises ADD COLUMN holdSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE session_exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}
