package com.devil.calendar.data.local.workout

/** How a set is performed. Persisted by name via [WorkoutConverters]. */
enum class SetType {
    NORMAL,
    WARMUP,
    DROP,
    SUPERSET,
}

/**
 * How an exercise is performed. A hold is timed (plank, L-sit, handstand)
 * rather than counted, so it carries seconds instead of reps.
 */
enum class ExerciseMode {
    REPS,
    HOLD,
}

/** A routine can hold several exercises, or be a quick single-exercise log. */
enum class RoutineKind {
    ROUTINE,
    SINGLE,
}

/** Weight unit for a single set. Persisted by name. */
enum class WeightUnit {
    KG,
    LB,
}
