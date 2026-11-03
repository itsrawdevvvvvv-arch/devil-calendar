package com.devil.calendar.data.local.workout

/** How a set is performed. Persisted by name via [WorkoutConverters]. */
enum class SetType {
    NORMAL,
    WARMUP,
    DROP,
    SUPERSET,
}

/** Weight unit for a single set. Persisted by name. */
enum class WeightUnit {
    KG,
    LB,
}
