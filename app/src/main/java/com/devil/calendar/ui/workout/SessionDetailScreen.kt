package com.devil.calendar.ui.workout

import com.devil.calendar.ui.components.edgeFade
import com.devil.calendar.ui.components.topFadeHeight
import com.devil.calendar.ui.components.bottomFadeHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.domain.workout.SessionDetail
import com.devil.calendar.domain.workout.SessionExerciseItem
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.HorizontalBar
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val session = detail ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .edgeFade(top = topFadeHeight(), bottom = bottomFadeHeight())
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = DevilColors.Muted,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .pressFeedback(haptic = false, onClick = onBack)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatDate(session.startTime),
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${formatDuration(session.durationMinutes)}  -  ${formatVolume(session.volumeKg)}",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        session.exercises.forEachIndexed { index, exercise ->
            ExerciseSummary(
                exercise = exercise,
                onClick = { onOpenExercise(exercise.exerciseId) },
                modifier = Modifier.animatedEntrance(index + 1),
            )
            Spacer(Modifier.height(10.dp))
        }

        if (session.muscleVolume.isNotEmpty()) {
            val maxMuscle = session.muscleVolume.first().volumeKg.coerceAtLeast(1.0)
            Spacer(Modifier.height(6.dp))
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Volume by muscle",
                        color = DevilColors.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    session.muscleVolume.forEachIndexed { index, muscle ->
                        HorizontalBar(
                            label = muscle.muscleGroup,
                            valueText = formatVolume(muscle.volumeKg),
                            fraction = (muscle.volumeKg / maxMuscle).toFloat(),
                        )
                        if (index != session.muscleVolume.lastIndex) Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSummary(
    exercise: SessionExerciseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.name,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = exercise.muscleGroup,
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val done = exercise.sets.filter { it.completed }
            if (done.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "No completed sets",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Row {
                    done.take(6).forEach { set ->
                        SetPill(
                            text = "${weightText(set.weight)} x ${set.reps}",
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetPill(text: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Ink)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = DevilColors.Yellow,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun weightText(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else String.format(java.util.Locale.US, "%.1f", weight)
