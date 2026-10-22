package com.devil.calendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.theme.LocalMotion
import com.devil.calendar.ui.theme.Motion

/** A flat bento card: solid colour, big radius, no shadow. */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    color: Color = DevilColors.Card,
    cornerRadius: Dp = DevilRadius.card,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalMotion.current
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = motion.tune(Motion.quick),
        label = "cardPress",
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

/** A round icon button used by the calendar header and tool tiles. */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = DevilColors.Grey,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = motion.tune(Motion.quick),
        label = "circle",
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The floating bottom navigation pill. */
@Composable
fun PillNav(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        NavItem("home", Icons.Rounded.GridView, "Home"),
        NavItem("stats", Icons.Rounded.BarChart, "Stats"),
        NavItem("profile", Icons.Rounded.Person, "Profile"),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(44.dp))
            .background(DevilColors.Card)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            val selected = item.route == current
            val motion = LocalMotion.current
            val bg by animateColorAsState(
                targetValue = if (selected) DevilColors.White else DevilColors.NavIdle,
                animationSpec = motion.tuneColor(160),
                label = "navBg",
            )
            val tint by animateColorAsState(
                targetValue = if (selected) DevilColors.Ink else DevilColors.NavIconIdle,
                animationSpec = motion.tuneColor(160),
                label = "navTint",
            )
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = motion.tune(Motion.quick),
                label = "navScale",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(30.dp))
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(item.route) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
    }
}

private data class NavItem(val route: String, val icon: ImageVector, val label: String)

/** A coloured tool tile: dark circle icon on top, label at the bottom. */
@Composable
fun ToolTile(
    label: String,
    icon: ImageVector,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(
        modifier = modifier.height(118.dp),
        color = background,
        cornerRadius = DevilRadius.tile,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = DevilColors.White, modifier = Modifier.size(24.dp))
            }
            Text(text = label, color = DevilColors.Ink, style = MaterialTheme.typography.titleSmall)
        }
    }
}
