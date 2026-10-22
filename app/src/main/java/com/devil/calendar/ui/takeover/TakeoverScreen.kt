package com.devil.calendar.ui.takeover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.Motion

/**
 * The full-screen takeover. Rendered by both [com.devil.calendar.TakeoverActivity]
 * and the window-manager overlay so the two layers look identical.
 *
 * @param label e.g. "TIME'S UP" / "ALARM" / "FOCUS SESSION COMPLETE"
 * @param onDismiss called once the hold completes
 */
@Composable
fun TakeoverScreen(
    label: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hold = remember { Animatable(0f) }
    var pressing by remember { mutableStateOf(false) }

    // Slow pulse on the flame so the screen feels alive without costing frames.
    val pulse = rememberInfiniteTransition(label = "pulse")
    val flameScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "flame",
    )

    LaunchedEffect(pressing) {
        if (pressing) {
            hold.snapTo(0f)
            hold.animateTo(1f, tween(1200, easing = LinearEasing))
            onDismiss()
        } else {
            hold.animateTo(0f, tween(180, easing = Motion.easeOut))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DevilColors.TakeoverRed),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "\uD83D\uDD25",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(64.dp)
                    .scale(flameScale),
                style = MaterialTheme.typography.displayMedium,
            )

            Text(
                text = "00:00",
                color = DevilColors.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.6f,
                ),
                textAlign = TextAlign.Center,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 28.dp),
                )

                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .border(2.dp, DevilColors.White, RoundedCornerShape(34.dp))
                        .then(
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        pressing = true
                                        tryAwaitRelease()
                                        pressing = false
                                    },
                                )
                            },
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Progress fill grows from the left while held.
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hold.value.coerceIn(0f, 1f))
                            .background(DevilColors.White.copy(alpha = 0.35f)),
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
}
