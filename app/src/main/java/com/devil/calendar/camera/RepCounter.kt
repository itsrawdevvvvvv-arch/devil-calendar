package com.devil.calendar.camera

import com.devil.calendar.domain.coach.CoachConfig
import com.devil.calendar.domain.coach.RepPhase

/**
 * Adaptive peak-valley rep counter.
 *
 * Fixed angle thresholds cannot work: the same push-up produces different
 * numbers head-on and side-on, and different numbers again for a shorter or
 * taller person. So the range is learned from the signal itself - running 10th
 * and 90th percentiles over the last few seconds - and the thresholds ride
 * inside that range.
 *
 * Deliberate choices:
 * - A dropped landmark never resets the state. Short gaps are held, not zeroed.
 * - Single-frame spikes larger than [SPIKE] are ignored outright.
 * - Shallow reps (under 60% of range) count but are flagged so the UI can say
 *   "go lower" instead of silently swallowing them.
 */
class RepCounter(private val config: CoachConfig) {

    private val window = ArrayDeque<Float>()
    private var previous: Float? = null
    private var medianBuffer = ArrayDeque<Float>()

    private var phase: RepPhase = RepPhase.UNKNOWN
    private var phaseStartedAt = 0L
    private var lastRepAt = 0L
    private var peak = -Float.MAX_VALUE
    // The widest range seen so far, decaying slowly. Without this a run of
    // shallow reps would drag the calibrated range down until they looked full.
    private var referenceRange = 0f

    var reps: Int = 0
        private set
    var shallow: Int = 0
        private set
    var lastRepWasShallow: Boolean = false
        private set
    var holdSeconds: Int = 0
        private set

    private var holding = false
    private var holdStartedAt = 0L

    /** Current calibrated thresholds, for the debug overlay. */
    var lowThreshold: Float = 0f
        private set
    var highThreshold: Float = 0f
        private set

    val calibrated: Boolean get() = window.size >= MIN_SAMPLES

    fun reset() {
        window.clear()
        medianBuffer.clear()
        previous = null
        phase = RepPhase.UNKNOWN
        phaseStartedAt = 0L
        lastRepAt = 0L
        peak = -Float.MAX_VALUE
        referenceRange = 0f
        reps = 0
        shallow = 0
        lastRepWasShallow = false
        holdSeconds = 0
        holding = false
        holdStartedAt = 0L
    }

    fun adjust(delta: Int) {
        reps = (reps + delta).coerceAtLeast(0)
    }

    /**
     * Feeds one signal sample. Returns the phase. [nowMillis] must be monotonic.
     * Returns early without changing state when the sample is a spike.
     */
    fun onSignal(raw: Float, nowMillis: Long): RepPhase {
        val last = previous
        if (last != null && kotlin.math.abs(raw - last) > SPIKE) return phase

        // Three-frame median kills single-frame jumps without adding lag.
        medianBuffer.addLast(raw)
        if (medianBuffer.size > 3) medianBuffer.removeFirst()
        val value = PoseMath.median3(
            if (medianBuffer.size > 2) medianBuffer.elementAt(0) else null,
            medianBuffer.elementAt(medianBuffer.size / 2),
            if (medianBuffer.size > 2) medianBuffer.elementAt(2) else null,
        )
        previous = value

        if (config.mode == com.devil.calendar.domain.coach.CoachMode.HOLD) {
            val valid = value > HOLD_MIN
            if (valid) {
                if (!holding) {
                    holding = true
                    holdStartedAt = nowMillis
                }
                holdSeconds = ((nowMillis - holdStartedAt) / 1000L).toInt()
            } else {
                holding = false
            }
            phase = if (valid) RepPhase.UP else RepPhase.DOWN
            return phase
        }

        window.addLast(value)
        while (window.size > WINDOW) window.removeFirst()
        if (!calibrated) return phase

        val samples = window.toList()
        val min = PoseMath.percentile(samples, 0.10f)
        val max = PoseMath.percentile(samples, 0.90f)
        val range = max - min
        if (range < MIN_RANGE) return phase

        // The signal grows with depth, so "down" is the HIGH end.
        // Entry is deliberately lenient (40% of range) so a shallow rep still
        // registers; whether it was deep enough only decides the "go lower" flag.
        referenceRange = maxOf(referenceRange, range)
        lowThreshold = min + range * EXIT_FRACTION
        highThreshold = min + range * ENTER_FRACTION

        when (phase) {
            RepPhase.UNKNOWN, RepPhase.UP -> {
                if (value >= highThreshold && nowMillis - phaseStartedAt >= MIN_PHASE_MS) {
                    phase = RepPhase.DOWN
                    phaseStartedAt = nowMillis
                    peak = value
                } else if (phase == RepPhase.UNKNOWN) {
                    phase = RepPhase.UP
                    phaseStartedAt = nowMillis
                }
            }
            RepPhase.DOWN -> {
                peak = maxOf(peak, value)
                if (value <= lowThreshold && nowMillis - phaseStartedAt >= MIN_PHASE_MS) {
                    val travelled = peak - min
                    val deepEnough = travelled >= referenceRange * 0.6f
                    val spaced = lastRepAt == 0L || nowMillis - lastRepAt >= config.minRepMillis
                    if (spaced) {
                        reps++
                        lastRepAt = nowMillis
                        lastRepWasShallow = !deepEnough
                        if (!deepEnough) shallow++
                        // Let the reference relax so a genuinely easier
                        // variation is not flagged forever.
                        referenceRange *= 0.995f
                    }
                    phase = RepPhase.UP
                    phaseStartedAt = nowMillis
                }
            }
        }
        return phase
    }

    private companion object {
        const val WINDOW = 90          // about 3 seconds at 30 fps
        const val MIN_SAMPLES = 15
        const val MIN_RANGE = 0.04f
        const val SPIKE = 2.5f
        const val MIN_PHASE_MS = 150L
        const val HOLD_MIN = 0.15f
        const val ENTER_FRACTION = 0.40f
        const val EXIT_FRACTION = 0.25f
    }
}
