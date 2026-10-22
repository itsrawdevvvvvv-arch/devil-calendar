package com.devil.calendar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Very large rounded corners, no gradients, no shadows — flat by design. */
val DevilShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

object DevilRadius {
    val card = 32.dp
    val goal = 30.dp
    val tile = 28.dp
    val field = 20.dp
    val pill = 40.dp
    val button = 34.dp
    val cell = 14.dp
    val circle = 50
}
