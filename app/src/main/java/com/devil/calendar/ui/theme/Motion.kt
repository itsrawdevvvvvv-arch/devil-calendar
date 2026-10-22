package com.devil.calendar.ui.theme

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * A single, small motion language used everywhere. Everything is a spring or a
 * short tween — no long chained animations — so the whole app stays at frame
 * rate on low-end hardware.
 */
object Motion {
    val easeOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val easeInOut: Easing = FastOutSlowInEasing
    val linear: Easing = LinearEasing

    /** Tap feedback, colour fades, small shifts. */
    val quick: FiniteAnimationSpec<Float> = tween(160, easing = easeOut)

    /** Screen and card entrances. */
    val standard: FiniteAnimationSpec<Float> = tween(260, easing = easeOut)

    /** Emphasis: hero numbers, takeover reveal. */
    val emphasized: FiniteAnimationSpec<Float> = tween(340, easing = easeOut)

    /** Playful but cheap — settles fast, no long wobble. */
    fun <T> pop(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> settle(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Staggered list/card entrances: index * this many ms. */
    const val staggerMs = 45
}

/**
 * Motion budget. On a low-RAM / low-end device we shorten durations, drop the
 * stagger and skip the decorative (non-essential) motion so the app stays
 * fluid. Behavioural feedback (a day crossing out) is always kept.
 */
@Immutable
data class MotionConfig(
    val scale: Float,
    val allowStagger: Boolean,
    val allowDecorative: Boolean,
) {
    fun <T> tune(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> {
        if (scale >= 1f) return spec
        val ms = (durationOf(spec) * scale).toInt().coerceAtLeast(90)
        return tween(ms, easing = Motion.easeOut)
    }

    /** Same budget, for value types that are not Float (e.g. Color). */
    fun <T> tuneColor(baseMs: Int = 160): FiniteAnimationSpec<T> =
        tween((baseMs * scale).toInt().coerceAtLeast(90), easing = Motion.easeOut)

    fun stagger(index: Int): Int =
        if (allowStagger) (index * Motion.staggerMs) else 0
}

private fun durationOf(spec: FiniteAnimationSpec<*>): Int = when (spec) {
    is androidx.compose.animation.core.TweenSpec<*> -> spec.durationMillis
    else -> 260
}

/**
 * Reads whether the device is memory-constrained. `isLowRamDevice` is the
 * platform's own signal and is the cheapest reliable heuristic.
 */
fun isLowEndDevice(context: Context): Boolean = runCatching {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    am.isLowRamDevice
}.getOrDefault(false)

@Composable
fun rememberMotionConfig(): MotionConfig {
    val context = LocalContext.current
    return remember(context) {
        val low = isLowEndDevice(context)
        if (low) {
            MotionConfig(scale = 0.7f, allowStagger = false, allowDecorative = false)
        } else {
            MotionConfig(scale = 1f, allowStagger = true, allowDecorative = true)
        }
    }
}
