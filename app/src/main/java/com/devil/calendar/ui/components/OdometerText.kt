package com.devil.calendar.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.devil.calendar.ui.theme.LocalMotion

/**
 * Digits roll rather than swap: each character is its own AnimatedContent, so
 * a changing second slides up while the rest of the number stays put.
 */
@Composable
fun OdometerText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    Row(modifier = modifier) {
        text.forEachIndexed { index, character ->
            AnimatedContent(
                targetState = character,
                transitionSpec = {
                    (
                        slideInVertically(motion.tweenOf(180)) { height -> height } +
                            fadeIn(motion.tweenOf(180))
                        ) togetherWith (
                        slideOutVertically(motion.tweenOf(180)) { height -> -height } +
                            fadeOut(motion.tweenOf(180))
                        )
                },
                label = "digit$index",
            ) { target ->
                Text(text = target.toString(), style = style, color = color)
            }
        }
    }
}
