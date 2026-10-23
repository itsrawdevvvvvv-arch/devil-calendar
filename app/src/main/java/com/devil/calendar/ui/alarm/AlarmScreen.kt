package com.devil.calendar.ui.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.data.local.AlarmEntity
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.util.TimeFormat

@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val use24Hour by viewModel.use24Hour.collectAsStateWithLifecycle()
    val lastDeleted by viewModel.lastDeleted.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxWidth()) {
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
                text = "Alarms",
                color = DevilColors.White,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.animatedEntrance(0),
            )

            Spacer(Modifier.height(16.dp))
            if (alarms.isEmpty()) {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = DevilColors.Card,
                    cornerRadius = DevilRadius.card,
                ) {
                    Text(
                        text = "No alarms yet. Add one and it will ring even with the app closed.",
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            } else {
                alarms.forEachIndexed { index, alarm ->
                    AlarmRow(
                        alarm = alarm,
                        use24Hour = use24Hour,
                        onToggle = { enabled -> viewModel.toggle(alarm, enabled) },
                        onEdit = { onEdit(alarm.id) },
                        onDelete = { viewModel.delete(alarm) },
                        modifier = Modifier.animatedEntrance(index),
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            PillButton(
                text = "ADD ALARM",
                onClick = { onEdit(-1L) },
            )
            // Room for the undo bar.
            Spacer(Modifier.height(80.dp))
        }

        // Undo bar after a swipe-to-delete.
        AnimatedVisibility(
            visible = lastDeleted != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DevilRadius.card))
                    .background(DevilColors.Card)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Alarm deleted",
                    color = DevilColors.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "UNDO",
                    color = DevilColors.Yellow,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressFeedback(onClick = viewModel::undoDelete)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }

    // Auto-dismiss the undo affordance.
    LaunchedEffect(lastDeleted) {
        if (lastDeleted != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.clearUndo()
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmEntity,
    use24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    androidx.compose.material3.SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(DevilRadius.card))
                    .background(DevilColors.Red),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = "DELETE",
                    color = DevilColors.Ink,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
        modifier = modifier,
    ) {
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            color = DevilColors.Card,
            cornerRadius = DevilRadius.card,
            onClick = onEdit,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TimeFormat.format(alarm.hour, alarm.minute, use24Hour),
                        color = if (alarm.enabled) DevilColors.White else DevilColors.Muted,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = buildString {
                            append(TimeFormat.repeatSummary(alarm.days))
                            if (alarm.label.isNotBlank()) append("  -  ${alarm.label}")
                        },
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DevilColors.Ink,
                        checkedTrackColor = DevilColors.Yellow,
                        uncheckedThumbColor = DevilColors.White,
                        uncheckedTrackColor = DevilColors.NavIdle,
                    ),
                )
            }
        }
    }
}
