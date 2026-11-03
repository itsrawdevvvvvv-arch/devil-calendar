package com.devil.calendar.camera

import com.devil.calendar.domain.coach.CoachConfig
import com.devil.calendar.domain.coach.CoachMode
import com.devil.calendar.domain.coach.RepPhase

/**
 * Counts reps with hysteresis and a minimum rep duration.
 *
 * Two thresholds, not one: the movement must pass [downAngle] minus tolerance
 * to count as "down" and [upAngle] minus tolerance to count as "up". A single
 * threshold flickers when the angle hovers on it, which double-counts.
 */
class RepStateMachine(private val config: CoachConfig) {

    private var phase: RepPhase = RepPhase.UNKNOWN
    private var lastRepAt: Long = 0L
    private var downEnteredAt: Long = 0L
    private var smoothed: Float? = null
    private var holdStartedAt: Long = 0L
    private var holding = false

    // The observed angle range for this set. A front-on push-up never reaches
    // the side-on angles in the config, which is why it counted 2 of 9.
    private var minSeen = Float.MAX_VALUE
    private var maxSeen = -Float.MAX_VALUE

    var reps: Int = 0
        private set

    var holdSeconds: Int = 0
        private set

    private val configDown = config.downAngle + config.tolerance
    private val configUp = config.upAngle - config.tolerance

    fun reset() {
        phase = RepPhase.UNKNOWN
        lastRepAt = 0L
        downEnteredAt = 0L
        smoothed = null
        holdStartedAt = 0L
        holding = false
        minSeen = Float.MAX_VALUE
        maxSeen = -Float.MAX_VALUE
        reps = 0
        holdSeconds = 0
    }

    /** A manual correction for a miscount. */
    fun adjustReps(delta: Int) {
        reps = (reps + delta).coerceAtLeast(0)
    }

    /**
     * Feeds one frame. Returns the current phase; increments [reps] when a full
     * down-up cycle completes.
     */
    fun onFrame(angle: Float?, now: Long): RepPhase {
        if (angle == null) return phase
        smoothed = PoseMath.smooth(smoothed, angle)
        val value = smoothed ?: angle

        if (config.mode == CoachMode.HOLD) {
            // A hold only accrues while the pose is inside the tolerance band.
            val valid = value >= config.upAngle - config.tolerance ||
                value <= config.downAngle + config.tolerance
            if (valid) {
                if (!holding) {
                    holding = true
                    holdStartedAt = now
                }
                holdSeconds = ((now - holdStartedAt) / 1000L).toInt()
            } else {
                holding = false
            }
            phase = if (valid) RepPhase.UP else RepPhase.DOWN
            return phase
        }

        // Calibrate to this person, this camera, this room: once we have seen
        // enough movement, the thresholds follow their own range instead of the
        // template's idealised angles.
        minSeen = minOf(minSeen, value)
        maxSeen = maxOf(maxSeen, value)
        val range = maxSeen - minSeen
        val calibrated = range >= MIN_RANGE
        val downThreshold = if (calibrated) minSeen + range * 0.4f else configDown
        val upThreshold = if (calibrated) minSeen + range * 0.6f else configUp

        when (phase) {
            RepPhase.UNKNOWN, RepPhase.UP -> {
                if (value <= downThreshold) {
                    phase = RepPhase.DOWN
                    downEnteredAt = now
                } else {
                    phase = RepPhase.UP
                }
            }
            RepPhase.DOWN -> {
                if (value >= upThreshold) {
                    val longEnough = now - downEnteredAt >= config.minRepMillis
                    val spaced = lastRepAt == 0L || now - lastRepAt >= config.minRepMillis
                    if (longEnough && spaced) {
                        reps++
                        lastRepAt = now
                    }
                    phase = RepPhase.UP
                }
            }
        }
        return phase
    }

    private companion object {
        /** Degrees of observed travel before auto-calibration takes over. */
        const val MIN_RANGE = 10f
    }

    /** True when the current pose is good enough to show green. */
    fun formIsValid(): Boolean = when (config.mode) {
        CoachMode.HOLD -> holding
        CoachMode.REPS -> phase != RepPhase.UNKNOWN
    }
}
