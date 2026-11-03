package com.devil.calendar.data.repository

import androidx.room.withTransaction
import com.devil.calendar.data.local.AppDatabase
import com.devil.calendar.data.local.workout.ExerciseEntity
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
            rows.map { RoutineSummary(it.id, it.name, it.exerciseCount) }
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
                    exercises = exercises.map { row ->
                        RoutineExerciseItem(
                            id = row.id,
                            exerciseId = row.exerciseId,
                            name = row.name,
                            muscleGroup = row.muscleGroup,
                            order = row.order,
                            restSeconds = row.restSeconds,
                            supersetGroupId = row.supersetGroupId,
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

    suspend fun createRoutine(name: String): Long =
        dao.insertRoutine(
            RoutineEntity(
                name = name.trim().ifBlank { "New routine" },
                createdAt = System.currentTimeMillis(),
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
                    ),
                )
            }
            newId
        }
    }

    // ---- routine lines ----------------------------------------------------

    suspend fun addExercise(routineId: Long, exerciseId: Long, restSeconds: Int = 90) {
        val next = dao.maxOrder(routineId) + 1
        dao.insertRoutineExercise(
            RoutineExerciseEntity(
                routineId = routineId,
                exerciseId = exerciseId,
                order = next,
                restSeconds = restSeconds,
            ),
        )
    }

    suspend fun removeRoutineExercise(id: Long) = dao.deleteRoutineExercise(id)

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

    /** Seeds the starter library once. Safe to call on every start. */
    suspend fun seedIfEmpty() {
        if (dao.exerciseCount() > 0) return
        dao.insertExercises(DefaultExercises)
    }

    private companion object {
        val DefaultExercises: List<ExerciseEntity> = listOf(
            ExerciseEntity(name = "Bench Press", muscleGroup = "Chest"),
            ExerciseEntity(name = "Incline Dumbbell Press", muscleGroup = "Chest"),
            ExerciseEntity(name = "Cable Fly", muscleGroup = "Chest"),
            ExerciseEntity(name = "Push-up", muscleGroup = "Chest"),
            ExerciseEntity(name = "Deadlift", muscleGroup = "Back"),
            ExerciseEntity(name = "Barbell Row", muscleGroup = "Back"),
            ExerciseEntity(name = "Lat Pulldown", muscleGroup = "Back"),
            ExerciseEntity(name = "Pull-up", muscleGroup = "Back"),
            ExerciseEntity(name = "Seated Cable Row", muscleGroup = "Back"),
            ExerciseEntity(name = "Overhead Press", muscleGroup = "Shoulders"),
            ExerciseEntity(name = "Lateral Raise", muscleGroup = "Shoulders"),
            ExerciseEntity(name = "Face Pull", muscleGroup = "Shoulders"),
            ExerciseEntity(name = "Barbell Curl", muscleGroup = "Biceps"),
            ExerciseEntity(name = "Hammer Curl", muscleGroup = "Biceps"),
            ExerciseEntity(name = "Triceps Pushdown", muscleGroup = "Triceps"),
            ExerciseEntity(name = "Overhead Triceps Extension", muscleGroup = "Triceps"),
            ExerciseEntity(name = "Back Squat", muscleGroup = "Legs"),
            ExerciseEntity(name = "Front Squat", muscleGroup = "Legs"),
            ExerciseEntity(name = "Romanian Deadlift", muscleGroup = "Legs"),
            ExerciseEntity(name = "Leg Press", muscleGroup = "Legs"),
            ExerciseEntity(name = "Walking Lunge", muscleGroup = "Legs"),
            ExerciseEntity(name = "Hip Thrust", muscleGroup = "Glutes"),
            ExerciseEntity(name = "Cable Kickback", muscleGroup = "Glutes"),
            ExerciseEntity(name = "Plank", muscleGroup = "Core"),
            ExerciseEntity(name = "Hanging Leg Raise", muscleGroup = "Core"),
            ExerciseEntity(name = "Cable Crunch", muscleGroup = "Core"),
            ExerciseEntity(name = "Treadmill Run", muscleGroup = "Cardio"),
            ExerciseEntity(name = "Rowing Machine", muscleGroup = "Cardio"),
        )
    }
}
