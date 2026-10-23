package com.devil.calendar.ui.takeover

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devil.calendar.service.AlertController
import com.devil.calendar.service.AlertKind
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)

private fun iconFor(kind: AlertKind): ImageVector = when (kind) {
    AlertKind.TIMER -> Icons.Rounded.Timer
    AlertKind.ALARM -> Icons.Rounded.Notifications
    AlertKind.FOCUS -> Icons.Rounded.CenterFocusStrong
}

/**
 * The full-screen takeover. Rendered by both
 * [com.devil.calendar.TakeoverActivity] and the window-manager overlay, driven
 * entirely by [AlertController.AlertState]. It never plays sound or vibrates -
 * AlertController owns both.
 */
@Composable
fun TakeoverScreen(
    state: AlertController.AlertState,
    onDismiss: () -> Unit,
    onSnooze: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** Insets for the inner content only; the background stays full-bleed. */
    contentInsets: PaddingValues = PaddingValues(0.dp),
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current

    var pressing by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    var progress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var milestone by remember { mutableIntStateOf(0) }

    // Entrance: 1.04 -> 1 with a fade over 350ms.
    val entered = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { entered.animateTo(1f, motion.tweenOf(350)) }

    // Exit: fade out 250ms, then hand back to the caller.
    val exitAlpha by animateFloatAsState(
        targetValue = if (dismissing) 0f else 1f,
        animationSpec = motion.tweenOf(250),
        label = "exit",
    )
    LaunchedEffect(dismissing) {
        if (dismissing) {
            kotlinx.coroutines.delay(250)
            onDismiss()
        }
    }

    // Hold to dismiss: fills over 1.2s, ticks at 25/50/75%, springs back on release.
    LaunchedEffect(pressing) {
        if (pressing) {
            progress = 0f
            milestone = 0
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val p = ((now - start) / 1_200_000_000f).coerceIn(0f, 1f)
                progress = p
                val reached = (p * 4f).toInt()
                if (reached > milestone) {
                    milestone = reached
                    if (reached in 1..3) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                if (p >= 1f) {
                    dismissing = true
                    onDismiss()
                    break
                }
            }
        } else if (progress > 0f) {
            val from = progress
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val t = ((now - start) / 200_000_000f).coerceIn(0f, 1f)
                progress = from * (1f - t)
                if (t >= 1f) break
            }
            progress = 0f
        }
    }

    // Three rings pulsing outward, staggered a third of a cycle apart.
    val rings = rememberInfiniteTransition(label = "rings")
    val ringT by rings.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringT",
    )
    val breathe by rings.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val enterScale = 1.04f - 0.04f * entered.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // The only gradient in the app: a subtle edge vignette.
                Brush.radialGradient(
                    colors = listOf(
                        DevilColors.TakeoverRed,
                        DevilColors.TakeoverRed,
                        DevilColors.VignetteEdge,
                    ),
                ),
            )
            .graphicsLayer {
                scaleX = enterScale
                scaleY = enterScale
                alpha = entered.value * exitAlpha
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentInsets)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.kind.shortLabel,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = LocalDate.now().format(dateFormat),
                    color = DevilColors.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2f
                    for (i in 0..2) {
                        val p = (ringT + i / 3f) % 1f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f * (1f - p)),
                            radius = maxRadius * p,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                            ),
                        )
                    }
                }
                Icon(
                    imageVector = iconFor(state.kind),
                    contentDescription = null,
                    tint = DevilColors.White,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(breathe),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = state.headline,
                color = DevilColors.White,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )
            if (state.subtitle.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.subtitle,
                    color = DevilColors.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))

            if (!state.goal.isNullOrBlank()) {
                Text(
                    text = "Your goal: ${state.goal}",
                    color = DevilColors.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
            }

            if (state.showSnooze && onSnooze != null) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .border(2.dp, DevilColors.White.copy(alpha = 0.7f), RoundedCornerShape(30.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onSnooze() })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "SNOOZE 5 MIN",
                        color = DevilColors.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .border(2.dp, DevilColors.White, RoundedCornerShape(34.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressing = true
                                tryAwaitRelease()
                                pressing = false
                            },
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(DevilColors.White.copy(alpha = 0.30f)),
                )
                Text(
                    text = "HOLD TO DISMISS",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
