package com.devil.calendar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.devil.calendar.ui.theme.LocalMotion

/**
 * Cheap entrance animation: fade + rise, staggered by [index].
 *
 * It animates through [graphicsLayer], which runs on the render thread and
 * skips recomposition and layout — the reason this stays smooth on low-end
 * devices. On a memory-constrained device the stagger is dropped entirely.
 */
@Composable
fun Modifier.animatedEntrance(
    index: Int = 0,
    rise: Float = 28f,
): Modifier {
    val motion = LocalMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val delay = motion.stagger(index)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = (motion.scale * 280).toInt(), delayMillis = delay),
        label = "entranceAlpha",
    )
    val offset by animateFloatAsState(
        targetValue = if (visible) 0f else rise,
        animationSpec = tween(durationMillis = (motion.scale * 300).toInt(), delayMillis = delay),
        label = "entranceOffset",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = offset
    }
}
