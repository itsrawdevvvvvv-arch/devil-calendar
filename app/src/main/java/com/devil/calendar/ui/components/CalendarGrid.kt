package com.devil.calendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion
import com.devil.calendar.ui.theme.Motion
import com.devil.calendar.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

private val WeekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun CalendarGrid(
    month: YearMonth,
    crossed: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = DateUtils.today(),
) {
    val motion = LocalMotion.current
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            WeekdayLabels.forEach { label ->
                Text(
                    text = label,
                    color = DevilColors.WeekdayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val cells = DateUtils.gridDates(month)
        val todayKey = DateUtils.key(today)

        // Rows are laid out manually so each cell can animate independently.
        cells.chunked(7).forEachIndexed { rowIndex, week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                week.forEachIndexed { colIndex, date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date == null) {
                            Spacer(Modifier.height(44.dp))
                        } else {
                            val key = DateUtils.key(date)
                            DayCell(
                                day = date.dayOfMonth,
                                crossed = key in crossed,
                                isToday = key == todayKey,
                                onClick = { onToggle(key) },
                                // Only the first row staggers, to hint at the
                                // grid without delaying every later row.
                                delayMs = motion.stagger(if (rowIndex == 0) colIndex else 0),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    crossed: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    delayMs: Int,
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val slashProgress by animateFloatAsState(
        targetValue = if (crossed) 1f else 0f,
        animationSpec = motion.tune(Motion.quick),
        label = "slash",
    )
    val numberColor by animateColorAsState(
        targetValue = when {
            isToday -> DevilColors.White
            crossed -> DevilColors.DayDisabled
            else -> DevilColors.Ink
        },
        animationSpec = motion.tuneColor(160),
        label = "number",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isToday) DevilColors.Ink else Color.Transparent,
        animationSpec = motion.tuneColor(260),
        label = "bg",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = motion.tune(Motion.quick),
        label = "press",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            color = numberColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        // The diagonal slash. It grows from the centre outwards as it appears.
        if (slashProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .scale(scaleX = slashProgress, scaleY = 1f)
                    .rotate(-42f)
                    .background(DevilColors.Slash),
            )
        }
    }
}
