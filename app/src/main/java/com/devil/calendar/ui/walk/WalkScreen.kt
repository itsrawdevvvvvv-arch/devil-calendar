package com.devil.calendar.ui.walk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.ui.components.LiquidWord
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.bottomFadeHeight
import com.devil.calendar.ui.components.edgeFade
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.components.topFadeHeight
import com.devil.calendar.ui.theme.DevilColors
import kotlin.math.max

/**
 * Terrace-friendly walk tracking. No GPS: steps and heading only, so the trail is
 * a shape rather than a map, and it drifts over time.
 *
 * The daily goal is the hero here - the word, the count and the bar - because
 * that is the thing you look at every day. The walk itself is the secondary card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkScreen(
    onBack: () -> Unit,
    viewModel: WalkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showGoal by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .edgeFade(top = topFadeHeight(), bottom = bottomFadeHeight())
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = 120.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = DevilColors.Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressFeedback(haptic = false, onClick = onBack)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text("Walk", color = DevilColors.White, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(18.dp))

        // ---- hero: the goal ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(DevilColors.Card)
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LiquidWord(
                word = state.word,
                progress = state.progress,
                height = 104.dp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${state.stepsToday}",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = " / ${state.goal} steps",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            GoalBar(progress = state.progress, done = state.goalReached)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DevilColors.Ink)
                        .pressFeedback(haptic = false) { showGoal = true }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                ) {
                    Text(
                        text = "CHANGE GOAL",
                        color = DevilColors.Yellow,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state.goalReached) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DevilColors.Yellow)
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    ) {
                        Text(
                            text = "GOAL DONE",
                            color = DevilColors.Ink,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- the walk ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DevilColors.Card)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "${state.walkSteps} steps",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "${"%.0f".format(state.distanceMetres)} m",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DevilColors.Ink),
                contentAlignment = Alignment.Center,
            ) {
                if (state.path.size < 2) {
                    Text(
                        text = if (state.walking) "Walk to draw your trail" else "Start a walk to draw your trail",
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                WalkPath(path = state.path, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Dead reckoning drifts - this is a trail, not a map.",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(16.dp))
        if (!state.hasSensor) {
            Text(
                text = "This phone has no step sensor, so walks cannot be tracked.",
                color = DevilColors.RedText,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
        }
        PillButton(
            text = if (state.walking) "STOP WALK" else "START WALK",
            onClick = { if (state.walking) viewModel.stopWalk() else viewModel.startWalk() },
            enabled = state.hasSensor,
            height = 56.dp,
        )
    }

    if (showGoal) {
        ModalBottomSheet(
            onDismissRequest = { showGoal = false },
            sheetState = sheetState,
            containerColor = DevilColors.Card,
            dragHandle = null,
        ) {
            GoalSheet(
                current = state.goal,
                onDismiss = { showGoal = false },
                onConfirm = viewModel::setGoal,
            )
        }
    }
}

@Composable
private fun GoalBar(progress: Float, done: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Ink),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(if (done) DevilColors.Yellow else DevilColors.RedText),
        )
    }
}

/** Auto-fitting trail with a comet head and a fading tail. */
@Composable
private fun WalkPath(path: List<Pair<Double, Double>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (path.size < 2) return@Canvas
        val minX = path.minOf { it.first }
        val maxX = path.maxOf { it.first }
        val minY = path.minOf { it.second }
        val maxY = path.maxOf { it.second }
        val span = max(max(maxX - minX, maxY - minY), 1.0)
        val scale = (minOf(size.width, size.height) * 0.80f) / span.toFloat()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val midX = ((minX + maxX) / 2).toFloat()
        val midY = ((minY + maxY) / 2).toFloat()

        fun project(p: Pair<Double, Double>) = Offset(
            cx + (p.first.toFloat() - midX) * scale,
            cy - (p.second.toFloat() - midY) * scale,
        )

        for (i in 1 until path.size) {
            val alpha = 0.15f + 0.75f * (i.toFloat() / path.size)
            drawLine(
                color = DevilColors.RedText.copy(alpha = alpha),
                start = project(path[i - 1]),
                end = project(path[i]),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val head = project(path.last())
        drawCircle(
            brush = Brush.radialGradient(
                listOf(DevilColors.Yellow, Color.Transparent),
                center = head,
                radius = 18.dp.toPx(),
            ),
            radius = 18.dp.toPx(),
            center = head,
        )
        drawCircle(color = DevilColors.Yellow, radius = 5.dp.toPx(), center = head)
    }
}
