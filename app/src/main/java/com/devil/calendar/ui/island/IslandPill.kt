package com.devil.calendar.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devil.calendar.service.island.IslandController
import com.devil.calendar.service.island.IslandKind
import com.devil.calendar.service.island.IslandMode
import com.devil.calendar.service.island.IslandSource
import com.devil.calendar.service.island.IslandState
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilTheme
import com.devil.calendar.ui.theme.LocalMotion
import kotlinx.coroutines.delay

/** Entry point used by the overlay. */
@Composable
fun IslandPillHost(controller: IslandController) {
    val state by controller.state.collectAsState()
    DevilTheme(darkTheme = true) {
        IslandPill(
            state = state,
            onTap = { controller.toggleExpanded() },
            onNext = { controller.next() },
            onPrevious = { controller.previous() },
        )
    }
}

@Composable
fun IslandPill(
    state: IslandState,
    onTap: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val motion = LocalMotion.current
    val mode = state.mode

    val width = when (mode) {
        IslandMode.IDLE -> 62.dp
        IslandMode.COMPACT -> 208.dp
        IslandMode.EXPANDED -> 312.dp
    }
    val height = when (mode) {
        IslandMode.IDLE -> 26.dp
        IslandMode.COMPACT -> 36.dp
        IslandMode.EXPANDED -> 148.dp
    }
    val corner = when (mode) {
        IslandMode.IDLE -> 13.dp
        IslandMode.COMPACT -> 18.dp
        IslandMode.EXPANDED -> 28.dp
    }

    // The liquid morph: width, height and radius all spring together.
    val animatedWidth by animateDpAsState(width, motion.springOf(), label = "islandW")
    val animatedHeight by animateDpAsState(height, motion.springOf(), label = "islandH")
    val animatedCorner by animateDpAsState(corner, motion.springOf(), label = "islandR")

    val shape = RoundedCornerShape(animatedCorner)

    Box(
        modifier = Modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .clip(shape)
            .background(DevilColors.Ink)
            .border(1.dp, DevilColors.White.copy(alpha = 0.10f), shape)
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .pointerInput(state.index) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragEnd = { dragged = 0f },
                    onHorizontalDrag = { _, amount ->
                        dragged += amount
                        if (dragged > SWIPE_THRESHOLD) {
                            onPrevious()
                            dragged = 0f
                        } else if (dragged < -SWIPE_THRESHOLD) {
                            onNext()
                            dragged = 0f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(motion.tweenOf(140)) togetherWith fadeOut(motion.tweenOf(100)) },
            label = "islandContent",
        ) { target ->
            when (target) {
                IslandMode.IDLE -> IdleDot()
                IslandMode.COMPACT -> state.current?.let { CompactPill(it) }
                IslandMode.EXPANDED -> state.current?.let { ExpandedPill(it, state.hasMultiple) }
            }
        }
    }
}

@Composable
private fun IdleDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(DevilColors.Red),
    )
}

@Composable
private fun CompactPill(source: IslandSource) {
    val live = liveValue(source)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(source.kind),
            contentDescription = null,
            tint = DevilColors.RedText,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = source.title,
            color = DevilColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = live,
            color = DevilColors.White,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun ExpandedPill(source: IslandSource, hasMultiple: Boolean) {
    val live = liveValue(source)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = iconFor(source.kind),
                contentDescription = null,
                tint = DevilColors.RedText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = source.title,
                color = DevilColors.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (hasMultiple) {
                Text(
                    text = "swipe",
                    color = DevilColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            text = live,
            color = DevilColors.White,
            style = MaterialTheme.typography.headlineLarge,
        )
        source.subtitle.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val progress = source.progress
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DevilColors.RingTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(DevilColors.RedText),
                )
            }
        }
    }
}

/** Ticks only while a countdown is running. */
@Composable
private fun liveValue(source: IslandSource): String {
    val end = source.endMillis
    if (end == null) return source.value
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val remaining = (end - now).coerceAtLeast(0L)
    return "%d:%02d".format(remaining / 60_000L, (remaining / 1_000L) % 60)
}

private fun iconFor(kind: IslandKind): ImageVector = when (kind) {
    IslandKind.CALL -> Icons.Rounded.Call
    IslandKind.TIMER -> Icons.Rounded.Timer
    IslandKind.REST -> Icons.Rounded.FitnessCenter
    IslandKind.MUSIC -> Icons.Rounded.MusicNote
    IslandKind.OTHER -> Icons.Rounded.Notifications
}

private const val SWIPE_THRESHOLD = 48f
