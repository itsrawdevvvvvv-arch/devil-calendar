package com.devil.calendar.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.devil.calendar.ui.components.PillButton
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.components.navAwareBottomPadding
import com.devil.calendar.ui.components.pressFeedback
import com.devil.calendar.ui.components.topContentPadding
import com.devil.calendar.ui.home.Avatar
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.util.Permissions

@Composable
fun ProfileScreen(
    onOpenAlarms: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val use24Hour by viewModel.use24Hour.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::setAvatar) }

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var goal by remember(profile.goal) { mutableStateOf(profile.goal) }

    val exactAlarmOk = remember { Permissions.hasExactAlarm(context) }
    val fullScreenOk = remember { Permissions.hasFullScreenIntent(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(topContentPadding())
            .padding(horizontal = 18.dp)
            .padding(bottom = navAwareBottomPadding()),
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
                    .pressFeedback {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
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

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Settings",
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.animatedEntrance(4),
        )
        Spacer(Modifier.height(8.dp))

        SettingsRow(
            title = "24-hour clock",
            subtitle = if (use24Hour) "Alarms show 13:45" else "Alarms show 1:45 PM",
            checked = use24Hour,
            onCheckedChange = viewModel::setUse24Hour,
            modifier = Modifier.animatedEntrance(5),
        )

        Spacer(Modifier.height(10.dp))
        SettingsAction(
            title = "Alarms",
            subtitle = "Add, edit and delete alarms",
            onClick = onOpenAlarms,
            modifier = Modifier.animatedEntrance(6),
        )

        Spacer(Modifier.height(10.dp))
        SettingsAction(
            title = "Test alarm in 5 seconds",
            subtitle = "Uses the same path as a real alarm",
            onClick = viewModel::testAlarm,
            modifier = Modifier.animatedEntrance(7),
        )

        if (!exactAlarmOk || !fullScreenOk) {
            Spacer(Modifier.height(16.dp))
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animatedEntrance(8),
                color = DevilColors.Red,
                cornerRadius = DevilRadius.card,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Alarms need two permissions",
                        color = DevilColors.Ink,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            if (!exactAlarmOk) append("Allow exact alarms. ")
                            if (!fullScreenOk) append("Allow full-screen notifications.")
                        },
                        color = DevilColors.Ink.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    PillButton(
                        text = "GRANT",
                        onClick = {
                            val intent = if (!exactAlarmOk) {
                                Permissions.exactAlarmIntent(context)
                            } else {
                                Permissions.fullScreenIntentSettings(context)
                            }
                            runCatching { context.startActivity(intent) }
                        },
                        background = DevilColors.Ink,
                        textColor = DevilColors.White,
                        height = 48.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        cornerRadius = DevilRadius.card,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DevilColors.White, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = DevilColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DevilColors.Ink,
                    checkedTrackColor = DevilColors.Yellow,
                    uncheckedThumbColor = DevilColors.White,
                    uncheckedTrackColor = DevilColors.NavIdle,
                ),
            )
        }
    }
}

@Composable
private fun SettingsAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Card,
        cornerRadius = DevilRadius.card,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(title, color = DevilColors.White, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = DevilColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        targetValue = if (focused) DevilColors.Red else DevilColors.Card,
        label = "fieldBorder",
    )
    val lift by animateDpAsState(
        targetValue = if (focused) 2.dp else 0.dp,
        label = "fieldLift",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp + lift)
                .clip(RoundedCornerShape(DevilRadius.field))
                .background(DevilColors.Card)
                .border(2.dp, border, RoundedCornerShape(DevilRadius.field))
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
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
