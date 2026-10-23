package com.devil.calendar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilRadius
import com.devil.calendar.ui.theme.LocalMotion

/** A flat bento card: solid colour, big radius, no shadow. */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    color: Color = DevilColors.Card,
    cornerRadius: Dp = DevilRadius.card,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val base = if (onClick != null) {
        modifier.pressFeedback(onClick = onClick)
    } else {
        modifier
    }
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = base
            .clip(shape)
            .background(color)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier,
            ),
    ) {
        content()
    }
}

/** A round icon button used by the calendar header and step controls. */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = DevilColors.Grey,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(background)
            .pressFeedback(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private data class NavItem(val route: String, val icon: ImageVector, val label: String)

/**
 * The floating bottom navigation. The white pill is a single indicator that
 * slides between tabs on a spring; icon colours crossfade.
 */
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
    val selectedIndex = items.indexOfFirst { it.route == current }.coerceAtLeast(0)
    val motion = LocalMotion.current
    val shape = RoundedCornerShape(44.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DevilColors.Card)
            .border(2.dp, Color(0xFF111111), shape)
            .padding(6.dp),
    ) {
        val itemWidth = maxWidth / items.size
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = motion.springOf(),
            label = "navIndicator",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(itemWidth)
                .height(56.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(DevilColors.White),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val tint by animateColorAsState(
                    targetValue = if (selected) DevilColors.Ink else DevilColors.NavIconIdle,
                    animationSpec = motion.tuneColor(160),
                    label = "navTint",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .pressFeedback(onClick = { onSelect(item.route) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

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
