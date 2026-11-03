package com.devil.calendar.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devil.calendar.ui.alarm.AlarmEditScreen
import com.devil.calendar.ui.alarm.AlarmScreen
import com.devil.calendar.ui.countdown.CountdownScreen
import com.devil.calendar.ui.home.HomeScreen
import com.devil.calendar.ui.island.IslandSetupScreen
import com.devil.calendar.ui.profile.ProfileScreen
import com.devil.calendar.ui.stats.StatsScreen
import com.devil.calendar.ui.workout.RoutineEditorScreen
import com.devil.calendar.ui.coach.CameraCoachScreen
import com.devil.calendar.ui.workout.AiImportScreen
import com.devil.calendar.ui.workout.ExerciseProgressScreen
import com.devil.calendar.ui.workout.HistoryScreen
import com.devil.calendar.ui.workout.SessionDetailScreen
import com.devil.calendar.ui.workout.SessionScreen
import com.devil.calendar.ui.workout.WorkoutHomeScreen

object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val WORKOUT = "workout"
    const val WORKOUT_ROUTINE = "workout/routine/{routineId}"
    const val WORKOUT_SESSION = "workout/session"
    const val ISLAND_SETUP = "island/setup"
    const val AI_IMPORT = "workout/ai-import"
    const val CAMERA_COACH = "coach/{exerciseName}/{setId}/{restSeconds}"
    const val WORKOUT_HISTORY = "workout/history"
    const val WORKOUT_HISTORY_DETAIL = "workout/history/{sessionId}"
    const val WORKOUT_PROGRESS = "workout/progress/{exerciseId}"
    const val ALARM = "alarm"
    const val ALARM_EDIT = "alarm/edit/{alarmId}"
    const val COUNTDOWN = "countdown/{mode}"
    const val TIMER = "countdown/TIMER"
    const val FOCUS = "countdown/FOCUS"

    const val NEW_ALARM = -1L
    fun alarmEdit(alarmId: Long = NEW_ALARM) = "alarm/edit/$alarmId"
    fun workoutRoutine(routineId: Long) = "workout/routine/$routineId"
    fun workoutHistoryDetail(sessionId: Long) = "workout/history/$sessionId"
    fun workoutProgress(exerciseId: Long) = "workout/progress/$exerciseId"
}

/** Used only to decide the slide direction of a transition. */
private val depth = mapOf(
    Routes.HOME to 0,
    Routes.STATS to 1,
    Routes.WORKOUT to 1,
    Routes.PROFILE to 2,
    Routes.WORKOUT_ROUTINE to 3,
    Routes.WORKOUT_SESSION to 4,
    Routes.ISLAND_SETUP to 3,
    Routes.AI_IMPORT to 3,
    Routes.CAMERA_COACH to 4,
    Routes.WORKOUT_HISTORY to 3,
    Routes.WORKOUT_HISTORY_DETAIL to 4,
    Routes.WORKOUT_PROGRESS to 5,
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
        composable(Routes.WORKOUT) {
            WorkoutHomeScreen(
                onOpenRoutine = { id -> navController.navigate(Routes.workoutRoutine(id)) },
                onSessionStarted = { navController.navigate(Routes.WORKOUT_SESSION) },
                onOpenHistory = { navController.navigate(Routes.WORKOUT_HISTORY) },
                onOpenAiImport = { navController.navigate(Routes.AI_IMPORT) },
            )
        }
        composable(
            route = Routes.WORKOUT_ROUTINE,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType }),
        ) { entry ->
            RoutineEditorScreen(
                onBack = { navController.popBackStack() },
                onStartSession = { navController.navigate(Routes.WORKOUT_SESSION) },
            )
        }
        composable(Routes.AI_IMPORT) {
            AiImportScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    navController.popBackStack()
                    navController.navigate(Routes.workoutRoutine(id))
                },
            )
        }
        composable(
            route = Routes.CAMERA_COACH,
            arguments = listOf(
                navArgument("exerciseName") { type = NavType.StringType },
                navArgument("setId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("restSeconds") { type = NavType.IntType; defaultValue = 90 },
            ),
        ) {
            CameraCoachScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WORKOUT_HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenSession = { id -> navController.navigate(Routes.workoutHistoryDetail(id)) },
            )
        }
        composable(
            route = Routes.WORKOUT_HISTORY_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
        ) {
            SessionDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenExercise = { id -> navController.navigate(Routes.workoutProgress(id)) },
            )
        }
        composable(
            route = Routes.WORKOUT_PROGRESS,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
        ) {
            ExerciseProgressScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WORKOUT_SESSION) {
            SessionScreen(
                onBack = { navController.popBackStack() },
                onOpenCoach = { name, setId, rest ->
                    navController.navigate("coach/${Uri.encode(name)}/$setId/$rest")
                },
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onOpenAlarms = { navController.navigate(Routes.ALARM) },
                onOpenIslandSetup = { navController.navigate(Routes.ISLAND_SETUP) },
            )
        }
        composable(Routes.ISLAND_SETUP) {
            IslandSetupScreen(onBack = { navController.popBackStack() })
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
