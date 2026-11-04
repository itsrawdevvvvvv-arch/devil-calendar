package com.devil.calendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * The font size is fitted to the width instead of being fixed - a fixed 40sp ran
 * off both edges for long phrases, which is what made it look cropped.
 */
@Composable
fun LiquidWord(
    word: String,
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(height)) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val fitted = remember(word, maxWidthPx) {
            if (word.isBlank() || maxWidthPx <= 0f) {
                TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold)
            } else {
                var size = 46f
                var style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Bold)
                var measured = measurer.measure(word, style, maxLines = 1)
                while (measured.size.width > maxWidthPx * 0.94f && size > 12f) {
                    size -= 1.5f
                    style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Bold)
                    measured = measurer.measure(word, style, maxLines = 1)
                }
                style
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (word.isBlank()) return@Canvas
            val layout = measurer.measure(word, fitted, maxLines = 1)
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
            if (progress > 0f) {
                drawRect(
                    color = DevilColors.RedText,
                    topLeft = Offset(0f, level),
                    size = Size(size.width, 2.dp.toPx()),
                )
            }
        }
    }
}
