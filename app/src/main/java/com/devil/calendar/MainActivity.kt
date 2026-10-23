package com.devil.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devil.calendar.service.FocusSessionController
import com.devil.calendar.ui.components.PillNav
import com.devil.calendar.ui.components.StatusBarAppearance
import com.devil.calendar.ui.main.MainViewModel
import com.devil.calendar.ui.nav.DevilNavHost
import com.devil.calendar.ui.nav.Routes
import com.devil.calendar.ui.onboarding.OnboardingScreen
import com.devil.calendar.ui.theme.DevilColors
import com.devil.calendar.ui.theme.DevilTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var focusController: FocusSessionController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before setContent so the window is edge-to-edge from the
        // first frame; screens then apply their own insets.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                focusController.active.collect { active ->
                    runCatching { if (active) startLockTask() else stopLockTask() }
                }
            }
        }

        setContent {
            DevilTheme(darkTheme = true) {
                DevilApp()
            }
        }
    }
}

@Composable
private fun DevilApp(viewModel: MainViewModel = hiltViewModel()) {
    val onboardingDone by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    when (onboardingDone) {
        null -> Unit
        false -> OnboardingScreen(onDone = viewModel::completeOnboarding)
        true -> MainShell()
    }
}

@Composable
private fun MainShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Routes.HOME
    val showNav = route in setOf(Routes.HOME, Routes.STATS, Routes.PROFILE)

    // One background for the whole app: ink, everywhere, always. No screen or
    // nav container paints a colour of its own, so there is never a split.
    StatusBarAppearance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DevilColors.Ink),
    ) {
        DevilNavHost(navController = navController)

        AnimatedVisibility(
            visible = showNav,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            PillNav(
                current = route,
                onSelect = { target ->
                    if (target != route) {
                        navController.navigate(target) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
