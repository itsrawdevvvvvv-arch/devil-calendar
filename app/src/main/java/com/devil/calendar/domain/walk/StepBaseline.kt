package com.devil.calendar.domain.walk

/**
 * Turns a raw `TYPE_STEP_COUNTER` reading into "steps today".
 *
 * The counter is cumulative since boot, so it resets on reboot. It also has to
 * reset at midnight. Both are handled by re-baselining rather than by assuming
 * the number only ever grows - that assumption is what makes step apps show
 * negative or absurd totals after a restart.
 */
object StepBaseline {

    data class Result(val stepsToday: Int, val baseline: Long, val needsPersist: Boolean)

    fun stepsToday(
        rawSensor: Long,
        storedBaseline: Long,
        storedSteps: Int,
        isNewDay: Boolean,
    ): Result {
        // Reboot: the counter went backwards.
        if (rawSensor < storedBaseline) {
            return Result(stepsToday = 0, baseline = rawSensor, needsPersist = true)
        }
        if (isNewDay) {
            return Result(stepsToday = 0, baseline = rawSensor, needsPersist = true)
        }
        val steps = (rawSensor - storedBaseline).toInt().coerceAtLeast(0)
        return Result(stepsToday = steps, baseline = storedBaseline, needsPersist = steps != storedSteps)
    }
}
