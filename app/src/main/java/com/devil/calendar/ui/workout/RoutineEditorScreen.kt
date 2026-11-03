package com.devil.calendar.ui.workout

import com.devil.calendar.ui.components.edgeFade
import com.devil.calendar.ui.components.topFadeHeight
import com.devil.calendar.ui.components.bottomFadeHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.local.workout.RoutineKind
import com.devil.calendar.domain.workout.RestPresets
import com.devil.calendar.domain.workout.RoutineExerciseItem
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

/**
 * The routine builder. Deliberately minimal, as specified: a name, then per
 * exercise a name, a number of sets and a rest time. Nothing else is required
 * to save, and there is no library to pick from - the name field is free text
 * with autocomplete over names this user has typed before.
 */
@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    onStartSession: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val detail by viewModel.routine.collectAsStateWithLifecycle()
    val newName by viewModel.newName.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val routine = detail ?: return
    val isSingle = routine.kind == RoutineKind.SINGLE

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

        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isSingle) "Single workout" else "Routine",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        WorkoutField(
            value = routine.name,
            onValueChange = viewModel::rename,
            placeholder = if (isSingle) "Workout name" else "Routine name",
            modifier = Modifier.animatedEntrance(0),
            maxLength = 40,
        )

        Spacer(Modifier.height(16.dp))
        PillButton(
            text = "START SESSION",
            onClick = { viewModel.startSession(onStartSession) },
            enabled = routine.exercises.isNotEmpty(),
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel(
            if (isSingle) "Exercise" else "Exercises (${routine.exercises.size})",
        )
        Spacer(Modifier.height(8.dp))

        routine.exercises.forEachIndexed { index, line ->
            ExerciseLineCard(
                line = line,
                canMoveUp = index > 0,
                canMoveDown = index < routine.exercises.lastIndex,
                onMove = { delta -> viewModel.move(line.id, delta) },
                onRemove = { viewModel.removeLine(line.id) },
                onSets = { viewModel.setSets(line.id, it) },
                onRest = { viewModel.setRest(line.id, it) },
                onMode = { mode, hold -> viewModel.setMode(line.id, mode, hold) },
                modifier = Modifier.animatedEntrance(index + 1),
            )
            Spacer(Modifier.height(10.dp))
        }

        if (!isSingle || routine.exercises.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            SectionLabel("Add an exercise")
            Spacer(Modifier.height(8.dp))
            WorkoutField(
                value = newName,
                onValueChange = viewModel::onNewNameChange,
                placeholder = "Type a name, e.g. Push-ups",
                modifier = Modifier.fillMaxWidth(),
                maxLength = 40,
            )
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    suggestions.take(3).forEach { suggestion ->
                        SuggestionChip(
                            text = suggestion,
                            onClick = { viewModel.addExercise(suggestion) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PillButton(
                text = "ADD EXERCISE",
                onClick = { viewModel.addExercise() },
                enabled = newName.isNotBlank(),
                background = DevilColors.Card,
                textColor = DevilColors.White,
                height = 52.dp,
            )
        }
    }
}

@Composable
private fun ExerciseLineCard(
    line: RoutineExerciseItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onSets: (Int) -> Unit,
    onRest: (Int) -> Unit,
    onMode: (ExerciseMode, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = line.name,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconAction(
                    icon = Icons.Rounded.KeyboardArrowUp,
                    description = "Move up",
                    onClick = { if (canMoveUp) onMove(-1) },
                )
                Spacer(Modifier.width(4.dp))
                IconAction(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    description = "Move down",
                    onClick = { if (canMoveDown) onMove(1) },
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "REMOVE",
                    color = DevilColors.RedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressFeedback(onClick = onRemove)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sets",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(52.dp),
                )
                Stepper(
                    value = "${line.defaultSets}",
                    onMinus = { onSets(line.defaultSets - 1) },
                    onPlus = { onSets(line.defaultSets + 1) },
                )
                Spacer(Modifier.weight(1f))
                ModeToggle(
                    mode = line.mode,
                    onSelect = { mode -> onMode(mode, line.holdSeconds) },
                )
            }

            if (line.mode == ExerciseMode.HOLD) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hold",
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(52.dp),
                    )
                    Stepper(
                        value = "${line.holdSeconds}s",
                        onMinus = { onMode(ExerciseMode.HOLD, line.holdSeconds - 5) },
                        onPlus = { onMode(ExerciseMode.HOLD, line.holdSeconds + 5) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Rest",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RestPresets.forEach { preset ->
                    RestChoice(
                        seconds = preset,
                        selected = line.restSeconds == preset,
                        onClick = { onRest(preset) },
                    )
                }
                RestChoice(
                    seconds = line.restSeconds,
                    selected = line.restSeconds !in RestPresets,
                    onClick = { onRest(line.restSeconds + 15) },
                    custom = true,
                )
            }
        }
    }
}

@Composable
private fun RestChoice(
    seconds: Int,
    selected: Boolean,
    onClick: () -> Unit,
    custom: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) DevilColors.Yellow else DevilColors.Ink)
            .pressFeedback(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (custom) "${seconds}s" else "${seconds}s",
            color = if (selected) DevilColors.Ink else DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ModeToggle(mode: ExerciseMode, onSelect: (ExerciseMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Ink),
    ) {
        ModeTab("REPS", mode == ExerciseMode.REPS) { onSelect(ExerciseMode.REPS) }
        ModeTab("HOLD", mode == ExerciseMode.HOLD) { onSelect(ExerciseMode.HOLD) }
    }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) DevilColors.CardBorder else DevilColors.Ink)
            .pressFeedback(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            color = if (selected) DevilColors.White else DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Card)
            .pressFeedback(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = DevilColors.White,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
