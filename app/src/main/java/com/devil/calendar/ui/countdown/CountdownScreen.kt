package com.devil.calendar.ui.countdown

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.ProgressRing
import com.devil.calendar.ui.components.WheelPicker
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.theme.LocalMotion
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
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().animatedEntrance(0)) {
            Text(
                text = "\u2039 back",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressFeedback(haptic = false, onClick = onBack)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.mode.label,
            color = DevilColors.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.animatedEntrance(1),
        )
        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = state.running,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "countdownBody",
        ) { running ->
            if (running) {
                RunningBody(state = state, onPauseResume = {
                    if (state.paused) viewModel.resume() else viewModel.pause()
                }, onReset = viewModel::reset)
            } else {
                PickerBody(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PickerBody(
    state: CountdownUiState,
    viewModel: CountdownViewModel,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Quick presets.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CountdownPresets.forEach { (label, millis) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DevilColors.Card)
                        .pressFeedback(onClick = { viewModel.applyPreset(millis) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = DevilColors.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WheelPicker(
                itemCount = 24,
                selectedIndex = state.hours,
                onSelected = viewModel::setHours,
                labelOf = { "%02d".format(it) },
                suffix = "h",
                modifier = Modifier.weight(1f),
            )
            WheelPicker(
                itemCount = 60,
                selectedIndex = state.minutes,
                onSelected = viewModel::setMinutes,
                labelOf = { "%02d".format(it) },
                suffix = "m",
                infinite = true,
                modifier = Modifier.weight(1f),
            )
            if (state.mode.showSeconds) {
                WheelPicker(
                    itemCount = 60,
                    selectedIndex = state.seconds,
                    onSelected = viewModel::setSeconds,
                    labelOf = { "%02d".format(it) },
                    suffix = "s",
                    infinite = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PillButton(
            text = "START",
            onClick = viewModel::start,
            enabled = state.canStart,
        )
    }
}

@Composable
private fun RunningBody(
    state: CountdownUiState,
    onPauseResume: () -> Unit,
    onReset: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(
            progress = state.progress,
            modifier = Modifier.size(260.dp),
        ) {
            Text(
                text = state.display,
                color = DevilColors.White,
                style = MaterialTheme.typography.displayMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
        PillButton(
            text = if (state.paused) "RESUME" else "PAUSE",
            onClick = onPauseResume,
        )

        Spacer(Modifier.height(12.dp))
        if (state.mode == CountdownMode.FOCUS) {
            HoldToConfirmButton(label = "END SESSION", onConfirm = onReset)
        } else {
            PillButton(
                text = "RESET",
                onClick = onReset,
                background = DevilColors.Card,
                textColor = DevilColors.White,
            )
        }
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
