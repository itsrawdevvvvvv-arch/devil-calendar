package com.devil.calendar.data.repository

import androidx.room.withTransaction
import com.devil.calendar.data.local.AppDatabase
import com.devil.calendar.data.local.workout.RoutineExerciseEntity
import com.devil.calendar.data.local.workout.SessionExerciseEntity
import com.devil.calendar.data.local.workout.SessionExerciseRow
import com.devil.calendar.data.local.workout.ExerciseEntity
import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.local.workout.SetEntryEntity
import com.devil.calendar.data.local.workout.SetType
import com.devil.calendar.data.local.workout.WeightUnit
import com.devil.calendar.data.local.workout.WorkoutDao
import com.devil.calendar.data.local.workout.WorkoutSessionEntity
import com.devil.calendar.domain.workout.ActiveSession
import com.devil.calendar.domain.workout.GhostSet
import com.devil.calendar.domain.workout.SessionExerciseItem
import com.devil.calendar.domain.workout.SessionSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The live session. Everything is written to Room on every change, so process
 * death loses nothing: an unfinished session (endTime IS NULL) is the source of
 * truth and the screen simply re-reads it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SessionRepository @Inject constructor(
    private val dao: WorkoutDao,
    private val database: AppDatabase,
    private val history: HistoryRepository,
) {
    fun observeActiveSession(): Flow<ActiveSession?> =
        dao.observeActiveSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                combine(
                    dao.observeSessionExercises(session.id),
                    dao.observeSessionSets(session.id),
                ) { exercises, sets -> toModel(session, exercises, sets) }
            }
        }

    private fun toModel(
        session: WorkoutSessionEntity,
        rows: List<SessionExerciseRow>,
        sets: List<SetEntryEntity>,
    ): ActiveSession {
        val labels = supersetLabels(rows)
        val byExercise = sets.groupBy { it.sessionExerciseId }
        val items = rows.map { row ->
            SessionExerciseItem(
                id = row.id,
                exerciseId = row.exerciseId,
                name = row.name,
                muscleGroup = row.muscleGroup,
                order = row.order,
                supersetGroupId = row.supersetGroupId,
                supersetLabel = labels[row.id],
                mode = ExerciseMode.entries.firstOrNull { it.name == row.mode }
                    ?: ExerciseMode.REPS,
                holdSeconds = row.holdSeconds,
                note = row.note,
                sets = byExercise[row.id].orEmpty().map { entry ->
                    SessionSet(
                        id = entry.id,
                        index = entry.index,
                        type = entry.type,
                        reps = entry.reps,
                        holdSeconds = entry.holdSeconds,
                        weight = entry.weight,
                        unit = entry.unit,
                        restSeconds = entry.restSeconds,
                        completed = entry.completed,
                    )
                },
            )
        }
        return ActiveSession(
            id = session.id,
            routineId = session.routineId,
            startTime = session.startTime,
            exercises = items,
            totalSets = items.sumOf { it.sets.size },
            completedSets = items.sumOf { it.completedSets },
        )
    }

    /** Groups of two or more get a letter; members get A1, A2 ... */
    private fun supersetLabels(rows: List<SessionExerciseRow>): Map<Long, String?> {
        val groups = LinkedHashMap<Int, MutableList<Long>>()
        rows.forEach { row ->
            val group = row.supersetGroupId ?: return@forEach
            groups.getOrPut(group) { mutableListOf() }.add(row.id)
        }
        val result = mutableMapOf<Long, String?>()
        var letter = 0
        groups.forEach { (_, ids) ->
            if (ids.size < 2) {
                ids.forEach { result[it] = null }
            } else {
                val prefix = ('A' + letter)
                letter++
                ids.forEachIndexed { position, id -> result[id] = "$prefix${position + 1}" }
            }
        }
        return result
    }

    // ---- starting and finishing -------------------------------------------

    /**
     * Starts a session, copying the routine's exercises and seeding three blank
     * sets per exercise. Pass a null or 0 routine id to start empty.
     */
    suspend fun startSession(routineId: Long?): Long = database.withTransaction {
        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                routineId = routineId?.takeIf { it > 0L },
                startTime = System.currentTimeMillis(),
            ),
        )
        if (routineId != null && routineId > 0L) {
            dao.routineExercisesOnce(routineId).forEach { line ->
                val exerciseRowId = dao.insertSessionExercise(
                    SessionExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = line.exerciseId,
                        order = line.order,
                        supersetGroupId = line.supersetGroupId,
                        mode = line.mode,
                        holdSeconds = line.holdSeconds,
                        note = line.note,
                    ),
                )
                // Auto-fill from the last time this exercise was performed.
                val previous = history.previousSets(line.exerciseId)
                if (previous.isEmpty()) {
                    repeat(line.defaultSets.coerceAtLeast(1)) { index ->
                        dao.insertSetEntry(
                            SetEntryEntity(
                                sessionExerciseId = exerciseRowId,
                                index = index,
                                holdSeconds = line.holdSeconds,
                                restSeconds = line.restSeconds,
                            ),
                        )
                    }
                } else {
                    previous.forEachIndexed { index, set ->
                        dao.insertSetEntry(
                            SetEntryEntity(
                                sessionExerciseId = exerciseRowId,
                                index = index,
                                type = set.type,
                                reps = set.reps,
                                weight = set.weight,
                                unit = set.unit,
                                restSeconds = line.restSeconds,
                                completed = false,
                            ),
                        )
                    }
                }
            }
        }
        sessionId
    }

    suspend fun finishSession(sessionId: Long) {
        dao.endSession(sessionId, System.currentTimeMillis())
    }

    // ---- exercises in the session -----------------------------------------

    suspend fun addExercise(sessionId: Long, exerciseId: Long) = database.withTransaction {
        val rowId = dao.insertSessionExercise(
            SessionExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                order = dao.maxSessionOrder(sessionId) + 1,
            ),
        )
        repeat(DEFAULT_SETS) { index ->
            dao.insertSetEntry(SetEntryEntity(sessionExerciseId = rowId, index = index))
        }
    }

    /** Adds an exercise mid-session by the name the user typed. */
    suspend fun addExerciseByName(sessionId: Long, name: String): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return database.withTransaction {
            val exerciseId = dao.findExerciseByName(trimmed)?.id
                ?: dao.insertExercise(
                    ExerciseEntity(
                        name = trimmed,
                        muscleGroup = "Other",
                        isCustom = true,
                        userCreated = true,
                    ),
                )
            val rowId = dao.insertSessionExercise(
                SessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    order = dao.maxSessionOrder(sessionId) + 1,
                ),
            )
            repeat(DEFAULT_SETS) { index ->
                dao.insertSetEntry(SetEntryEntity(sessionExerciseId = rowId, index = index))
            }
            rowId
        }
    }

    suspend fun removeExercise(sessionExerciseId: Long) =
        dao.deleteSessionExercise(sessionExerciseId)

    suspend fun moveExercise(sessionId: Long, sessionExerciseId: Long, delta: Int) {
        database.withTransaction {
            val lines = dao.sessionExercisesOnce(sessionId).toMutableList()
            val index = lines.indexOfFirst { it.id == sessionExerciseId }
            if (index < 0) return@withTransaction
            val target = (index + delta).coerceIn(0, lines.lastIndex)
            if (target == index) return@withTransaction
            lines.add(target, lines.removeAt(index))
            lines.forEachIndexed { position, line ->
                dao.setSessionExerciseOrder(line.id, position)
            }
        }
    }

    /** Links this exercise with the one above it, or unlinks it. */
    suspend fun toggleSupersetWithPrevious(sessionId: Long, sessionExerciseId: Long) {
        database.withTransaction {
            val lines = dao.sessionExercisesOnce(sessionId)
            val index = lines.indexOfFirst { it.id == sessionExerciseId }
            if (index <= 0) return@withTransaction
            val current = lines[index]
            val previous = lines[index - 1]
            if (current.supersetGroupId != null && current.supersetGroupId == previous.supersetGroupId) {
                dao.setSupersetGroup(current.id, null)
            } else {
                val group = previous.supersetGroupId
                    ?: ((lines.mapNotNull { it.supersetGroupId }.maxOrNull() ?: 0) + 1)
                dao.setSupersetGroup(previous.id, group)
                dao.setSupersetGroup(current.id, group)
            }
        }
    }

    /**
     * True when [sessionExerciseId] is the last exercise of its superset round,
     * which is the only point at which a full rest is earned.
     */
    /**
     * Writes a camera rep count into the set and completes it, in one place, so
     * a camera-finished set goes through the same bookkeeping as a tapped one.
     * Idempotent: completing an already-completed set changes nothing.
     */
    suspend fun completeSetWithReps(setId: Long, reps: Int, holdSeconds: Int): Boolean {
        val existing = dao.setEntryById(setId) ?: return false
        if (existing.completed) return false
        dao.updateSetEntry(
            existing.copy(
                reps = reps.coerceAtLeast(0),
                holdSeconds = holdSeconds.coerceAtLeast(0),
                completed = true,
                completedAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    /** Draft count while the camera is still running, so back never loses it. */
    suspend fun saveDraftReps(setId: Long, reps: Int, holdSeconds: Int) {
        val existing = dao.setEntryById(setId) ?: return
        if (existing.completed) return
        dao.updateSetEntry(
            existing.copy(
                reps = reps.coerceAtLeast(0),
                holdSeconds = holdSeconds.coerceAtLeast(0),
            ),
        )
    }

    suspend fun setHold(setId: Long, seconds: Int) =
        dao.setEntryHold(setId, seconds.coerceIn(0, 600))

    /** Last session's completed sets per exercise, for the ghost hint. */
    suspend fun ghosts(sessionId: Long): Map<Long, List<GhostSet>> {
        val exerciseIds = dao.sessionExercisesOnce(sessionId).map { it.exerciseId }.distinct()
        return exerciseIds.mapNotNull { exerciseId ->
            val previous = history.previousSets(exerciseId)
            if (previous.isEmpty()) {
                null
            } else {
                exerciseId to previous.map {
                    GhostSet(reps = it.reps, holdSeconds = it.holdSeconds, weight = it.weight)
                }
            }
        }.toMap()
    }

    suspend fun isLastOfSupersetRound(sessionId: Long, sessionExerciseId: Long): Boolean {
        val lines = dao.sessionExercisesOnce(sessionId)
        val current = lines.firstOrNull { it.id == sessionExerciseId } ?: return true
        val group = current.supersetGroupId ?: return true
        val last = lines.lastOrNull { it.supersetGroupId == group } ?: return true
        return last.id == current.id
    }

    // ---- sets -------------------------------------------------------------

    suspend fun addSet(sessionExerciseId: Long) {
        val existing = dao.setsForExercise(sessionExerciseId)
        val last = existing.lastOrNull()
        dao.insertSetEntry(
            SetEntryEntity(
                sessionExerciseId = sessionExerciseId,
                index = (last?.index ?: -1) + 1,
                reps = last?.reps ?: 0,
                weight = last?.weight ?: 0.0,
                unit = last?.unit ?: WeightUnit.KG,
                restSeconds = last?.restSeconds ?: 90,
            ),
        )
    }

    suspend fun deleteSet(setId: Long) = dao.deleteSetEntry(setId)

    suspend fun adjustWeight(setId: Long, delta: Double) {
        val entry = dao.setEntryById(setId) ?: return
        dao.updateSetEntry(entry.copy(weight = (entry.weight + delta).coerceAtLeast(0.0)))
    }

    suspend fun adjustReps(setId: Long, delta: Int) {
        val entry = dao.setEntryById(setId) ?: return
        dao.updateSetEntry(entry.copy(reps = (entry.reps + delta).coerceAtLeast(0)))
    }

    suspend fun cycleSetType(setId: Long) {
        val entry = dao.setEntryById(setId) ?: return
        val order = SetType.entries
        val next = order[(order.indexOf(entry.type) + 1) % order.size]
        dao.updateSetEntry(entry.copy(type = next))
    }

    /** Returns the rest seconds to run for, or null when no rest is due. */
    suspend fun setCompleted(setId: Long, completed: Boolean): Int? {
        val entry = dao.setEntryById(setId) ?: return null
        dao.updateSetEntry(
            entry.copy(
                completed = completed,
                completedAt = if (completed) System.currentTimeMillis() else null,
            ),
        )
        return if (completed) entry.restSeconds else null
    }

    private companion object {
        const val DEFAULT_SETS = 3
    }
}
