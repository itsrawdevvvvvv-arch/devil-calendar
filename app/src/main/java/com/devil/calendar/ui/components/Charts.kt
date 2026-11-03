package com.devil.calendar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion

/**
 * Hand-drawn charts: no charting dependency, and every mark is a Canvas call so
 * nothing allocates per frame beyond the path itself.
 */

/** A line of [points] normalised to the chart height, with a dot per session. */
@Composable
fun ProgressLineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    color: androidx.compose.ui.graphics.Color = DevilColors.RedText,
) {
    val motion = LocalMotion.current
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = motion.tweenOf(500),
        label = "lineReveal",
    )
    if (points.isEmpty()) {
        EmptyChart(height = height, text = "No sessions yet", modifier = modifier)
        return
    }
    val maxValue = points.max().coerceAtLeast(1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val stroke = 3.dp.toPx()
        val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = if (points.size > 1) stepX * index else size.width / 2f
            val y = size.height - (value / maxValue) * (size.height - stroke * 2) - stroke
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        points.forEachIndexed { index, value ->
            val x = if (points.size > 1) stepX * index else size.width / 2f
            val y = size.height - (value / maxValue) * (size.height - stroke * 2) - stroke
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

/** Vertical bars for the last seven days. */
@Composable
fun VolumeBarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
) {
    val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(18.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fraction = (value / maxValue).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (value > 0f) DevilColors.Yellow else DevilColors.RingTrack),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** One labelled horizontal bar, used for volume per muscle group. */
@Composable
fun HorizontalBar(
    label: String,
    valueText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = DevilColors.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DevilColors.RingTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(DevilColors.RedText),
            )
        }
    }
}

@Composable
private fun EmptyChart(height: Dp, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(DevilColors.Ink)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = DevilColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}
