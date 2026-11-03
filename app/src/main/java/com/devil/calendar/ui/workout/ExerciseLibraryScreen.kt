package com.devil.calendar.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
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
import com.devil.calendar.domain.workout.ExerciseItem
import com.devil.calendar.domain.workout.MuscleGroups
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel(),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newGroup by remember { mutableStateOf(MuscleGroups.first()) }

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
            text = "Exercise library",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap an exercise to add it to the routine.",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.animatedEntrance(1),
        )

        Spacer(Modifier.height(14.dp))
        WorkoutField(
            value = query,
            onValueChange = {
                query = it
                viewModel.setQuery(it)
            },
            placeholder = "Search exercises",
            maxLength = 40,
            modifier = Modifier.animatedEntrance(2),
        )

        Spacer(Modifier.height(14.dp))
        if (exercises.isEmpty()) {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Text(
                    text = "Nothing matches that search.",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            exercises.forEachIndexed { index, exercise ->
                ExerciseRow(
                    exercise = exercise,
                    onAdd = { viewModel.addToRoutine(exercise.id) },
                    onDelete = if (exercise.isCustom) {
                        { viewModel.deleteCustom(exercise.id) }
                    } else {
                        null
                    },
                    modifier = Modifier.animatedEntrance(index + 3),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Create a custom exercise")
        Spacer(Modifier.height(8.dp))
        WorkoutField(
            value = newName,
            onValueChange = { newName = it },
            placeholder = "Exercise name",
            maxLength = 40,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MuscleGroups.forEach { group ->
                val selected = group == newGroup
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) DevilColors.Yellow else DevilColors.Card)
                        .pressFeedback(onClick = { newGroup = group })
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = group,
                        color = if (selected) DevilColors.Ink else DevilColors.SegInactiveLabel,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "ADD CUSTOM EXERCISE",
            onClick = {
                viewModel.createCustom(newName, newGroup) { newName = "" }
            },
            enabled = newName.isNotBlank(),
        )
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseItem,
    onAdd: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
        onClick = onAdd,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = exercise.muscleGroup,
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (onDelete != null) {
                IconAction(
                    icon = Icons.Rounded.DeleteOutline,
                    description = "Delete custom exercise",
                    onClick = onDelete,
                )
            }
        }
    }
}
