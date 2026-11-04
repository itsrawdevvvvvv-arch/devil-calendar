package com.devil.calendar.domain.walk

import kotlin.math.cos
import kotlin.math.sin

/**
 * Pedestrian dead reckoning: every step moves the position by the step length
 * along the current heading.
 *
 * Honest about what this is: heading comes from the rotation vector, which
 * drifts, and step length is an estimate from height. On a terrace the trail is
 * a shape, not a map. Error accumulates and never corrects itself without GPS.
 */
class DeadReckoning(
    private var stepLengthMetres: Double = 0.7,
) {
    private var x = 0.0
    private var y = 0.0
    private var headingDeg = 0.0
    private var hasHeading = false
    private val points = mutableListOf<Pair<Double, Double>>()

    val path: List<Pair<Double, Double>> get() = points

    fun setStepLength(metres: Double) {
        stepLengthMetres = metres.coerceIn(0.3, 1.2)
    }

    /** Circular low-pass on the heading, so it does not snap between samples. */
    fun onHeading(degrees: Double, alpha: Double = 0.15) {
        if (!hasHeading) {
            headingDeg = degrees
            hasHeading = true
            return
        }
        val current = Math.toRadians(headingDeg)
        val next = Math.toRadians(degrees)
        val sx = (1 - alpha) * sin(current) + alpha * sin(next)
        val sy = (1 - alpha) * cos(current) + alpha * cos(next)
        headingDeg = Math.toDegrees(kotlin.math.atan2(sx, sy))
    }

    fun onStep() {
        val rad = Math.toRadians(headingDeg)
        x += stepLengthMetres * sin(rad)
        y += stepLengthMetres * cos(rad)
        points += x to y
    }

    fun reset() {
        x = 0.0
        y = 0.0
        points.clear()
        hasHeading = false
    }

    /** Step length from height: roughly 0.415 x height. */
    companion object {
        fun stepLengthForHeight(heightCm: Int): Double =
            (heightCm * 0.415 / 100.0).coerceIn(0.3, 1.2)

        fun encode(points: List<Pair<Double, Double>>): String =
            points.joinToString(";") { "${"%.2f".format(it.first)},${"%.2f".format(it.second)}" }

        fun decode(raw: String): List<Pair<Double, Double>> =
            raw.split(';').mapNotNull { part ->
                val bits = part.split(',')
                if (bits.size != 2) return@mapNotNull null
                val px = bits[0].toDoubleOrNull() ?: return@mapNotNull null
                val py = bits[1].toDoubleOrNull() ?: return@mapNotNull null
                px to py
            }
    }
}
