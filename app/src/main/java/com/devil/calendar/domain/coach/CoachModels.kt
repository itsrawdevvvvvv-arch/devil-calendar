package com.devil.calendar.domain.coach

/**
 * What the camera is looking for. Deliberately a closed set: the coach tunes
 * from these rather than inventing a movement, because a made-up config cannot
 * be validated.
 */
enum class MovementType {
    PUSH_UP,
    PULL_UP,
    DIP,
    SQUAT,
    LUNGE,
    PISTOL,
    SIT_UP,
    LEG_RAISE,
    PIKE_PUSH_UP,
    BURPEE,
    PLANK,
    L_SIT,
    HANDSTAND,
    FRONT_LEVER,
    PLANCHE,
    HUMAN_FLAG,
    DB_CURL,
    DB_PRESS,
    DB_ROW,
    GOBLET_SQUAT,
    CUSTOM,
    ;

    val isHold: Boolean
        get() = this in setOf(PLANK, L_SIT, HANDSTAND, FRONT_LEVER, PLANCHE, HUMAN_FLAG)
}

enum class CoachMode { REPS, HOLD }

/**
 * Everything the rep counter and the skeleton need for one exercise.
 * Angles are degrees at the primary joint; [downAngle] is the bottom of the
 * movement and [upAngle] the top.
 */
data class CoachConfig(
    val exerciseKey: String,
    val movement: MovementType,
    val mode: CoachMode,
    val cameraAngle: String,
    val cameraDistance: String,
    val tip: String,
    val primaryJoint: String,
    val downAngle: Float,
    val upAngle: Float,
    val tolerance: Float,
    val minRepMillis: Long,
    /** True when this came from the built-in templates rather than Sarvam. */
    val fromTemplate: Boolean,
)

/** One frame's worth of derived numbers. Never leaves the device. */
data class PoseFrame(
    val primaryAngle: Float,
    val valid: Boolean,
)

data class CoachFeedback(
    val reps: Int,
    val phase: RepPhase,
    val formMessage: String?,
    val holdSeconds: Int,
)

enum class RepPhase { UP, DOWN, UNKNOWN }
