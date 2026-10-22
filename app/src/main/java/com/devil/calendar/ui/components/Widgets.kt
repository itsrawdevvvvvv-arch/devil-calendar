package com.devil.calendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.theme.LocalMotion
import com.devil.calendar.ui.theme.Motion

/** Day / Week / Month control. Active option is a white pill in a grey track. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(DevilColors.Grey)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (active) DevilColors.White else DevilColors.Ink,
                animationSpec = motion.tuneColor(160),
                label = "segBg",
            )
            val fg by animateColorAsState(
                targetValue = if (active) DevilColors.Ink else DevilColors.White,
                animationSpec = motion.tuneColor(160),
                label = "segFg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(bg)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** A black card with a small label and a big animated number. */
@Composable
fun StatCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = Motion.settle(),
        label = "statValue",
    )
    BentoCard(modifier = modifier.height(130.dp), color = DevilColors.Ink) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = DevilColors.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$animated", color = DevilColors.White, style = MaterialTheme.typography.displaySmall)
                if (suffix.isNotEmpty()) {
                    Text(
                        suffix,
                        color = DevilColors.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** Seven vertical bars, Monday..Sunday. Crossed days are full-height ink bars. */
@Composable
fun BarsChart(
    bars: List<Boolean>,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    Row(
        modifier = modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { index, crossed ->
            val target = if (crossed) 1f else 0.30f
            val fraction by animateFloatAsState(
                targetValue = target,
                animationSpec = motion.tune(Motion.emphasized),
                label = "bar$index",
            )
            val color by animateColorAsState(
                targetValue = if (crossed) DevilColors.Ink else DevilColors.White.copy(alpha = 0.55f),
                animationSpec = motion.tuneColor(260),
                label = "barColor$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(color),
                )
            }
        }
    }
}

@Composable
fun WeekdayRow(modifier: Modifier = Modifier, color: Color = DevilColors.Ink) {
    Row(modifier = modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
            Text(
                text = it,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The countdown ring. Drawn on a single Canvas; progress is animated so the
 * sweep is smooth without recomposing the whole screen each frame.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 14f,
    trackColor: Color = DevilColors.RingTrack,
    progressColor: Color = DevilColors.RedText,
    content: @Composable () -> Unit,
) {
    val motion = LocalMotion.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = motion.tune(Motion.quick),
        label = "ring",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val inset = strokeWidth / 2f
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                style = stroke,
            )
            rotate(-90f) {
                drawArc(
                    color = progressColor,
                    startAngle = 0f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = stroke,
                )
            }
        }
        content()
    }
}

/** A full-width pill button. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = DevilColors.Yellow,
    textColor: Color = DevilColors.Ink,
    height: androidx.compose.ui.unit.Dp = 60.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(DevilRadius.button))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}
