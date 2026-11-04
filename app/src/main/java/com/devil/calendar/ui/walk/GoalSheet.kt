package com.devil.calendar.ui.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.theme.DevilColors

/**
 * Pick a daily step goal three ways, because one way is never enough: type it,
 * nudge it, or scroll a list of round numbers.
 */
@Composable
fun GoalSheet(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var typed by remember { mutableStateOf(current.toString()) }
    var selected by remember { mutableStateOf(current) }

    val presets = remember { (1000..30000 step 500).toList() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 44.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(DevilColors.CardBorder),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Daily step goal",
            color = DevilColors.White,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(14.dp))

        // Typing.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DevilColors.Ink)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (typed.isEmpty()) {
                    Text("Type a goal", color = DevilColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                BasicTextField(
                    value = typed,
                    onValueChange = { input ->
                        typed = input.filter { it.isDigit() }.take(5)
                        typed.toIntOrNull()?.let {
                            selected = it.coerceIn(500, 50_000)
                        }
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(DevilColors.Yellow),
                    textStyle = TextStyle(color = DevilColors.White, fontSize = 20.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.size(10.dp))
            StepChip("-500") {
                selected = (selected - 500).coerceIn(500, 50_000)
                typed = selected.toString()
            }
            Spacer(Modifier.size(8.dp))
            StepChip("+500") {
                selected = (selected + 500).coerceIn(500, 50_000)
                typed = selected.toString()
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Or scroll",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))

        // Scrolling.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DevilColors.Ink),
        ) {
            items(presets) { value ->
                val active = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (active) DevilColors.Card else DevilColors.Ink)
                        .pressFeedback(haptic = false) {
                            selected = value
                            typed = value.toString()
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "$value",
                        color = if (active) DevilColors.Yellow else DevilColors.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (active) {
                        Text(
                            text = "SELECTED",
                            color = DevilColors.Yellow,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PillButton(
            text = "SET ${selected.coerceIn(500, 50_000)}",
            onClick = { onConfirm(selected.coerceIn(500, 50_000)); onDismiss() },
            height = 54.dp,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StepChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Card)
            .pressFeedback(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(text = label, color = DevilColors.White, style = MaterialTheme.typography.bodySmall)
    }
}
