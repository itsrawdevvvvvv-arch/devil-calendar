package com.devil.calendar.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devil.calendar.service.island.IslandAction
import com.devil.calendar.service.island.IslandAppearance
import com.devil.calendar.service.island.IslandController
import com.devil.calendar.service.island.IslandKind
import com.devil.calendar.service.island.IslandMode
import com.devil.calendar.service.island.IslandSource
import com.devil.calendar.service.island.IslandState
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilTheme
import com.devil.calendar.ui.theme.LocalMotion
import com.devil.calendar.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * The island pill.
 *
 * The only colour that is not a theme token is [IslandCool]: the heat ring has
 * to start cold, and the app palette has no cold colour. Everything else comes
 * from DevilColors.
 */
private val IslandCool = Color(0xFF4A9EFF)

@Composable
fun IslandPillHost(
    controller: IslandController,
    appearance: StateFlow<IslandAppearance>,
) {
    val state by controller.state.collectAsState()
    val look by appearance.collectAsState()
    DevilTheme(darkTheme = true) {
        IslandPill(
            state = state,
            appearance = look,
            onTap = { controller.toggleExpanded() },
            onNext = { controller.next() },
            onPrevious = { controller.previous() },
            onAction = { sourceId, actionId -> controller.performAction(sourceId, actionId) },
        )
    }
}

@Composable
fun IslandPill(
    state: IslandState,
    appearance: IslandAppearance,
    onTap: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onAction: (String, String) -> Unit = { _, _ -> },
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val mode = state.mode
    val source = state.current

    fun tick() {
        if (appearance.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val scale = appearance.scale
    val width = when (mode) {
        IslandMode.IDLE -> 62.dp
        IslandMode.COMPACT -> if (state.hasMultiple) 236.dp else 208.dp
        IslandMode.EXPANDED -> 312.dp
    } * scale
    val height = when (mode) {
        IslandMode.IDLE -> 26.dp
        IslandMode.COMPACT -> 36.dp
        IslandMode.EXPANDED -> 156.dp
    } * scale
    val corner = when (mode) {
        IslandMode.IDLE -> 13.dp
        IslandMode.COMPACT -> 18.dp
        IslandMode.EXPANDED -> appearance.cornerDp.dp
    }

    // Jelly: low damping so the size overshoots slightly before settling.
    val jelly = motion.springOf<androidx.compose.ui.unit.Dp>(damping = 0.42f)
    val animatedWidth by animateDpAsState(width, jelly, label = "islandW")
    val animatedHeight by animateDpAsState(height, jelly, label = "islandH")
    val animatedCorner by animateDpAsState(corner, jelly, label = "islandR")

    // Squash and stretch: the pill stretches vertically as it grows and squashes
    // as it shrinks, always counter-phased so volume feels preserved.
    val stretchTarget = when (mode) {
        IslandMode.IDLE -> 1f
        IslandMode.COMPACT -> 1.04f
        IslandMode.EXPANDED -> 1.06f
    }
    val squashY by animateFloatAsState(stretchTarget, motion.springOf(0.4f), label = "squashY")
    val squashX by animateFloatAsState(2f - stretchTarget, motion.springOf(0.4f), label = "squashX")

    // Wobble on touch and a small hop when a new source takes over.
    val wobble = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(source?.id) {
        if (source != null && motion.allowDecorative) {
            hop.snapTo(1f)
            hop.animateTo(0f, tween(motion.duration(320), easing = Motion.easeOut))
        }
    }

    val shape = RoundedCornerShape(animatedCorner)

    Box(
        modifier = Modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .graphicsLayer {
                scaleX = squashX
                scaleY = squashY
                translationX = wobble.value
                translationY = -hop.value * 6.dp.toPx()
            }
            .shadow(elevation = 12.dp, shape = shape, clip = false)
            .clip(shape)
            .background(DevilColors.Ink)
            // Rim light: a thin gradient edge that catches the "light" from the
            // top-left, so the pill separates from any wallpaper.
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.06f),
                            DevilColors.RedText.copy(alpha = 0.22f),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        animatedCorner.toPx(),
                        animatedCorner.toPx(),
                    ),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    tick()
                    onTap()
                    if (motion.allowDecorative) {
                        // A quick left-right wobble, never a full shake.
                        wobble.animateTo(0f, tween(1))
                        wobble.animateTo(3f, tween(70))
                        wobble.animateTo(-2f, tween(70))
                        wobble.animateTo(0f, tween(90))
                    }
                })
            }
            .pointerInput(state.index) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragEnd = { dragged = 0f },
                    onHorizontalDrag = { _, amount ->
                        dragged += amount
                        if (dragged > SWIPE_THRESHOLD) {
                            tick()
                            onPrevious()
                            dragged = 0f
                        } else if (dragged < -SWIPE_THRESHOLD) {
                            tick()
                            onNext()
                            dragged = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Glow(source = source, modifier = Modifier.matchParentSize())

        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(motion.tweenOf(140)) togetherWith fadeOut(motion.tweenOf(100)) },
            label = "islandContent",
        ) { target ->
            when (target) {
                IslandMode.IDLE -> IdleDot()
                IslandMode.COMPACT -> source?.let { CompactPill(it, state.hasMultiple) }
                IslandMode.EXPANDED -> source?.let { ExpandedPill(it, state.hasMultiple, onAction) }
            }
        }
    }
}

/** Soft coloured halo behind the pill, tinted by the source. */
@Composable
private fun Glow(source: IslandSource?, modifier: Modifier = Modifier) {
    val tint = when (source?.kind) {
        IslandKind.REST, IslandKind.TIMER -> DevilColors.RedText
        IslandKind.CALL -> DevilColors.Red
        IslandKind.MUSIC -> DevilColors.Yellow
        else -> DevilColors.White
    }
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.15f),
                    radius = size.maxDimension * 0.75f,
                ),
            )
        },
    )
}

@Composable
private fun IdleDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Red),
    )
}

@Composable
private fun CompactPill(source: IslandSource, hasMultiple: Boolean) {
    val live = liveValue(source)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(source.kind),
            contentDescription = null,
            tint = tintFor(source.kind),
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = source.title,
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (hasMultiple) {
            // Split pill: a small divider plus dots shows more sources are queued.
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(1.dp)
                    .height(14.dp)
                    .background(DevilColors.CardBorder),
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DevilColors.Yellow),
            )
            Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = live,
            color = DevilColors.White,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun ExpandedPill(
    source: IslandSource,
    hasMultiple: Boolean,
    onAction: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = iconFor(source.kind),
                contentDescription = null,
                tint = tintFor(source.kind),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = source.title,
                color = DevilColors.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (hasMultiple) {
                Text(
                    text = "swipe",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        when (source.kind) {
            IslandKind.MUSIC -> MusicBody(source)
            IslandKind.REST, IslandKind.TIMER -> RingBody(source)
            else -> PlainBody(source)
        }

        if (source.actions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                source.actions.forEach { action ->
                    ActionChip(action) { onAction(source.id, action.id) }
                }
            }
        }
    }
}

/** Rest and timer: a heat ring that runs cool to ember as time drains. */
@Composable
private fun RingBody(source: IslandSource) {
    val remaining = remainingMillis(source)
    val total = remember(source.id) { remaining.coerceAtLeast(1L) }
    val fraction = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val heat = 1f - fraction
    val ringColour = androidx.compose.ui.graphics.lerp(IslandCool, DevilColors.RedText, heat)

    val pulse by animateFloatAsState(
        targetValue = if (fraction <= 0.08f) 1.08f else 1f,
        animationSpec = LocalMotion.current.springOf(),
        label = "heatPulse",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .drawBehind {
                    val stroke = 5.dp.toPx()
                    drawArc(
                        color = DevilColors.RingTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = ringColour,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatMillis(remaining),
                color = DevilColors.White,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.subtitle.ifBlank { source.title },
                color = DevilColors.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (fraction <= 0f) "Rest over" else "Resting",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Music: four bars that breathe while something is playing, plus a scrubber. */
@Composable
private fun MusicBody(source: IslandSource) {
    val motion = LocalMotion.current
    val bars = if (motion.allowDecorative) {
        val transition = rememberInfiniteTransition(label = "eq")
        (0..3).map { index ->
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420 + index * 90, easing = Motion.easeInOut),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$index",
            ).value
        }
    } else {
        listOf(0.4f, 0.4f, 0.4f, 0.4f)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.height(28.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            bars.forEach { value ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(value.coerceIn(0.15f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(DevilColors.Yellow),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.subtitle.ifBlank { source.title },
                color = DevilColors.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            source.progress?.let { progress ->
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DevilColors.RingTrack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(DevilColors.Yellow),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainBody(source: IslandSource) {
    Column {
        Text(
            text = liveValue(source),
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 1,
        )
        source.subtitle.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        source.progress?.let { progress ->
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DevilColors.RingTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(DevilColors.RedText),
                )
            }
        }
    }
}

@Composable
private fun ActionChip(action: IslandAction, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Card)
            .pointerInput(action.id) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = action.label,
            color = DevilColors.White,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun remainingMillis(source: IslandSource): Long {
    val end = source.endMillis ?: return 0L
    return (end - System.currentTimeMillis()).coerceAtLeast(0L)
}

private fun formatMillis(millis: Long): String =
    "%d:%02d".format(millis / 60_000L, (millis / 1_000L) % 60)

/** Ticks only while a countdown is running. */
@Composable
private fun liveValue(source: IslandSource): String {
    val end = source.endMillis
    if (end == null) return source.value
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    return formatMillis((end - now).coerceAtLeast(0L))
}

private fun iconFor(kind: IslandKind): ImageVector = when (kind) {
    IslandKind.CALL -> Icons.Rounded.Call
    IslandKind.TIMER -> Icons.Rounded.Timer
    IslandKind.REST -> Icons.Rounded.FitnessCenter
    IslandKind.MUSIC -> Icons.Rounded.MusicNote
    IslandKind.OTHER -> Icons.Rounded.Notifications
}

private fun tintFor(kind: IslandKind): Color = when (kind) {
    IslandKind.CALL -> DevilColors.Red
    IslandKind.TIMER, IslandKind.REST -> DevilColors.RedText
    IslandKind.MUSIC -> DevilColors.Yellow
    IslandKind.OTHER -> DevilColors.Muted
}

private const val SWIPE_THRESHOLD = 48f
