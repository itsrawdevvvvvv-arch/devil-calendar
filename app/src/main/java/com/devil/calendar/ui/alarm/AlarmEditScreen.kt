package com.devil.calendar.ui.alarm

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.domain.AlarmTime
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.SegmentedControl
import com.devil.calendar.ui.components.WheelPicker
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.util.TimeFormat
import java.time.LocalTime
import java.time.ZonedDateTime

@Composable
fun AlarmEditScreen(
    onBack: () -> Unit,
    viewModel: AlarmEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
    ) {
        Text(
            text = "\u2039 back",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .pressFeedback(haptic = false, onClick = onBack)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.isNew) "New alarm" else "Edit alarm",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )

        Spacer(Modifier.height(18.dp))

        // Time wheels. In 24-hour mode the hour wheel is 0..23 and there is no
        // AM/PM toggle.
        if (state.use24Hour) {
            Row(
                modifier = Modifier.fillMaxWidth().animatedEntrance(1),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WheelPicker(
                    itemCount = 24,
                    selectedIndex = state.hour,
                    onSelected = viewModel::setHour24,
                    labelOf = { "%02d".format(it) },
                    suffix = "h",
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    itemCount = 60,
                    selectedIndex = state.minute,
                    onSelected = viewModel::setMinute,
                    labelOf = { "%02d".format(it) },
                    suffix = "m",
                    infinite = true,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().animatedEntrance(1),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    itemCount = 12,
                    selectedIndex = TimeFormat.displayHour(state.hour) - 1,
                    onSelected = { index ->
                        viewModel.setHour24(
                            TimeFormat.to24Hour(index + 1, isPm = state.hour >= 12),
                        )
                    },
                    labelOf = { (it + 1).toString() },
                    modifier = Modifier.weight(1f),
                )
                WheelPicker(
                    itemCount = 60,
                    selectedIndex = state.minute,
                    onSelected = viewModel::setMinute,
                    labelOf = { "%02d".format(it) },
                    infinite = true,
                    modifier = Modifier.weight(1f),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SegmentedControl(
                        options = listOf("AM", "PM"),
                        selectedIndex = if (state.hour >= 12) 1 else 0,
                        onSelect = { index ->
                            viewModel.setHour24(
                                TimeFormat.to24Hour(TimeFormat.displayHour(state.hour), isPm = index == 1),
                            )
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val helper = remember(state.hour, state.minute, state.days) {
            val now = ZonedDateTime.now()
            val next = AlarmTime.nextTrigger(LocalTime.of(state.hour, state.minute), state.days, now)
            "Rings in " + AlarmTime.describeDelay(next, now)
        }
        Text(
            text = helper,
            color = DevilColors.Yellow,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.animatedEntrance(2),
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Repeat",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().animatedEntrance(3),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TimeFormat.dayOrder.forEachIndexed { index, day ->
                val selected = day in state.days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) DevilColors.Yellow else DevilColors.Card)
                        .pressFeedback(onClick = { viewModel.toggleDay(day) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = listOf("M", "T", "W", "T", "F", "S", "S")[index],
                        color = if (selected) DevilColors.Ink else DevilColors.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Label",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .animatedEntrance(4),
            color = DevilColors.Card,
            cornerRadius = DevilRadius.field,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (state.label.isEmpty()) {
                    Text(
                        text = "e.g. Wake up",
                        color = DevilColors.Muted,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                BasicTextField(
                    value = state.label,
                    onValueChange = viewModel::setLabel,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = DevilColors.White),
                    cursorBrush = SolidColor(DevilColors.Red),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PillButton(
            text = "SAVE",
            onClick = { viewModel.save(onBack) },
        )

        if (!state.isNew) {
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = "DELETE",
                onClick = { viewModel.delete(onBack) },
                background = DevilColors.Card,
                textColor = DevilColors.White,
            )
        }
    }
}
