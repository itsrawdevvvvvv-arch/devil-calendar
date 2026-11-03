package com.devil.calendar.ui.workout

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
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.ProgressLineChart
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors

@Composable
fun ExerciseProgressScreen(
    onBack: () -> Unit,
    viewModel: ExerciseProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            text = state.name.ifBlank { "Progress" },
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Heaviest set per session",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        BentoCard(
            modifier = Modifier.fillMaxWidth().animatedEntrance(1),
            color = DevilColors.Card,
            borderColor = DevilColors.CardBorder,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                ProgressLineChart(points = state.points.map { it.bestWeightKg.toFloat() })
            }
        }

        Spacer(Modifier.height(12.dp))
        state.points.reversed().forEachIndexed { index, point ->
            BentoCard(
                modifier = Modifier.fillMaxWidth().animatedEntrance(index + 2),
                color = DevilColors.Card,
                borderColor = DevilColors.CardBorder,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDate(point.timeMillis),
                        color = DevilColors.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatVolume(point.bestWeightKg),
                        color = DevilColors.Yellow,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
