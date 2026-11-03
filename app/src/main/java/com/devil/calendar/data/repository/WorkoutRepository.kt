package com.devil.calendar.data.repository

import androidx.room.withTransaction
import com.devil.calendar.data.local.AppDatabase
import com.devil.calendar.data.local.workout.ExerciseEntity
import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.local.workout.RoutineKind
import com.devil.calendar.data.local.workout.RoutineEntity
import com.devil.calendar.data.local.workout.RoutineExerciseEntity
import com.devil.calendar.data.local.workout.WorkoutDao
import com.devil.calendar.domain.workout.ExerciseItem
import com.devil.calendar.domain.workout.RoutineDetail
import com.devil.calendar.domain.workout.RoutineExerciseItem
import com.devil.calendar.domain.workout.RoutineSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val dao: WorkoutDao,
    private val database: AppDatabase,
) {
    val routines: Flow<List<RoutineSummary>> =
        dao.observeRoutineSummaries().map { rows ->
            rows.map {
                RoutineSummary(
                    id = it.id,
                    name = it.name,
                    exerciseCount = it.exerciseCount,
                    kind = RoutineKind.entries.firstOrNull { k -> k.name == it.kind }
                        ?: RoutineKind.ROUTINE,
                )
            }
        }

    fun routine(id: Long): Flow<RoutineDetail?> =
        combine(
            dao.observeRoutine(id),
            dao.observeRoutineExercises(id),
        ) { routine, exercises ->
            routine?.let {
                RoutineDetail(
                    id = it.id,
                    name = it.name,
                    kind = it.kind,
                    exercises = exercises.map { row ->
                        RoutineExerciseItem(
                            id = row.id,
                            exerciseId = row.exerciseId,
                            name = row.name,
                            muscleGroup = row.muscleGroup,
                            order = row.order,
                            restSeconds = row.restSeconds,
                            supersetGroupId = row.supersetGroupId,
                            defaultSets = row.defaultSets,
                            mode = ExerciseMode.entries.firstOrNull { m -> m.name == row.mode }
                                ?: ExerciseMode.REPS,
                            holdSeconds = row.holdSeconds,
                            note = row.note,
                        )
                    },
                )
            }
        }

    fun exercises(query: String): Flow<List<ExerciseItem>> =
        dao.observeExercises(query.trim()).map { list ->
            list.map { ExerciseItem(it.id, it.name, it.muscleGroup, it.isCustom) }
        }

    // ---- routines ---------------------------------------------------------

    suspend fun createRoutine(
        name: String,
        kind: RoutineKind = RoutineKind.ROUTINE,
    ): Long =
        dao.insertRoutine(
            RoutineEntity(
                name = name.trim().ifBlank {
                    if (kind == RoutineKind.SINGLE) "New single workout" else "New routine"
                },
                createdAt = System.currentTimeMillis(),
                kind = kind,
            ),
        )

    suspend fun renameRoutine(id: Long, name: String) {
        dao.renameRoutine(id, name.trim().ifBlank { "New routine" })
    }

    suspend fun deleteRoutine(id: Long) = dao.deleteRoutine(id)

    /** Copies the routine and all of its lines. Returns the new id. */
    suspend fun duplicateRoutine(id: Long): Long? {
        val original = dao.routineById(id) ?: return null
        return database.withTransaction {
            val newId = dao.insertRoutine(
                RoutineEntity(
                    name = "${original.name} copy",
                    createdAt = System.currentTimeMillis(),
                    kind = original.kind,
                ),
            )
            dao.routineExercisesOnce(id).forEach { line ->
                dao.insertRoutineExercise(
                    RoutineExerciseEntity(
                        routineId = newId,
                        exerciseId = line.exerciseId,
                        order = line.order,
                        supersetGroupId = line.supersetGroupId,
                        restSeconds = line.restSeconds,
                        defaultSets = line.defaultSets,
                        mode = line.mode,
                        holdSeconds = line.holdSeconds,
                        note = line.note,
                    ),
                )
            }
            newId
        }
    }

    // ---- routine lines ----------------------------------------------------

    /**
     * Adds an exercise by the name the user typed, creating the exercise row on
     * first use. There is no preset library: names come only from this table,
     * which only ever holds names the user has written.
     */
    suspend fun addExerciseByName(routineId: Long, name: String): Long? {
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
            val next = dao.maxOrder(routineId) + 1
            dao.insertRoutineExercise(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    order = next,
                ),
            )
        }
    }

    /** Autocomplete over the user's own exercise names. */
    suspend fun suggestNames(query: String): List<String> =
        dao.suggestExerciseNames(query.trim())

    suspend fun setDefaultSets(lineId: Long, sets: Int) =
        dao.setRoutineExerciseSets(lineId, sets.coerceIn(1, 20))

    suspend fun setMode(lineId: Long, mode: ExerciseMode, holdSeconds: Int) =
        dao.setRoutineExerciseMode(
            id = lineId,
            mode = mode.name,
            holdSeconds = if (mode == ExerciseMode.HOLD) holdSeconds.coerceIn(5, 600) else 0,
        )

    suspend fun removeRoutineExercise(id: Long) = dao.deleteRoutineExercise(id)

    /** Saves a parsed or generated draft as a real routine. */
    /**
     * Saves a parsed or generated draft. Everything happens in ONE transaction:
     * the previous version called addExerciseByName here, which opened a second
     * transaction while this one held the write lock - the save silently hung
     * and the imported routine never appeared.
     */
    suspend fun saveDraft(
        draft: com.devil.calendar.data.ai.RoutineDraft,
        kind: RoutineKind,
    ): Long = database.withTransaction {
        val routineId = dao.insertRoutine(
            RoutineEntity(
                name = draft.name.trim().take(40).ifBlank {
                    if (kind == RoutineKind.SINGLE) "New single workout" else "New routine"
                },
                createdAt = System.currentTimeMillis(),
                kind = kind,
            ),
        )
        var order = 0
        draft.exercises.forEach { exercise ->
            val trimmed = exercise.name.trim().take(40)
            if (trimmed.isEmpty()) return@forEach
            val exerciseId = dao.findExerciseByName(trimmed)?.id
                ?: dao.insertExercise(
                    ExerciseEntity(
                        name = trimmed,
                        muscleGroup = "Other",
                        isCustom = true,
                        userCreated = true,
                    ),
                )
            dao.insertRoutineExercise(
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = exerciseId,
                    order = order++,
                    restSeconds = exercise.restSeconds.coerceIn(0, 600),
                    defaultSets = exercise.sets.coerceIn(1, 20),
                    mode = exercise.mode,
                    holdSeconds = if (exercise.mode == ExerciseMode.HOLD) {
                        exercise.holdSeconds.coerceIn(5, 600)
                    } else {
                        0
                    },
                ),
            )
        }
        routineId
    }

    suspend fun setRestSeconds(id: Long, seconds: Int) =
        dao.setRoutineExerciseRest(id, seconds.coerceIn(0, 600))

    /** Moves a line one slot up or down and renumbers the whole list. */
    suspend fun moveRoutineExercise(routineId: Long, id: Long, delta: Int) {
        database.withTransaction {
            val lines = dao.routineExercisesOnce(routineId).toMutableList()
            val index = lines.indexOfFirst { it.id == id }
            if (index < 0) return@withTransaction
            val target = (index + delta).coerceIn(0, lines.lastIndex)
            if (target == index) return@withTransaction
            val moved = lines.removeAt(index)
            lines.add(target, moved)
            lines.forEachIndexed { position, line ->
                dao.setRoutineExerciseOrder(line.id, position)
            }
        }
    }

    // ---- exercises --------------------------------------------------------

    suspend fun createCustomExercise(name: String, muscleGroup: String): Long =
        dao.insertExercise(
            ExerciseEntity(
                name = name.trim(),
                muscleGroup = muscleGroup,
                isCustom = true,
            ),
        )

    suspend fun deleteCustomExercise(id: Long) = dao.deleteCustomExercise(id)


}
