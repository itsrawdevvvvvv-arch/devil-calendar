package com.devil.calendar.ui.workout

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.domain.workout.RoutineExerciseItem
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

private val RestSteps = listOf(30, 45, 60, 90, 120, 180)

@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    val detail = routine ?: return

    var name by remember(detail.id) { mutableStateOf(detail.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
            text = "Edit routine",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel("Name")
        Spacer(Modifier.height(6.dp))
        WorkoutField(
            value = name,
            onValueChange = {
                name = it
                viewModel.rename(it)
            },
            placeholder = "Routine name",
            maxLength = 40,
            modifier = Modifier.animatedEntrance(1),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Exercises (${detail.exercises.size})")
        Spacer(Modifier.height(8.dp))

        if (detail.exercises.isEmpty()) {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Text(
                    text = "No exercises yet. Add the first one below.",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            detail.exercises.forEachIndexed { index, line ->
                ExerciseLine(
                    line = line,
                    onUp = { viewModel.move(line.id, -1) },
                    onDown = { viewModel.move(line.id, 1) },
                    onRemove = { viewModel.removeLine(line.id) },
                    onCycleRest = {
                        val index = RestSteps.indexOf(line.restSeconds).takeIf { it >= 0 } ?: 0
                        viewModel.setRest(line.id, RestSteps[(index + 1) % RestSteps.size])
                    },
                    modifier = Modifier.animatedEntrance(index + 2),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        PillButton(text = "ADD EXERCISE", onClick = onAddExercise)

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                text = "DUPLICATE",
                onClick = { viewModel.duplicate { onBack() } },
                background = DevilColors.Card,
                textColor = DevilColors.White,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "DELETE",
                onClick = { viewModel.delete(onBack) },
                background = DevilColors.Card,
                textColor = DevilColors.Red,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExerciseLine(
    line: RoutineExerciseItem,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    onCycleRest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = line.name,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = line.muscleGroup,
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.width(8.dp))
                    RestChip(seconds = line.restSeconds, onCycle = onCycleRest)
                }
            }
            IconAction(
                icon = Icons.Rounded.KeyboardArrowUp,
                description = "Move up",
                onClick = onUp,
            )
            Spacer(Modifier.width(4.dp))
            IconAction(
                icon = Icons.Rounded.KeyboardArrowDown,
                description = "Move down",
                onClick = onDown,
            )
            Spacer(Modifier.width(4.dp))
            IconAction(Icons.Rounded.DeleteOutline, "Remove exercise", onRemove)
        }
    }
}
