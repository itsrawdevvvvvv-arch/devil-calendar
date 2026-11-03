package com.devil.calendar.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.data.local.workout.SetType
import com.devil.calendar.domain.workout.ActiveSession
import com.devil.calendar.domain.workout.SessionExerciseItem
import com.devil.calendar.domain.workout.SessionSet
import com.devil.calendar.service.RestTimerController
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.EmberBurst
import com.devil.calendar.ui.components.OdometerText
import com.devil.calendar.ui.components.RingFlare
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.ProgressRing
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SessionScreen(
    onBack: () -> Unit,
    /** Receives the active session id so the library adds to the right place. */
    onAddExercise: (Long) -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val recordSetIds by viewModel.recordSetIds.collectAsStateWithLifecycle()
    val active = session
    val motion = LocalMotion.current
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(recordSetIds.size) {
        if (recordSetIds.isNotEmpty()) {
            celebrate = true
            delay(1_500)
            celebrate = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
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
            if (active != null) {
                PillButton(
                    text = "FINISH",
                    onClick = { viewModel.finish(active.id, onBack) },
                    height = 44.dp,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Session",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )

        if (active == null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "No active session. Start one from a routine.",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            return@Column
        }

        AnimatedVisibility(
            visible = celebrate,
            enter = fadeIn(motion.tweenOf(180)) + scaleIn(motion.tweenOf(220), initialScale = 0.86f),
            exit = fadeOut(motion.tweenOf(180)),
        ) {
            RecordCelebration()
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "${active.completedSets} / ${active.totalSets} sets done",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        if (rest.active) {
            Spacer(Modifier.height(14.dp))
            RestCard(
                rest = rest,
                onMinus = { viewModel.adjustRest(-15) },
                onPlus = { viewModel.adjustRest(15) },
                onSkip = { viewModel.skipRest() },
            )
        }

        Spacer(Modifier.height(16.dp))

        active.exercises.forEachIndexed { index, exercise ->
            ExerciseCard(
                exercise = exercise,
                sessionId = active.id,
                isFirst = index == 0,
                recordSetIds = recordSetIds,
                onToggleSet = { set -> viewModel.toggleSet(active.id, exercise, set) },
                onAddSet = { viewModel.addSet(exercise.id) },
                onDeleteSet = { setId -> viewModel.deleteSet(setId) },
                onAdjustWeight = { setId, delta -> viewModel.adjustWeight(setId, delta) },
                onAdjustReps = { setId, delta -> viewModel.adjustReps(setId, delta) },
                onCycleType = { setId -> viewModel.cycleType(setId) },
                onRemove = { viewModel.removeExercise(exercise.id) },
                onUp = { viewModel.moveExercise(active.id, exercise.id, -1) },
                onDown = { viewModel.moveExercise(active.id, exercise.id, 1) },
                onToggleSuperset = { viewModel.toggleSuperset(active.id, exercise.id) },
                modifier = Modifier.animatedEntrance(index + 2),
            )
            Spacer(Modifier.height(12.dp))
        }

        if (active.exercises.isEmpty()) {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Text(
                    text = "Empty session. Add your first exercise.",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        PillButton(text = "ADD EXERCISE", onClick = { onAddExercise(active.id) })
    }
}

@Composable
private fun RestCard(
    rest: RestTimerController.RestState,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSkip: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest.endMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val remainingMillis = (rest.endMillis - now).coerceAtLeast(0L)
    val remaining = (remainingMillis / 1000L).toInt()
    val progress = if (rest.totalSeconds > 0) {
        (remainingMillis / 1000f) / rest.totalSeconds.toFloat()
    } else {
        0f
    }
    val urgent = remaining in 1..5
    val pulse by animateFloatAsState(
        targetValue = if (urgent) 1.08f else 1f,
        animationSpec = LocalMotion.current.springOf(),
        label = "restPulse",
    )

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.Yellow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
            ) {
                ProgressRing(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6f,
                    progressColor = if (urgent) DevilColors.Yellow else DevilColors.RedText,
                ) {
                    OdometerText(
                        text = "%d:%02d".format(remaining / 60, remaining % 60),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (urgent) DevilColors.Yellow else DevilColors.White,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rest",
                    color = DevilColors.Yellow,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = rest.exerciseName,
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row {
                    IconAction(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        description = "Minus fifteen seconds",
                        onClick = onMinus,
                    )
                    Spacer(Modifier.width(6.dp))
                    IconAction(
                        icon = Icons.Rounded.KeyboardArrowUp,
                        description = "Plus fifteen seconds",
                        onClick = onPlus,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "SKIP",
                    color = DevilColors.RedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressFeedback(onClick = onSkip)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: SessionExerciseItem,
    sessionId: Long,
    isFirst: Boolean,
    recordSetIds: Set<Long>,
    onToggleSet: (SessionSet) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onAdjustWeight: (Long, Double) -> Unit,
    onAdjustReps: (Long, Int) -> Unit,
    onCycleType: (Long) -> Unit,
    onRemove: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onToggleSuperset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise.name,
                            color = DevilColors.White,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        exercise.supersetLabel?.let { label ->
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(DevilColors.Yellow)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = label,
                                    color = DevilColors.Ink,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Text(
                        text = exercise.muscleGroup,
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!isFirst) {
                    IconAction(Icons.Rounded.Link, "Superset with previous", onToggleSuperset)
                    Spacer(Modifier.width(4.dp))
                }
                IconAction(Icons.Rounded.KeyboardArrowUp, "Move up", onUp)
                Spacer(Modifier.width(4.dp))
                IconAction(Icons.Rounded.KeyboardArrowDown, "Move down", onDown)
                Spacer(Modifier.width(4.dp))
                IconAction(Icons.Rounded.DeleteOutline, "Remove exercise", onRemove)
            }

            Spacer(Modifier.height(10.dp))

            exercise.sets.forEach { set ->
                SetRow(
                    set = set,
                    isRecord = set.id in recordSetIds,
                    onToggle = { onToggleSet(set) },
                    onDelete = { onDeleteSet(set.id) },
                    onAdjustWeight = { delta -> onAdjustWeight(set.id, delta) },
                    onAdjustReps = { delta -> onAdjustReps(set.id, delta) },
                    onCycleType = { onCycleType(set.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = "+ SET",
                color = DevilColors.RedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressFeedback(onClick = onAddSet)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SetRow(
    set: SessionSet,
    isRecord: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onCycleType: () -> Unit,
) {
    val motion = LocalMotion.current
    var burst by remember { mutableIntStateOf(0) }
    LaunchedEffect(set.completed) {
        if (set.completed) burst++
    }
    val rowColor by animateColorAsState(
        targetValue = if (set.completed) DevilColors.Card else DevilColors.Ink,
        animationSpec = motion.tweenOf(220),
        label = "setRow",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        EmberBurst(
            trigger = burst,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(rowColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Set ${set.index + 1}",
                color = if (set.completed) DevilColors.Muted else DevilColors.White,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.width(8.dp))
            TypeChip(type = set.type, onClick = onCycleType)
            if (isRecord) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DevilColors.Yellow)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "PR",
                        color = DevilColors.Ink,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (set.completed) DevilColors.Yellow else DevilColors.Card)
                    .pressFeedback(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = if (set.completed) "Mark incomplete" else "Complete set",
                    tint = if (set.completed) DevilColors.Ink else DevilColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stepper(
                value = weightText(set.weight),
                onMinus = { onAdjustWeight(-2.5) },
                onPlus = { onAdjustWeight(2.5) },
            )
            Spacer(Modifier.width(14.dp))
            Stepper(
                value = "${set.reps}",
                onMinus = { onAdjustReps(-1) },
                onPlus = { onAdjustReps(1) },
            )
            Spacer(Modifier.weight(1f))
            IconAction(Icons.Rounded.DeleteOutline, "Delete set", onDelete)
        }
        }
    }
}

@Composable
private fun TypeChip(type: SetType, onClick: () -> Unit) {
    val colour = when (type) {
        SetType.NORMAL -> DevilColors.Muted
        SetType.WARMUP -> DevilColors.Yellow
        SetType.DROP -> DevilColors.RedText
        SetType.SUPERSET -> DevilColors.White
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Card)
            .pressFeedback(haptic = false, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
            color = colour,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RecordCelebration() {
    val motion = LocalMotion.current
    val flare = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        flare.snapTo(0f)
        flare.animateTo(1f, motion.tweenOf(900))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        RingFlare(progress = flare.value, modifier = Modifier.size(72.dp))
        Text(
            text = "NEW RECORD",
            color = DevilColors.Yellow,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private fun weightText(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else String.format(Locale.US, "%.1f", weight)
