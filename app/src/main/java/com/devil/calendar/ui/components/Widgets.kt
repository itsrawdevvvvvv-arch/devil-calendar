package com.devil.calendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.theme.LocalMotion
import kotlinx.coroutines.delay

/** Day / Week / Month control with a thumb that slides between options. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val shape = RoundedCornerShape(40.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DevilColors.Grey)
            .padding(4.dp),
    ) {
        val itemWidth = maxWidth / options.size
        val thumbX by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = motion.springOf(),
            label = "segThumb",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .width(itemWidth)
                .height(52.dp)
                .clip(shape)
                .background(DevilColors.White),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val active = index == selectedIndex
                val fg by animateColorAsState(
                    targetValue = if (active) DevilColors.Ink else DevilColors.White,
                    animationSpec = motion.tuneColor(200),
                    label = "segFg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .pressFeedback(onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = fg, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** A black card with a small label and a big counting-up number. */
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

/** Seven vertical bars, Monday..Sunday, growing with a staggered spring. */
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
            val grow = remember { Animatable(target) }
            LaunchedEffect(crossed) {
                if (motion.reduced) {
                    grow.snapTo(target)
                } else {
                    delay(index * 40L)
                    grow.animateTo(target, motion.springOf())
                }
            }
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
                        .fillMaxHeight(grow.value.coerceIn(0.02f, 1f))
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
 * The countdown ring. Progress is a one-second linear tween so the sweep is
 * continuous, and it is drawn on a single Canvas.
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
        animationSpec = if (motion.reduced) tween(0) else tween(1000, easing = androidx.compose.animation.core.LinearEasing),
        label = "ring",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val inset = strokeWidth / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            rotate(-90f) {
                drawArc(
                    color = progressColor,
                    startAngle = 0f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
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
    height: Dp = 60.dp,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(DevilRadius.button))
            .background(background.copy(alpha = background.alpha * alpha))
            .pressFeedback(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor.copy(alpha = textColor.alpha * alpha), style = MaterialTheme.typography.labelLarge)
    }
}
