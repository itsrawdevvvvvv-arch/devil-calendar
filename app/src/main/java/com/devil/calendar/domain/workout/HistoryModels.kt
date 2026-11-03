package com.devil.calendar.domain.workout

/** Read models for the history, progress and personal-record screens. */

data class HistoryItem(
    val id: Long,
    val startTime: Long,
    val durationMinutes: Int,
    val setCount: Int,
    /** Normalised to kilograms so mixed kg/lb sessions compare. */
    val volumeKg: Double,
)

data class MuscleVolume(
    val muscleGroup: String,
    val volumeKg: Double,
)

data class ProgressPoint(
    val timeMillis: Long,
    /** Heaviest completed set in that session, in kilograms. */
    val bestWeightKg: Double,
    val volumeKg: Double,
)

data class SessionDetail(
    val id: Long,
    val startTime: Long,
    val durationMinutes: Int,
    val exercises: List<SessionExerciseItem>,
    val volumeKg: Double,
    val muscleVolume: List<MuscleVolume>,
)

/** The result of checking one finished set against history. */
data class RecordCheck(
    val isWeightRecord: Boolean,
    val isRepRecord: Boolean,
) {
    val isRecord: Boolean get() = isWeightRecord || isRepRecord

    companion object {
        val None = RecordCheck(isWeightRecord = false, isRepRecord = false)
    }
}
