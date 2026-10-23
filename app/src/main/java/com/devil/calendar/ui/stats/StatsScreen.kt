package com.devil.calendar.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.ui.components.BarsChart
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.SegmentedControl
import com.devil.calendar.ui.components.StatCard
import com.devil.calendar.ui.components.WeekdayRow
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius

/**
 * Stats uses the same dark theme as every other screen. Like the rest, it
 * paints no background of its own - the single root background shows through,
 * which is what keeps it free of colour splits and black bands.
 */
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
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
        Text(
            text = state.dateLabel,
            color = DevilColors.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.animatedEntrance(0),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your Habit\nDetails",
            color = DevilColors.White,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.animatedEntrance(1),
        )

        Spacer(Modifier.height(18.dp))
        SegmentedControl(
            options = listOf("Day", "Week", "Month"),
            selectedIndex = state.range.ordinal,
            onSelect = viewModel::selectRange,
            modifier = Modifier.animatedEntrance(2),
        )

        Spacer(Modifier.height(14.dp))
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .animatedEntrance(3),
            color = DevilColors.Yellow,
            cornerRadius = DevilRadius.card,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "Days crossed",
                        color = DevilColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = state.rangeLabel,
                            color = DevilColors.Ink.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        // The value crossfades when the range changes.
                        AnimatedContent(
                            targetState = state.rangeValue,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                            label = "rangeValue",
                        ) { value ->
                            Text(
                                text = value,
                                color = DevilColors.Ink,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                BarsChart(bars = state.bars)
                Spacer(Modifier.height(8.dp))
                WeekdayRow(color = DevilColors.Ink)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animatedEntrance(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(label = "Streak", value = state.streak, modifier = Modifier.weight(1f))
            StatCard(
                label = "This month",
                value = state.monthCrossed,
                suffix = "/${state.monthTotal}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
