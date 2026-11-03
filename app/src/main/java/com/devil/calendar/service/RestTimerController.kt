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
        /** True once the countdown reaches zero and starts counting up. */
        val overtime: Boolean = false,
        val overtimeStartMillis: Long = 0L,
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
        if (!current.active || current.overtime) return
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
    /**
     * The ring must never disappear on its own. At zero the chime fires once and
     * the state flips to overtime, counting up until the user starts the next
     * set or stops it explicitly.
     */
    fun onRestFinished() {
        val current = _state.value
        if (!current.active || current.overtime) return
        alerts.playChime()
        scheduler.cancel(AlertKind.REST)
        RestTimerService.stop(context)
        _state.value = current.copy(
            overtime = true,
            overtimeStartMillis = System.currentTimeMillis(),
            endMillis = 0L,
        )
    }

    /** "Start next set": clears the ring. */
    fun startNext() = finish()

    fun finish() {
        scheduler.cancel(AlertKind.REST)
        RestTimerService.stop(context)
        _state.value = RestState()
    }
}
