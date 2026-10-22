package com.devil.calendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion
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

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                week.forEach { date ->
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
) {
    val motion = LocalMotion.current

    // The slash draws itself from top-right to bottom-left, and un-draws when
    // the day is uncrossed.
    val slash = remember { Animatable(if (crossed) 1f else 0f) }
    LaunchedEffect(crossed) {
        if (crossed) {
            slash.animateTo(1f, motion.tweenOf(280, FastOutSlowInEasing))
        } else {
            slash.animateTo(0f, motion.tweenOf(200))
        }
    }

    val numberColor by animateColorAsState(
        targetValue = when {
            isToday -> DevilColors.White
            crossed -> DevilColors.DayDisabled
            else -> DevilColors.Ink
        },
        animationSpec = motion.tuneColor(200),
        label = "number",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isToday) DevilColors.Ink else Color.Transparent,
        animationSpec = motion.tuneColor(260),
        label = "bg",
    )
    val todayScale by animateFloatAsState(
        targetValue = if (isToday) 1f else 1f,
        animationSpec = motion.springOf(),
        label = "todayCell",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .scale(todayScale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .pressFeedback(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            color = numberColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (slash.value > 0.01f) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 7.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .rotate(-42f)
                    // Grows from its right end, so the slash draws from
                    // top-right towards bottom-left. transformOrigin is set
                    // through graphicsLayer: this Compose version has no
                    // scale(transformOrigin) overload.
                    .graphicsLayer {
                        scaleX = slash.value
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
                    .background(DevilColors.Slash),
            )
        }
    }
}
