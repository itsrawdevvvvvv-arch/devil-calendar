package com.devil.calendar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion
import com.devil.calendar.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ember particles thrown outward when a set is ticked off.
 *
 * Everything is drawn in one Canvas pass from pre-computed angles, so there are
 * no allocations inside the draw scope and no layout animation - the burst is
 * pure GPU work.
 */
@Composable
fun EmberBurst(
    /** Increment to replay. Zero never fires. */
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    if (motion.reduced) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(Motion.Workout.emberBurstMs, easing = Motion.easeOut),
        )
    }

    val value = progress.value
    if (value <= 0f || value >= 1f) return

    Canvas(modifier = modifier) {
        val count = Motion.Workout.emberParticles
        val radiusBase = 3.dp.toPx()
        val travel = size.minDimension * 0.42f * value
        for (i in 0 until count) {
            val angle = (i * 2.0 * PI / count).toFloat()
            val centre = Offset(
                x = size.width / 2f + cos(angle) * travel,
                // Embers drift upward as they fade.
                y = size.height / 2f + sin(angle) * travel - 18.dp.toPx() * value,
            )
            drawCircle(
                color = if (i % 3 == 0) DevilColors.Yellow else DevilColors.RedText,
                radius = radiusBase * (1f - value * 0.6f),
                center = centre,
                alpha = (1f - value).coerceIn(0f, 1f),
            )
        }
    }
}

/** A single soft flare used for the record celebration. */
@Composable
fun RingFlare(progress: Float, modifier: Modifier = Modifier, color: Color = DevilColors.Yellow) {
    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.5f * progress
        drawCircle(
            color = color,
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            alpha = (1f - progress).coerceIn(0f, 1f),
        )
    }
}
