package com.devil.calendar.camera

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** Geometry and filtering. Pure functions, unit-testable without a camera. */
object PoseMath {

    fun angle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float? {
        val abx = ax - bx
        val aby = ay - by
        val cbx = cx - bx
        val cby = cy - by
        val magAb = sqrt(abx * abx + aby * aby)
        val magCb = sqrt(cbx * cbx + cby * cby)
        if (magAb < 1e-4f || magCb < 1e-4f) return null
        val cosine = ((abx * cbx + aby * cby) / (magAb * magCb)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosine).toDouble()).toFloat()
    }

    /** Median of up to three samples: kills single-frame spikes without lag. */
    fun median3(a: Float?, b: Float, c: Float?): Float {
        if (a == null) return if (c == null) b else (b + c) / 2f
        if (c == null) return (a + b) / 2f
        return maxOf(minOf(a, b), minOf(maxOf(a, b), c))
    }

    /**
     * One-Euro filter: low jitter when still, low lag when moving. This is what
     * replaces the heavy EMA, which lagged behind fast reps.
     */
    class OneEuro(private val minCutoff: Float = 1.6f, private val beta: Float = 0.05f) {
        private var value: Float? = null
        private var lastTime = 0L

        fun filter(next: Float, nowMillis: Long): Float {
            val previous = value
            val previousTime = lastTime
            value = next
            lastTime = nowMillis
            if (previous == null || previousTime == 0L) return next
            val dt = ((nowMillis - previousTime).coerceIn(1L, 200L)) / 1000f
            val speed = abs(next - previous) / dt
            val cutoff = minCutoff + beta * speed
            val alpha = 1f / (1f + 1f / (2f * Math.PI.toFloat() * cutoff * dt))
            return previous + (next - previous) * alpha
        }

        fun reset() {
            value = null
            lastTime = 0L
        }
    }

    /** 10th and 90th percentile of a sample, by nearest rank. */
    fun percentile(samples: List<Float>, fraction: Float): Float {
        if (samples.isEmpty()) return 0f
        val sorted = samples.sorted()
        val index = ((sorted.size - 1) * fraction).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }
}
