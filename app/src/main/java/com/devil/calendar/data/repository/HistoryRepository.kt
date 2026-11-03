package com.devil.calendar.data.repository

import com.devil.calendar.data.local.workout.CompletedSetRow
import com.devil.calendar.data.local.workout.SetEntryEntity
import com.devil.calendar.data.local.workout.WeightUnit
import com.devil.calendar.data.local.workout.WorkoutDao
import com.devil.calendar.data.local.workout.WorkoutSessionEntity
import com.devil.calendar.domain.workout.HistoryItem
import com.devil.calendar.domain.workout.MuscleVolume
import com.devil.calendar.domain.workout.ProgressPoint
import com.devil.calendar.domain.workout.RecordCheck
import com.devil.calendar.domain.workout.SessionDetail
import com.devil.calendar.domain.workout.SessionExerciseItem
import com.devil.calendar.domain.workout.SessionSet
import com.devil.calendar.domain.workout.Volume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Everything read-only: history, muscle volume, per-exercise progress and
 * personal records. All volumes are normalised to kilograms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HistoryRepository @Inject constructor(
    private val dao: WorkoutDao,
) {
    fun history(): Flow<List<HistoryItem>> =
        combine(dao.observeCompletedSessions(), dao.observeCompletedSets()) { sessions, sets ->
            val bySession = sets.groupBy { it.sessionId }
            sessions.map { session ->
                val rows = bySession[session.id].orEmpty()
                HistoryItem(
                    id = session.id,
                    startTime = session.startTime,
                    durationMinutes = durationMinutes(session),
                    setCount = rows.size,
                    volumeKg = rows.sumOf { volumeOf(it) },
                )
            }
        }

    /** Calendar keys ("2026-11-03") of days with a finished workout. */
    fun workoutDays(): Flow<Set<String>> =
        dao.observeCompletedSessions().map { sessions ->
            sessions.mapTo(mutableSetOf()) { session ->
                com.devil.calendar.util.DateUtils.key(
                    java.time.Instant.ofEpochMilli(session.startTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate(),
                )
            }
        }

    /** Total kg per calendar day, for the calendar heat map. */
    fun volumeByDay(): Flow<Map<String, Double>> =
        dao.observeCompletedSets().map { sets ->
            sets.groupBy {
                com.devil.calendar.util.DateUtils.key(
                    java.time.Instant.ofEpochMilli(it.startTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate(),
                )
            }.mapValues { (_, rows) -> rows.sumOf { volumeOf(it) } }
        }

    fun muscleVolume(sinceMillis: Long = 0L): Flow<List<MuscleVolume>> =
        dao.observeCompletedSets().map { sets ->
            sets.asSequence()
                .filter { it.startTime >= sinceMillis }
                .groupBy { it.muscleGroup }
                .map { (muscle, rows) -> MuscleVolume(muscle, rows.sumOf { volumeOf(it) }) }
                .sortedByDescending { it.volumeKg }
        }

    fun progress(exerciseId: Long): Flow<List<ProgressPoint>> =
        dao.observeCompletedSets().map { sets ->
            sets.asSequence()
                .filter { it.exerciseId == exerciseId }
                .groupBy { it.startTime }
                .toSortedMap()
                .map { (startTime, rows) ->
                    ProgressPoint(
                        timeMillis = startTime,
                        bestWeightKg = rows.maxOf { weightKg(it) },
                        volumeKg = rows.sumOf { volumeOf(it) },
                    )
                }
        }

    fun detail(sessionId: Long): Flow<SessionDetail?> =
        dao.observeSession(sessionId).flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                combine(
                    dao.observeSessionExercises(sessionId),
                    dao.observeSessionSets(sessionId),
                ) { rows, sets ->
                    val byExercise = sets.groupBy { it.sessionExerciseId }
                    val exercises = rows.map { row ->
                        SessionExerciseItem(
                            id = row.id,
                            exerciseId = row.exerciseId,
                            name = row.name,
                            muscleGroup = row.muscleGroup,
                            order = row.order,
                            supersetGroupId = row.supersetGroupId,
                            supersetLabel = null,
                            mode = com.devil.calendar.data.local.workout.ExerciseMode.entries
                                .firstOrNull { it.name == row.mode }
                                ?: com.devil.calendar.data.local.workout.ExerciseMode.REPS,
                            holdSeconds = row.holdSeconds,
                            note = row.note,
                            sets = byExercise[row.id].orEmpty().map { it.toModel() },
                        )
                    }
                    val completed = exercises.flatMap { exercise ->
                        exercise.sets.filter { it.completed }.map { exercise.muscleGroup to it }
                    }
                    SessionDetail(
                        id = session.id,
                        startTime = session.startTime,
                        durationMinutes = durationMinutes(session),
                        exercises = exercises,
                        volumeKg = completed.sumOf { (_, set) ->
                            Volume.setVolume(set.reps, Volume.toKilograms(set.weight, set.unit))
                        },
                        muscleVolume = completed
                            .groupBy({ it.first }, { Volume.setVolume(it.second.reps, Volume.toKilograms(it.second.weight, it.second.unit)) })
                            .map { (muscle, volumes) -> MuscleVolume(muscle, volumes.sum()) }
                            .sortedByDescending { it.volumeKg },
                    )
                }
            }
        }

    /** The best previous weight for this exercise, in kilograms. */
    suspend fun bestPreviousWeightKg(exerciseId: Long, excludeSessionId: Long): Double =
        dao.weightsForExercise(exerciseId, excludeSessionId)
            .maxOfOrNull { weightKg(it.weight, it.unit) } ?: 0.0

    /** The most reps ever performed at or above [weightKg] for this exercise. */
    suspend fun bestPreviousReps(
        exerciseId: Long,
        excludeSessionId: Long,
        weightKg: Double,
    ): Int {
        val sessionId = dao.lastSessionForExercise(exerciseId) ?: return 0
        if (sessionId == excludeSessionId) return 0
        return dao.setsForExerciseInSession(sessionId, exerciseId)
            .filter { it.completed }
            .filter { Volume.toKilograms(it.weight, it.unit) >= weightKg }
            .maxOfOrNull { it.reps } ?: 0
    }

    /**
     * Compares a freshly completed set against history.
     * Weight records are exact; rep records only count at the same or heavier weight.
     */
    suspend fun checkRecord(
        exerciseId: Long,
        sessionId: Long,
        reps: Int,
        weight: Double,
        unit: WeightUnit,
    ): RecordCheck {
        if (reps <= 0) return RecordCheck.None
        val kg = Volume.toKilograms(weight, unit)
        val previousBest = bestPreviousWeightKg(exerciseId, sessionId)
        val weightRecord = kg > previousBest && kg > 0.0
        val repRecord = !weightRecord && kg > 0.0 &&
            reps > bestPreviousReps(exerciseId, sessionId, kg)
        return RecordCheck(weightRecord, repRecord)
    }

    suspend fun exerciseName(exerciseId: Long): String =
        dao.exerciseById(exerciseId)?.name ?: "Exercise"

    /** Sets from the last finished session for this exercise, for auto-fill. */
    suspend fun previousSets(exerciseId: Long): List<SetEntryEntity> {
        val sessionId = dao.lastSessionForExercise(exerciseId) ?: return emptyList()
        return dao.setsForExerciseInSession(sessionId, exerciseId).filter { it.completed }
    }

    private fun volumeOf(row: CompletedSetRow): Double =
        Volume.setVolume(row.reps, weightKg(row))

    private fun weightKg(row: CompletedSetRow): Double = weightKg(row.weight, row.unit)

    private fun weightKg(weight: Double, unit: String): Double =
        Volume.toKilograms(weight, WeightUnit.entries.firstOrNull { it.name == unit } ?: WeightUnit.KG)

    private fun durationMinutes(session: WorkoutSessionEntity): Int {
        val end = session.endTime ?: return 0
        return max(0, ((end - session.startTime) / 60_000L).toInt())
    }

    private fun SetEntryEntity.toModel(): SessionSet = SessionSet(
        id = id,
        index = index,
        type = type,
        reps = reps,
        holdSeconds = holdSeconds,
        weight = weight,
        unit = unit,
        restSeconds = restSeconds,
        completed = completed,
    )
}
