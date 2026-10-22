package com.devil.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/** Exposes the resolved motion budget to the whole tree. */
val LocalMotion = staticCompositionLocalOf {
    MotionConfig(scale = 1f, allowStagger = true, allowDecorative = true)
}

private val DarkScheme = darkColorScheme(
    primary = DevilColors.Red,
    onPrimary = DevilColors.Ink,
    secondary = DevilColors.Yellow,
    onSecondary = DevilColors.Ink,
    background = DevilColors.Ink,
    onBackground = DevilColors.White,
    surface = DevilColors.Card,
    onSurface = DevilColors.White,
    error = DevilColors.Red,
)

private val LightScheme = lightColorScheme(
    primary = DevilColors.Red,
    onPrimary = DevilColors.White,
    secondary = DevilColors.Yellow,
    onSecondary = DevilColors.Ink,
    background = DevilColors.White,
    onBackground = DevilColors.Ink,
    surface = DevilColors.Grey,
    onSurface = DevilColors.Ink,
    error = DevilColors.Red,
)

@Composable
fun DevilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val motion = rememberMotionConfig()
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalMotion provides motion) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DevilTypography,
            shapes = DevilShapes,
            content = content,
        )
    }
}
