package com.devil.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.LocalMotion
import kotlin.math.abs

/**
 * A snapping wheel column. Used for the timer's H/M/S and the alarm's H/M.
 *
 * The centered value is derived from the lazy list's scroll position and read
 * inside [graphicsLayer], so scrolling never recomposes the item content.
 *
 * @param itemCount how many distinct values exist
 * @param selectedIndex the value the caller wants centred
 * @param labelOf renders a value index as text
 * @param infinite loop the values endlessly (used for minutes and seconds)
 */
@Composable
fun WheelPicker(
    itemCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    labelOf: (Int) -> String,
    modifier: Modifier = Modifier,
    visibleItems: Int = 5,
    itemHeight: Dp = 46.dp,
    infinite: Boolean = false,
    suffix: String? = null,
) {
    if (itemCount <= 0) return

    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    // Repeating the values keeps the wheel endless without special-case maths.
    val multiplier = if (infinite) 400 else 1
    val total = itemCount * multiplier
    val baseOffset = if (infinite) (multiplier / 2) * itemCount else 0

    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = baseOffset + selectedIndex.coerceIn(0, itemCount - 1),
    )
    val fling = rememberSnapFlingBehavior(state, SnapPosition.Center)

    val centred by remember {
        derivedStateOf {
            val index = state.firstVisibleItemIndex
            if (state.firstVisibleItemScrollOffset > itemHeightPx / 2f) index + 1 else index
        }
    }
    val realIndex by remember {
        derivedStateOf {
            if (infinite) ((centred % itemCount) + itemCount) % itemCount
            else centred.coerceIn(0, itemCount - 1)
        }
    }

    // Report the settled value once, with a tick.
    LaunchedEffect(realIndex) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onSelected(realIndex)
    }

    // Follow external changes (preset chips, loading a saved alarm).
    LaunchedEffect(selectedIndex) {
        val target = baseOffset + selectedIndex.coerceIn(0, itemCount - 1)
        if (realIndex != selectedIndex) {
            state.animateScrollToItem(target)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Centre band behind the numbers.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(DevilColors.Card.copy(alpha = 0.55f)),
        )

        LazyColumn(
            state = state,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItems),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                vertical = itemHeight * (visibleItems / 2),
            ),
        ) {
            items(total) { virtualIndex ->
                val value = virtualIndex % itemCount
                val distance = abs(virtualIndex - centred)
                // Read in the draw phase only — no recomposition while flinging.
                val closeness = (1f - (distance / 2f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelOf(value),
                        color = DevilColors.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.graphicsLayer {
                            val scale = 0.72f + 0.28f * closeness
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.35f + 0.65f * closeness
                        },
                    )
                }
            }
        }

        if (suffix != null) {
            Text(
                text = suffix,
                color = DevilColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            )
        }
    }
}
