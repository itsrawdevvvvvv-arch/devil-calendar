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
import com.devil.calendar.domain.workout.HistoryItem
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.HorizontalBar
import com.devil.calendar.ui.components.VolumeBarChart
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            text = "History",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )

        Spacer(Modifier.height(16.dp))
        BentoCard(
            modifier = Modifier.fillMaxWidth().animatedEntrance(1),
            color = DevilColors.Yellow,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "This week",
                        color = DevilColors.Ink,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = formatVolume(state.weekVolume),
                        color = DevilColors.Ink,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
                VolumeBarChart(
                    values = state.weekValues,
                    labels = state.weekLabels,
                )
            }
        }

        if (state.muscleVolume.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            val maxMuscle = state.muscleVolume.first().volumeKg.coerceAtLeast(1.0)
            BentoCard(
                modifier = Modifier.fillMaxWidth().animatedEntrance(2),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Volume by muscle - 30 days",
                        color = DevilColors.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    state.muscleVolume.take(6).forEachIndexed { index, muscle ->
                        HorizontalBar(
                            label = muscle.muscleGroup,
                            valueText = formatVolume(muscle.volumeKg),
                            fraction = (muscle.volumeKg / maxMuscle).toFloat(),
                        )
                        if (index != state.muscleVolume.take(6).lastIndex) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sessions",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))

        if (!state.hasHistory) {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Text(
                    text = "Finish a session and it will appear here.",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            state.sessions.forEachIndexed { index, session ->
                SessionRow(
                    session = session,
                    onClick = { onOpenSession(session.id) },
                    modifier = Modifier.animatedEntrance(index + 3),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        borderColor = DevilColors.CardBorder,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(session.startTime),
                    color = DevilColors.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${formatDuration(session.durationMinutes)}  -  ${session.setCount} sets",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = formatVolume(session.volumeKg),
                color = DevilColors.Yellow,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
