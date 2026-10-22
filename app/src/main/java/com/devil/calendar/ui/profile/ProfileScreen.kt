package com.devil.calendar.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.home.Avatar
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setAvatar) }

    // Local editable text, seeded from the saved profile once.
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var goal by remember(profile.goal) { mutableStateOf(profile.goal) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 120.dp),
    ) {
        Text(
            text = "Your\nProfile",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.animatedEntrance(0),
        )

        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animatedEntrance(1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                avatarPath = profile.avatarPath,
                initial = profile.initial,
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                size = 130.dp,
                fontSize = MaterialTheme.typography.displaySmall.fontSize,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DevilRadius.button))
                    .background(DevilColors.Yellow)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "Upload photo",
                    color = DevilColors.Ink,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        ProfileField(
            label = "Name",
            value = name,
            placeholder = "Your name",
            maxLength = 20,
            onValueChange = {
                name = it
                viewModel.setName(it)
            },
            modifier = Modifier.animatedEntrance(2),
        )
        Spacer(Modifier.height(16.dp))
        ProfileField(
            label = "Goal",
            value = goal,
            placeholder = "e.g. Wake up at 5 AM",
            maxLength = 60,
            onValueChange = {
                goal = it
                viewModel.setGoal(it)
            },
            modifier = Modifier.animatedEntrance(3),
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    placeholder: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
}
