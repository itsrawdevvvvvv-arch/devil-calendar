package com.devil.calendar.ui.profile

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.theme.DevilColors

enum class AiKeyStatus { NOT_SET, CHECKING, CONNECTED, ERROR }

/**
 * The Sarvam key section. The key is write-only from the UI's point of view:
 * once saved it is never shown again, only replaced or cleared.
 */
@Composable
fun AiSection(
    keyInput: String,
    onKeyChange: (String) -> Unit,
    status: AiKeyStatus,
    statusText: String,
    hasKey: Boolean,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AI (Sarvam)",
                color = DevilColors.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            StatusChip(status = status, text = statusText)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your own key, stored encrypted on this device. It is never logged or shared.",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DevilColors.Ink)
                .padding(12.dp),
        ) {
            if (keyInput.isEmpty()) {
                Text(
                    text = if (hasKey) "Key saved - paste a new one to replace" else "Paste your Sarvam key",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            BasicTextField(
                value = keyInput,
                onValueChange = onKeyChange,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodySmall.copy(color = DevilColors.White),
                cursorBrush = SolidColor(DevilColors.Yellow),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("SAVE", enabled = keyInput.isNotBlank(), onClick = onSave)
            ActionChip("TEST", enabled = hasKey, onClick = onTest)
            if (hasKey) ActionChip("CLEAR", enabled = true, onClick = onClear, danger = true)
        }
    }
}

@Composable
private fun StatusChip(status: AiKeyStatus, text: String) {
    val colour = when (status) {
        AiKeyStatus.CONNECTED -> DevilColors.Yellow
        AiKeyStatus.ERROR -> DevilColors.RedText
        AiKeyStatus.CHECKING -> DevilColors.Muted
        AiKeyStatus.NOT_SET -> DevilColors.Muted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Ink)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, color = colour, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (danger) DevilColors.Ink else DevilColors.Card)
            .pressFeedback(haptic = false) { if (enabled) onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = when {
                !enabled -> DevilColors.Muted
                danger -> DevilColors.RedText
                else -> DevilColors.White
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
