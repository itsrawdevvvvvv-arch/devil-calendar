package com.devil.calendar.camera

import kotlin.math.acos
import kotlin.math.sqrt

/** Geometry helpers. Pure functions, so they are unit-testable without a camera. */
object PoseMath {

    /**
     * Angle at [b] formed by a-b-c, in degrees. Returns null when any point is
     * missing or the three points are coincident.
     */
    fun angle(
        ax: Float, ay: Float,
        bx: Float, by: Float,
        cx: Float, cy: Float,
    ): Float? {
        val abx = ax - bx
        val aby = ay - by
        val cbx = cx - bx
        val cby = cy - by
        val magAb = sqrt(abx * abx + aby * aby)
        val magCb = sqrt(cbx * cbx + cby * cby)
        if (magAb < 1e-3f || magCb < 1e-3f) return null
        val cosine = ((abx * cbx + aby * cby) / (magAb * magCb)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosine).toDouble()).toFloat()
    }

    /** Exponential smoothing: higher [alpha] follows the raw value faster. */
    fun smooth(previous: Float?, raw: Float, alpha: Float = 0.35f): Float =
        if (previous == null) raw else previous + (raw - previous) * alpha
}
