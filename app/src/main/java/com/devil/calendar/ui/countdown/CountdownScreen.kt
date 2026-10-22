package com.devil.calendar.ui.countdown

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.CircleButton
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.ProgressRing
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.theme.Motion

@Composable
fun CountdownScreen(
    onBack: () -> Unit,
    viewModel: CountdownViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackButton(onBack, Modifier.animatedEntrance(0))
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.mode.label,
            color = DevilColors.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.animatedEntrance(1),
        )
        Spacer(Modifier.height(26.dp))

        ProgressRing(
            progress = state.progress,
            modifier = Modifier
                .size(270.dp)
                .animatedEntrance(2),
        ) {
            Text(
                text = format(state.leftSeconds),
                color = DevilColors.White,
                style = MaterialTheme.typography.displayMedium,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.animatedEntrance(3),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CircleButton(
                onClick = { viewModel.step(false) },
                size = 52.dp,
                background = DevilColors.Card,
            ) {
                Text("\u2212", color = DevilColors.White, style = MaterialTheme.typography.headlineSmall)
            }
            CircleButton(
                onClick = { viewModel.step(true) },
                size = 52.dp,
                background = DevilColors.Card,
            ) {
                Text("+", color = DevilColors.White, style = MaterialTheme.typography.headlineSmall)
            }
        }

        Spacer(Modifier.height(22.dp))
        if (state.mode == CountdownMode.FOCUS && state.running) {
            HoldToConfirmButton(
                label = state.mode.stopLabel,
                onConfirm = viewModel::stop,
                modifier = Modifier.animatedEntrance(4),
            )
        } else {
            PillButton(
                text = if (state.running) state.mode.stopLabel else state.mode.startLabel,
                onClick = { if (state.running) viewModel.stop() else viewModel.start() },
                modifier = Modifier.animatedEntrance(4),
            )
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "\u2039 back",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(Unit) { detectTapGestures { onBack() } }
                .padding(vertical = 12.dp, horizontal = 4.dp),
        )
    }
}

/** A pill that fills while held for 3 seconds, then confirms. */
@Composable
private fun HoldToConfirmButton(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hold = remember { Animatable(0f) }
    var pressing by remember { mutableStateOf(false) }

    LaunchedEffect(pressing) {
        if (pressing) {
            hold.snapTo(0f)
            hold.animateTo(1f, tween(3000, easing = LinearEasing))
            onConfirm()
        } else {
            hold.animateTo(0f, tween(200, easing = Motion.easeOut))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(DevilRadius.button))
            .background(DevilColors.Card)
            .border(2.dp, DevilColors.Red, RoundedCornerShape(DevilRadius.button))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressing = true
                    tryAwaitRelease()
                    pressing = false
                })
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(hold.value.coerceIn(0f, 1f))
                .background(DevilColors.Red.copy(alpha = 0.35f)),
        )
        Text(
            text = "$label  (hold)",
            color = DevilColors.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

private fun format(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}
