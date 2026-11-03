package com.devil.calendar.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fades the top and bottom of a scrolling container to transparent so content
 * slides under the status bar and the floating bars instead of being clipped.
 *
 * The gradient is painted with [BlendMode.DstIn], which erases alpha where the
 * gradient is transparent - so it works over any background, including a
 * wallpaper or a blurred layer, without knowing the colour behind it.
 *
 * [CompositingStrategy.Offscreen] is required for DstIn to work against the
 * layer's own content rather than the window.
 */
fun Modifier.edgeFade(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
): Modifier {
    if (top <= 0.dp && bottom <= 0.dp) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (top > 0.dp) {
                val h = top.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black,
                        startY = 0f,
                        endY = h,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (bottom > 0.dp) {
                val h = bottom.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        1f to Color.Transparent,
                        startY = size.height - h,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}
