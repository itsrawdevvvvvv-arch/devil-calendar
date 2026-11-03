package com.devil.calendar.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius

/** Small round icon button used across the workout screens. */
@Composable
internal fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Ink)
            .pressFeedback(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = DevilColors.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Uppercase section heading in the app's muted tone. */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = DevilColors.Muted,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

/** Themed single-line text field, matching the Profile fields. */
@Composable
internal fun WorkoutField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 60,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth().height(56.dp),
        color = DevilColors.Card,
        cornerRadius = DevilRadius.field,
        borderColor = DevilColors.CardBorder,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                textStyle = MaterialTheme.typography.titleMedium.copy(color = DevilColors.White),
                cursorBrush = SolidColor(DevilColors.Red),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Compact -/+ stepper used for weight and reps inside a session. */
@Composable
internal fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(DevilColors.Ink)
                .pressFeedback(haptic = false, onClick = onMinus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Decrease",
                tint = DevilColors.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = value,
            color = DevilColors.White,
            style = MaterialTheme.typography.titleSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.widthIn(min = 46.dp),
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(DevilColors.Ink)
                .pressFeedback(haptic = false, onClick = onPlus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Increase",
                tint = DevilColors.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Cycles the rest time on tap. A proper picker arrives with the session phase. */
@Composable
internal fun RestChip(seconds: Int, onCycle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Ink)
            .pressFeedback(onClick = onCycle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "Rest ${seconds}s",
            color = DevilColors.Yellow,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
