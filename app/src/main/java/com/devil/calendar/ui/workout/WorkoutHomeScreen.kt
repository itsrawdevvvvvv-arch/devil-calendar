package com.devil.calendar.ui.workout

import com.devil.calendar.ui.components.edgeFade
import com.devil.calendar.ui.components.topFadeHeight
import com.devil.calendar.ui.components.bottomFadeHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.data.local.workout.RoutineKind
import com.devil.calendar.domain.workout.RoutineSummary
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

@Composable
fun WorkoutHomeScreen(
    onOpenRoutine: (Long) -> Unit,
    onSessionStarted: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAiImport: () -> Unit,
    onOpenWalk: () -> Unit,
    viewModel: WorkoutHomeViewModel = hiltViewModel(),
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .edgeFade(top = topFadeHeight(), bottom = bottomFadeHeight())
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
    ) {
        Text(
            text = "Workout",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (routines.isEmpty()) {
                "No routines yet"
            } else {
                "${routines.size} ${if (routines.size == 1) "routine" else "routines"}"
            },
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.animatedEntrance(1),
        )

        Spacer(Modifier.height(18.dp))

        val multi = routines.filter { it.kind == RoutineKind.ROUTINE }
        val singles = routines.filter { it.kind == RoutineKind.SINGLE }

        if (multi.isEmpty()) {
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animatedEntrance(2),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Build your first routine",
                        color = DevilColors.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Pick exercises once, then start a session from it any day.",
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            multi.forEachIndexed { index, routine ->
                RoutineCard(
                    routine = routine,
                    onOpen = { onOpenRoutine(routine.id) },
                    onStart = { viewModel.startSession(routine.id, onSessionStarted) },
                    onDuplicate = { viewModel.duplicate(routine.id) },
                    onDelete = { viewModel.delete(routine.id) },
                    modifier = Modifier.animatedEntrance(index + 2),
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        if (singles.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionLabel("Single workouts")
            Spacer(Modifier.height(8.dp))
            singles.forEachIndexed { index, routine ->
                RoutineCard(
                    routine = routine,
                    onOpen = { onOpenRoutine(routine.id) },
                    onStart = { viewModel.startSession(routine.id, onSessionStarted) },
                    onDuplicate = { viewModel.duplicate(routine.id) },
                    onDelete = { viewModel.delete(routine.id) },
                    modifier = Modifier.animatedEntrance(index + 2),
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        PillButton(
            text = "START EMPTY SESSION",
            onClick = { viewModel.startSession(null, onSessionStarted) },
            modifier = Modifier.animatedEntrance(routines.size + 2),
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "NEW ROUTINE",
            onClick = { viewModel.createRoutine(RoutineKind.ROUTINE, onOpenRoutine) },
            background = DevilColors.Card,
            textColor = DevilColors.White,
            modifier = Modifier.animatedEntrance(routines.size + 3),
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "NEW SINGLE WORKOUT",
            onClick = { viewModel.createRoutine(RoutineKind.SINGLE, onOpenRoutine) },
            background = DevilColors.Card,
            textColor = DevilColors.White,
            modifier = Modifier.animatedEntrance(routines.size + 4),
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "IMPORT WITH AI",
            onClick = onOpenAiImport,
            background = DevilColors.Card,
            textColor = DevilColors.White,
            modifier = Modifier.animatedEntrance(routines.size + 5),
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "WALK",
            onClick = onOpenWalk,
            background = DevilColors.Card,
            textColor = DevilColors.White,
            modifier = Modifier.animatedEntrance(routines.size + 6),
        )
        Spacer(Modifier.height(10.dp))
        PillButton(
            text = "HISTORY",
            onClick = onOpenHistory,
            background = DevilColors.Card,
            textColor = DevilColors.White,
            modifier = Modifier.animatedEntrance(routines.size + 4),
        )
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineSummary,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${routine.exerciseCount} " +
                        if (routine.exerciseCount == 1) "exercise" else "exercises",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconAction(Icons.Rounded.PlayArrow, "Start session", onStart)
            Spacer(Modifier.width(8.dp))
            IconAction(Icons.Rounded.ContentCopy, "Duplicate routine", onDuplicate)
            Spacer(Modifier.width(8.dp))
            IconAction(Icons.Rounded.DeleteOutline, "Delete routine", onDelete)
        }
    }
}
