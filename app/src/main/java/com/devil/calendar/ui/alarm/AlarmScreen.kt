package com.devil.calendar.ui.alarm

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.components.BentoCard

@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().animatedEntrance(0)) {
            Text(
                text = "\u2039 back",
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "ALARM",
            color = DevilColors.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.animatedEntrance(1),
        )

        Spacer(Modifier.height(40.dp))
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .animatedEntrance(2),
            color = DevilColors.Card,
            cornerRadius = DevilRadius.card,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%02d:%02d".format(state.hour, state.minute),
                    color = DevilColors.White,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier
                        .padding(4.dp)
                        .then(Modifier),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = state.status,
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(22.dp))
        PillButton(
            text = if (state.armed) "CANCEL ALARM" else "SET ALARM",
            onClick = viewModel::toggle,
            modifier = Modifier.animatedEntrance(3),
        )

        Spacer(Modifier.height(12.dp))
        // Simple time picker entry point.
        PillButton(
            text = "PICK TIME",
            onClick = {
                TimePickerDialog(
                    context,
                    { _, h, m -> viewModel.setTime(h, m) },
                    state.hour,
                    state.minute,
                    true,
                ).show()
            },
            background = DevilColors.Card,
            textColor = DevilColors.White,
            modifier = Modifier.animatedEntrance(4),
        )
    }
}
