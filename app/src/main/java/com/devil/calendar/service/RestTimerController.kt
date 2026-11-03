package com.devil.calendar.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the workout rest timer.
 *
 * The countdown itself is an absolute end timestamp backed by an exact
 * AlarmManager alert and a foreground service, so it keeps running with the
 * screen off or the app swiped away. The in-app ring just renders [state].
 */
@Singleton
class RestTimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: AlarmScheduler,
    private val alerts: AlertController,
) {
    data class RestState(
        val active: Boolean = false,
        val endMillis: Long = 0L,
        val totalSeconds: Int = 0,
        val exerciseName: String = "",
    )

    private val _state = MutableStateFlow(RestState())
    val state: StateFlow<RestState> = _state.asStateFlow()

    fun start(seconds: Int, exerciseName: String) {
        if (seconds <= 0) return
        val end = System.currentTimeMillis() + seconds * 1000L
        _state.value = RestState(
            active = true,
            endMillis = end,
            totalSeconds = seconds,
            exerciseName = exerciseName,
        )
        scheduler.schedule(AlertKind.REST, end, exerciseName, seconds.toLong())
        RestTimerService.start(context, end, exerciseName, seconds)
    }

    /** +15 / -15 while the timer runs. Re-arms the alert at the new end time. */
    fun adjust(deltaSeconds: Int) {
        val current = _state.value
        if (!current.active) return
        val remaining = ((current.endMillis - System.currentTimeMillis()) / 1000L).toInt()
        val next = (remaining + deltaSeconds).coerceAtLeast(0)
        if (next == 0) {
            onRestFinished()
            return
        }
        val end = System.currentTimeMillis() + next * 1000L
        _state.value = current.copy(
            endMillis = end,
            totalSeconds = maxOf(current.totalSeconds, next),
        )
        scheduler.schedule(AlertKind.REST, end, current.exerciseName, next.toLong())
        RestTimerService.start(context, end, current.exerciseName, _state.value.totalSeconds)
    }

    fun skip() = finish()

    /** Called by the alarm receiver when the rest actually ends. */
    fun onRestFinished() {
        alerts.playChime()
        finish()
    }

    fun finish() {
        scheduler.cancel(AlertKind.REST)
        RestTimerService.stop(context)
        _state.value = RestState()
    }
}
