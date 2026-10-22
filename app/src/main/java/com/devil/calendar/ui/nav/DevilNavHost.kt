package com.devil.calendar.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devil.calendar.ui.alarm.AlarmEditScreen
import com.devil.calendar.ui.alarm.AlarmScreen
import com.devil.calendar.ui.countdown.CountdownScreen
import com.devil.calendar.ui.home.HomeScreen
import com.devil.calendar.ui.profile.ProfileScreen
import com.devil.calendar.ui.stats.StatsScreen

object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val ALARM = "alarm"
    const val ALARM_EDIT = "alarm/edit/{alarmId}"
    const val COUNTDOWN = "countdown/{mode}"
    const val TIMER = "countdown/TIMER"
    const val FOCUS = "countdown/FOCUS"

    const val NEW_ALARM = -1L
    fun alarmEdit(alarmId: Long = NEW_ALARM) = "alarm/edit/$alarmId"
}

/** Used only to decide the slide direction of a transition. */
private val depth = mapOf(
    Routes.HOME to 0,
    Routes.STATS to 1,
    Routes.PROFILE to 2,
    Routes.ALARM to 3,
    Routes.ALARM_EDIT to 4,
    Routes.COUNTDOWN to 4,
)

private fun depthOf(route: String?): Int = depth[route] ?: 0

@Composable
fun DevilNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        // Fade plus a slight, direction-aware slide.
        enterTransition = {
            val forward = depthOf(targetState.destination.route) >= depthOf(initialState.destination.route)
            slideInHorizontally(tween(300)) { if (forward) it / 8 else -it / 8 } + fadeIn(tween(300))
        },
        exitTransition = {
            val forward = depthOf(targetState.destination.route) >= depthOf(initialState.destination.route)
            slideOutHorizontally(tween(300)) { if (forward) -it / 8 else it / 8 } + fadeOut(tween(300))
        },
        popEnterTransition = {
            val forward = depthOf(targetState.destination.route) >= depthOf(initialState.destination.route)
            slideInHorizontally(tween(300)) { if (forward) it / 8 else -it / 8 } + fadeIn(tween(300))
        },
        popExitTransition = {
            val forward = depthOf(targetState.destination.route) >= depthOf(initialState.destination.route)
            slideOutHorizontally(tween(300)) { if (forward) -it / 8 else it / 8 } + fadeOut(tween(300))
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
        composable(Routes.PROFILE) {
            ProfileScreen(onOpenAlarms = { navController.navigate(Routes.ALARM) })
        }
        composable(
            route = Routes.COUNTDOWN,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) {
            CountdownScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ALARM) {
            AlarmScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.alarmEdit(id)) },
            )
        }
        composable(
            route = Routes.ALARM_EDIT,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType }),
        ) {
            AlarmEditScreen(onBack = { navController.popBackStack() })
        }
    }
}
