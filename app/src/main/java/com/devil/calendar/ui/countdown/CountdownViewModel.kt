package com.devil.calendar.ui.countdown

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.service.AlertKind
import com.devil.calendar.service.AlertService
import com.devil.calendar.service.AlarmScheduler
import com.devil.calendar.service.CountdownService
import com.devil.calendar.service.FocusSessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CountdownMode(
    val defaultSeconds: Int,
    val stepSeconds: Int,
    val stopLabel: String,
    val startLabel: String,
    val kind: AlertKind,
    val label: String,
) {
    TIMER(
        defaultSeconds = 10 * 60,
        stepSeconds = 60,
        stopLabel = "RESET",
        startLabel = "START",
        kind = AlertKind.TIMER,
        label = "TIMER",
    ),
    FOCUS(
        defaultSeconds = 25 * 60,
        stepSeconds = 5 * 60,
        stopLabel = "END SESSION",
        startLabel = "START",
        kind = AlertKind.FOCUS,
        label = "FOCUS",
    );

    companion object {
        fun from(name: String?): CountdownMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TIMER
    }
}

data class CountdownUiState(
    val mode: CountdownMode = CountdownMode.TIMER,
    val durationSeconds: Int = 600,
    val leftSeconds: Int = 600,
    val running: Boolean = false,
) {
    val progress: Float
        get() = if (durationSeconds <= 0) 0f else (leftSeconds.toFloat() / durationSeconds).coerceIn(0f, 1f)
}

@HiltViewModel
class CountdownViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val scheduler: AlarmScheduler,
    private val focus: FocusSessionController,
) : ViewModel() {

    private val mode = CountdownMode.from(savedStateHandle.get<String>("mode"))

    private val _state = MutableStateFlow(
        CountdownUiState(
            mode = mode,
            durationSeconds = mode.defaultSeconds,
            leftSeconds = mode.defaultSeconds,
        ),
    )
    val state: StateFlow<CountdownUiState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var endMillis: Long = 0L

    /** +/- a step, clamped to 1 step … 120 minutes. */
    fun step(plus: Boolean) {
        if (_state.value.running) return
        val min = mode.stepSeconds
        val max = 120 * 60
        val current = _state.value.durationSeconds
        val next = if (plus) (current + mode.stepSeconds) else (current - mode.stepSeconds)
        val clamped = next.coerceIn(min, max)
        _state.update { it.copy(durationSeconds = clamped, leftSeconds = clamped) }
    }

    fun start() {
        val duration = _state.value.durationSeconds
        endMillis = System.currentTimeMillis() + duration * 1000L
        _state.update { it.copy(running = true, leftSeconds = duration) }

        // The real trigger is the absolute end timestamp — survives a kill.
        scheduler.schedule(mode.kind, endMillis, mode.label)
        CountdownService.start(context, mode.kind, endMillis, mode.label)
        if (mode == CountdownMode.FOCUS) focus.begin()

        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val left = ((endMillis - System.currentTimeMillis()) / 1000L).toInt()
                if (left <= 0) {
                    // The alarm fires the takeover; just settle the UI here.
                    _state.update { it.copy(leftSeconds = 0, running = false) }
                    if (mode == CountdownMode.FOCUS) focus.end()
                    break
                }
                _state.update { it.copy(leftSeconds = left) }
                delay(250)
            }
        }
    }

    /** Timer: reset. Focus: end the session and fire the takeover. */
    fun stop() {
        ticker?.cancel()
        scheduler.cancel(mode.kind, mode.label)
        CountdownService.stop(context, mode.kind)

        if (mode == CountdownMode.FOCUS) {
            focus.end()
            _state.update {
                it.copy(running = false, durationSeconds = mode.defaultSeconds, leftSeconds = mode.defaultSeconds)
            }
            // Fire the takeover to confirm the session is over.
            AlertService.fireNow(context, AlertKind.FOCUS, mode.label)
        } else {
            _state.update {
                it.copy(running = false, durationSeconds = mode.defaultSeconds, leftSeconds = mode.defaultSeconds)
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}
