package com.devil.calendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devil.calendar.ui.theme.DevilColors
import kotlin.math.sin

/**
 * A word that fills with liquid from the bottom as progress rises.
 *
 * Two passes: the whole word in a dim tone, then the same word clipped to a
 * wavy surface in the accent colour. Cheaper than a real metaball and it reads
 * correctly at any size.
 */
@Composable
fun LiquidWord(
    word: String,
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (word.isBlank()) return@Canvas
        val layout = measurer.measure(word, style, maxLines = 1)
        val x = (size.width - layout.size.width) / 2f
        val y = (size.height - layout.size.height) / 2f

        drawText(textLayoutResult = layout, topLeft = Offset(x, y), color = DevilColors.CardBorder)

        val level = size.height * (1f - progress.coerceIn(0f, 1f))
        val surface = Path().apply {
            moveTo(0f, level)
            var px = 0f
            val amplitude = 3.dp.toPx()
            while (px <= size.width) {
                lineTo(px, level + sin((px / size.width) * 12.5f) * amplitude)
                px += 4f
            }
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        clipPath(surface) {
            drawText(textLayoutResult = layout, topLeft = Offset(x, y), color = DevilColors.Yellow)
        }
        // A bright line at the surface so the level is obvious.
        drawRect(
            color = DevilColors.RedText,
            topLeft = Offset(0f, level),
            size = Size(size.width, 2.dp.toPx()),
        )
    }
}
