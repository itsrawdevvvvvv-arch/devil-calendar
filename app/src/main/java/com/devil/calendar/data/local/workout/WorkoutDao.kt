package com.devil.calendar.data.local.workout

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A routine plus how many exercises it holds. */
data class RoutineSummaryRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val exerciseCount: Int,
)

/** A completed set joined to its session and exercise - the basis of history. */
data class CompletedSetRow(
    val sessionId: Long,
    val startTime: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val reps: Int,
    val weight: Double,
    val unit: String,
)

/** Weight plus its unit, for unit-safe personal-record comparisons. */
data class WeightRow(
    val weight: Double,
    val unit: String,
)

/** One session line joined to its exercise. */
data class SessionExerciseRow(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    @ColumnInfo(name = "sort_order") val order: Int,
    val supersetGroupId: Int?,
)

/** One routine line joined to its exercise. */
data class RoutineExerciseRow(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    @ColumnInfo(name = "sort_order") val order: Int,
    val restSeconds: Int,
    val supersetGroupId: Int?,
)

@Dao
interface WorkoutDao {

    // ---- routines ---------------------------------------------------------

    @Query(
        """
        SELECT r.id AS id, r.name AS name, r.createdAt AS createdAt,
               COUNT(re.id) AS exerciseCount
        FROM routines r
        LEFT JOIN routine_exercises re ON re.routineId = r.id
        GROUP BY r.id, r.name, r.createdAt
        ORDER BY r.createdAt DESC
        """,
    )
    fun observeRoutineSummaries(): Flow<List<RoutineSummaryRow>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutine(id: Long): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun routineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Query("UPDATE routines SET name = :name WHERE id = :id")
    suspend fun renameRoutine(id: Long, name: String)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    // ---- routine exercises ------------------------------------------------

    @Query(
        """
        SELECT re.id AS id, re.exerciseId AS exerciseId, e.name AS name,
               e.muscleGroup AS muscleGroup, re.sort_order AS sort_order,
               re.restSeconds AS restSeconds, re.supersetGroupId AS supersetGroupId
        FROM routine_exercises re
        INNER JOIN exercises e ON e.id = re.exerciseId
        WHERE re.routineId = :routineId
        ORDER BY re.sort_order ASC
        """,
    )
    fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseRow>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY sort_order ASC")
    suspend fun routineExercisesOnce(routineId: Long): List<RoutineExerciseEntity>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM routine_exercises WHERE routineId = :routineId")
    suspend fun maxOrder(routineId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(row: RoutineExerciseEntity): Long

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteRoutineExercise(id: Long)

    @Query("UPDATE routine_exercises SET sort_order = :order WHERE id = :id")
    suspend fun setRoutineExerciseOrder(id: Long, order: Int)

    @Query("UPDATE routine_exercises SET restSeconds = :seconds WHERE id = :id")
    suspend fun setRoutineExerciseRest(id: Long, seconds: Int)

    // ---- exercises --------------------------------------------------------

    @Query(
        """
        SELECT * FROM exercises
        WHERE :query = '' OR name LIKE '%' || :query || '%'
        ORDER BY isCustom ASC, name ASC
        """,
    )
    fun observeExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun exerciseById(id: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomExercise(id: Long)

    // ---- live sessions ----------------------------------------------------

    @Query("SELECT * FROM workout_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun sessionById(id: Long): WorkoutSessionEntity?

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("UPDATE workout_sessions SET endTime = :endTime WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long)

    @Query(
        """
        SELECT se.id AS id, se.exerciseId AS exerciseId, e.name AS name,
               e.muscleGroup AS muscleGroup, se.sort_order AS sort_order,
               se.supersetGroupId AS supersetGroupId
        FROM session_exercises se
        INNER JOIN exercises e ON e.id = se.exerciseId
        WHERE se.sessionId = :sessionId
        ORDER BY se.sort_order ASC
        """,
    )
    fun observeSessionExercises(sessionId: Long): Flow<List<SessionExerciseRow>>

    @Query(
        """
        SELECT s.* FROM set_entries s
        INNER JOIN session_exercises se ON se.id = s.sessionExerciseId
        WHERE se.sessionId = :sessionId
        ORDER BY s.set_index ASC
        """,
    )
    fun observeSessionSets(sessionId: Long): Flow<List<SetEntryEntity>>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY sort_order ASC")
    suspend fun sessionExercisesOnce(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun maxSessionOrder(sessionId: Long): Int

    @Insert
    suspend fun insertSessionExercise(row: SessionExerciseEntity): Long

    @Query("DELETE FROM session_exercises WHERE id = :id")
    suspend fun deleteSessionExercise(id: Long)

    @Query("UPDATE session_exercises SET sort_order = :order WHERE id = :id")
    suspend fun setSessionExerciseOrder(id: Long, order: Int)

    @Query("UPDATE session_exercises SET supersetGroupId = :groupId WHERE id = :id")
    suspend fun setSupersetGroup(id: Long, groupId: Int?)

    // ---- sets -------------------------------------------------------------

    @Insert
    suspend fun insertSetEntry(entry: SetEntryEntity): Long

    @Update
    suspend fun updateSetEntry(entry: SetEntryEntity)

    @Query("SELECT * FROM set_entries WHERE id = :id")
    suspend fun setEntryById(id: Long): SetEntryEntity?

    @Query("DELETE FROM set_entries WHERE id = :id")
    suspend fun deleteSetEntry(id: Long)

    @Query("SELECT COALESCE(MAX(set_index), -1) FROM set_entries WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun maxSetIndex(sessionExerciseId: Long): Int

    @Query("SELECT * FROM set_entries WHERE sessionExerciseId = :sessionExerciseId ORDER BY set_index ASC")
    suspend fun setsForExercise(sessionExerciseId: Long): List<SetEntryEntity>

    // ---- history ----------------------------------------------------------

    @Query("SELECT * FROM workout_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query(
        """
        SELECT s.id AS sessionId, s.startTime AS startTime, e.id AS exerciseId,
               e.name AS exerciseName, e.muscleGroup AS muscleGroup,
               st.reps AS reps, st.weight AS weight, st.unit AS unit
        FROM set_entries st
        INNER JOIN session_exercises se ON se.id = st.sessionExerciseId
        INNER JOIN workout_sessions s ON s.id = se.sessionId
        INNER JOIN exercises e ON e.id = se.exerciseId
        WHERE s.endTime IS NOT NULL AND st.completed = 1
        ORDER BY s.startTime ASC
        """,
    )
    fun observeCompletedSets(): Flow<List<CompletedSetRow>>

    /** The most recent finished session that contains this exercise. */
    @Query(
        """
        SELECT s.id FROM workout_sessions s
        INNER JOIN session_exercises se ON se.sessionId = s.id
        WHERE s.endTime IS NOT NULL AND se.exerciseId = :exerciseId
        ORDER BY s.startTime DESC LIMIT 1
        """,
    )
    suspend fun lastSessionForExercise(exerciseId: Long): Long?

    @Query(
        """
        SELECT st.* FROM set_entries st
        INNER JOIN session_exercises se ON se.id = st.sessionExerciseId
        WHERE se.sessionId = :sessionId AND se.exerciseId = :exerciseId
        ORDER BY st.set_index ASC
        """,
    )
    suspend fun setsForExerciseInSession(sessionId: Long, exerciseId: Long): List<SetEntryEntity>

    /** Every completed weight for an exercise, so records compare across units. */
    @Query(
        """
        SELECT st.weight AS weight, st.unit AS unit FROM set_entries st
        INNER JOIN session_exercises se ON se.id = st.sessionExerciseId
        INNER JOIN workout_sessions s ON s.id = se.sessionId
        WHERE se.exerciseId = :exerciseId AND s.endTime IS NOT NULL
              AND st.completed = 1 AND s.id != :excludeSessionId
        """,
    )
    suspend fun weightsForExercise(exerciseId: Long, excludeSessionId: Long): List<WeightRow>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSessionEntity?>
}
