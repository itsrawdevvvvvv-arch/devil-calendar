package com.devil.calendar.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import android.app.Activity

/** Height of the floating nav pill plus its padding, used for scroll end spacing. */
private val navPillSpace = 110.dp

/** Top inset for screen titles: status bar + cutout + 8dp of breathing room. */
@Composable
fun topContentPadding(extra: androidx.compose.ui.unit.Dp = 8.dp): PaddingValues {
    val top = WindowInsets.statusBars.union(WindowInsets.displayCutout).asPaddingValues().calculateTopPadding()
    return PaddingValues(top = top + extra)
}

/** Bottom inset so scrollable content clears the nav pill and the nav bar. */
@Composable
fun navAwareBottomPadding(): androidx.compose.ui.unit.Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + navPillSpace

/**
 * Every screen is dark now, so the status bar always uses light icons. There is
 * no per-screen variant.
 */
@Composable
fun StatusBarAppearance() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    SideEffect {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
}
