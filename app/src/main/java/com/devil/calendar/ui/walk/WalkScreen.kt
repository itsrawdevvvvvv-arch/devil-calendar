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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.devil.calendar.ui.components.BentoCard
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
 * Terrace-friendly walk tracking. No GPS: steps and heading only, so the trail
 * is a shape rather than a map, and it drifts over time.
 */
@Composable
fun WalkScreen(
    onBack: () -> Unit,
    viewModel: WalkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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

        Spacer(Modifier.height(10.dp))
        LiquidWord(word = state.word, progress = state.progress)
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.goalReached) {
                "GOAL DONE - ${state.stepsToday} / ${state.goal}"
            } else {
                "${state.stepsToday} / ${state.goal} steps"
            },
            color = if (state.goalReached) DevilColors.Yellow else DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(14.dp))
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "${state.walkSteps} steps",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "${"%.0f".format(state.distanceMetres)} m this walk",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                WalkPath(path = state.path, modifier = Modifier.fillMaxWidth().height(220.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Dead reckoning drifts - this is a trail, not a map.",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
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

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3000, 6000, 10000).forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (state.goal == preset) DevilColors.Yellow else DevilColors.Card)
                        .pressFeedback(haptic = false) { viewModel.setGoal(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "$preset",
                        color = if (state.goal == preset) DevilColors.Ink else DevilColors.White,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Auto-fitting trail with a comet head and a fading tail. */
@Composable
private fun WalkPath(path: List<Pair<Double, Double>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(DevilColors.Ink)) {
        if (path.size < 2) return@Canvas
        val minX = path.minOf { it.first }
        val maxX = path.maxOf { it.first }
        val minY = path.minOf { it.second }
        val maxY = path.maxOf { it.second }
        val spanX = max(maxX - minX, 1.0)
        val spanY = max(maxY - minY, 1.0)
        val span = max(spanX, spanY)
        val scale = (minOf(size.width, size.height) * 0.82f) / span.toFloat()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val midX = ((minX + maxX) / 2).toFloat()
        val midY = ((minY + maxY) / 2).toFloat()

        fun project(p: Pair<Double, Double>) = Offset(
            cx + (p.first.toFloat() - midX) * scale,
            cy - (p.second.toFloat() - midY) * scale,
        )

        // Older segments fade, so laps on a small terrace do not become a blob.
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
