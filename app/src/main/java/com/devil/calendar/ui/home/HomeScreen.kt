package com.devil.calendar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devil.calendar.ui.components.BentoCard
import com.devil.calendar.ui.components.CalendarGrid
import com.devil.calendar.ui.components.CircleButton
import com.devil.calendar.ui.components.ToolTile
import com.devil.calendar.ui.components.animatedEntrance
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.util.DateUtils
import java.io.File

@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit,
    onOpenTool: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 120.dp),
    ) {
        HomeHeader(
            name = state.profile.displayName,
            streak = state.streak,
            avatarPath = state.profile.avatarPath,
            initial = state.profile.initial,
            onOpenProfile = onOpenProfile,
            modifier = Modifier.animatedEntrance(0),
        )

        Spacer(Modifier.height(20.dp))
        GoalCard(
            goal = state.profile.displayGoal,
            modifier = Modifier.animatedEntrance(1),
        )

        Spacer(Modifier.height(12.dp))
        CalendarCard(
            state = state,
            onToggle = viewModel::toggle,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            modifier = Modifier.animatedEntrance(2),
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animatedEntrance(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolTile(
                label = "Timer",
                icon = Icons.Rounded.Timer,
                background = DevilColors.Yellow,
                onClick = { onOpenTool("timer") },
                modifier = Modifier.weight(1f),
            )
            ToolTile(
                label = "Alarm",
                icon = Icons.Rounded.Alarm,
                background = DevilColors.Grey,
                onClick = { onOpenTool("alarm") },
                modifier = Modifier.weight(1f),
            )
            ToolTile(
                label = "Focus",
                icon = Icons.Rounded.CenterFocusStrong,
                background = DevilColors.Red,
                onClick = { onOpenTool("focus") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    name: String,
    streak: Int,
    avatarPath: String?,
    initial: String,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "Good day,\n$name!",
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(DevilColors.Card)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DevilColors.Ink),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = streak.toString(),
                    color = DevilColors.Yellow,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Avatar(avatarPath = avatarPath, initial = initial, onClick = onOpenProfile)
        }
    }
}

@Composable
fun Avatar(
    avatarPath: String?,
    initial: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.titleMedium.fontSize,
) {
    val context = LocalContext.current
    val model = avatarPath?.let { File(it) }?.takeIf { it.exists() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Red)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = initial,
                color = DevilColors.Ink,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize),
            )
        }
    }
}

@Composable
private fun GoalCard(goal: String, modifier: Modifier = Modifier) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.Red,
        cornerRadius = DevilRadius.goal,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = "Your goal",
                color = DevilColors.Ink.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = goal,
                color = DevilColors.Ink,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun CalendarCard(
    state: HomeUiState,
    onToggle: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        color = DevilColors.White,
        cornerRadius = DevilRadius.card,
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(onClick = onPrevious) {
                    Text("\u2039", color = DevilColors.Ink, style = MaterialTheme.typography.headlineSmall)
                }
                Text(
                    text = DateUtils.monthTitle(state.month),
                    color = DevilColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                CircleButton(onClick = onNext) {
                    Text("\u203A", color = DevilColors.Ink, style = MaterialTheme.typography.headlineSmall)
                }
            }
            CalendarGrid(
                month = state.month,
                crossed = state.crossed,
                onToggle = onToggle,
            )
        }
    }
}
