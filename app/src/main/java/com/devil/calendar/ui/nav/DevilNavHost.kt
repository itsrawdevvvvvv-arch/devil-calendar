package com.devil.calendar.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.devil.calendar.ui.alarm.AlarmScreen
import com.devil.calendar.ui.countdown.CountdownScreen
import com.devil.calendar.ui.home.HomeScreen
import com.devil.calendar.ui.profile.ProfileScreen
import com.devil.calendar.ui.stats.StatsScreen

object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val TIMER = "timer"
    const val FOCUS = "focus"
    const val ALARM = "alarm"
}

@Composable
fun DevilNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(tween(280)) { it / 6 } + fadeIn(tween(280))
        },
        exitTransition = {
            slideOutHorizontally(tween(240)) { -it / 8 } + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(tween(240)) { -it / 8 } + fadeIn(tween(240))
        },
        popExitTransition = {
            slideOutHorizontally(tween(280)) { it / 6 } + fadeOut(tween(200))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenTool = { tool ->
                    when (tool) {
                        "timer" -> navController.navigate(Routes.TIMER)
                        "alarm" -> navController.navigate(Routes.ALARM)
                        "focus" -> navController.navigate(Routes.FOCUS)
                    }
                },
            )
        }
        composable(Routes.STATS) { StatsScreen() }
        composable(Routes.PROFILE) { ProfileScreen() }
        composable(Routes.TIMER) { CountdownScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FOCUS) { CountdownScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ALARM) { AlarmScreen(onBack = { navController.popBackStack() }) }
    }
}
