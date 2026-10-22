package com.devil.calendar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.devil.calendar.ui.theme.LocalMotion

/**
 * The one press-feedback modifier used by every clickable in the app: a small
 * spring scale-down plus a light haptic.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    haptic: Boolean = true,
): Modifier {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = motion.springOf(),
        label = "pressScale",
    )

    LaunchedEffect(pressed) {
        if (pressed && haptic && enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable + [pressScale] sharing one interaction source. Use this for every
 * tappable surface so the feedback stays identical everywhere.
 */
@Composable
fun Modifier.pressFeedback(
    enabled: Boolean = true,
    haptic: Boolean = true,
    scaleDown: Float = 0.96f,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource = interaction, enabled = enabled, scaleDown = scaleDown, haptic = haptic)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
