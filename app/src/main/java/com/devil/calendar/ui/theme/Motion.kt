package com.devil.calendar.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
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
 * One motion language for the whole app. Short springs and tweens only, so the
 * app holds frame rate on low-end hardware.
 */
object Motion {
    val easeOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val easeInOut: Easing = FastOutSlowInEasing
    val linear: Easing = LinearEasing

    /** Default spring for taps, snaps and small movements. */
    fun <T> springSpec(
        damping: Float = 0.75f,
        stiffness: Float = Spring.StiffnessMediumLow,
    ): AnimationSpec<T> = spring(dampingRatio = damping, stiffness = stiffness)

    /** Stagger between siblings on first paint. */
    const val staggerMs = 60

    // Legacy float specs still used by a few call sites.
    val quick: FiniteAnimationSpec<Float> = tween(160, easing = easeOut)
    val standard: FiniteAnimationSpec<Float> = tween(260, easing = easeOut)
    val emphasized: FiniteAnimationSpec<Float> = tween(340, easing = easeOut)

    fun <T> pop(): AnimationSpec<T> = springSpec(damping = 0.75f)
    fun <T> settle(): AnimationSpec<T> = springSpec(damping = 1f, stiffness = Spring.StiffnessMedium)
}

/**
 * Motion budget. Shortens everything on a low-RAM device and, when the user has
 * turned animations off system-wide, snaps instead of animating.
 */
@Immutable
data class MotionConfig(
    val scale: Float,
    val allowStagger: Boolean,
    val allowDecorative: Boolean,
    val reduced: Boolean = false,
) {
    fun duration(baseMs: Int): Int =
        if (reduced) 0 else (baseMs * scale).toInt().coerceAtLeast(1)

    fun <T> tweenOf(baseMs: Int, easing: Easing = Motion.easeOut): FiniteAnimationSpec<T> =
        tween(duration(baseMs), easing = if (reduced) Motion.linear else easing)

    fun <T> springOf(
        damping: Float = 0.75f,
        stiffness: Float = Spring.StiffnessMediumLow,
    ): AnimationSpec<T> =
        if (reduced) tween(0, easing = Motion.linear) else Motion.springSpec(damping, stiffness)

    fun <T> tune(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> {
        if (reduced) return tween(0, easing = Motion.linear)
        if (scale >= 1f) return spec
        return tween(duration(durationOf(spec)), easing = Motion.easeOut)
    }

    /** Same budget, for value types that are not Float (e.g. Color). */
    fun <T> tuneColor(baseMs: Int = 160): FiniteAnimationSpec<T> = tweenOf(baseMs)

    fun stagger(index: Int): Int =
        if (allowStagger && !reduced) index * Motion.staggerMs else 0
}

private fun durationOf(spec: FiniteAnimationSpec<*>): Int = when (spec) {
    is androidx.compose.animation.core.TweenSpec<*> -> spec.durationMillis
    else -> 260
}

fun isLowEndDevice(context: Context): Boolean = runCatching {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    am.isLowRamDevice
}.getOrDefault(false)

/** True when the user has disabled animations system-wide. */
fun isReducedMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}.getOrDefault(false)

@Composable
fun rememberMotionConfig(): MotionConfig {
    val context = LocalContext.current
    return remember(context) {
        val reduced = isReducedMotion(context)
        val low = isLowEndDevice(context)
        when {
            reduced -> MotionConfig(scale = 1f, allowStagger = false, allowDecorative = false, reduced = true)
            low -> MotionConfig(scale = 0.7f, allowStagger = false, allowDecorative = false)
            else -> MotionConfig(scale = 1f, allowStagger = true, allowDecorative = true)
        }
    }
}
