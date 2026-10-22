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
    val kind: AlertKind,
    val label: String,
    val defaultHours: Int,
    val defaultMinutes: Int,
    val defaultSeconds: Int,
    val showSeconds: Boolean,
) {
    TIMER(
        kind = AlertKind.TIMER,
        label = "TIMER",
        defaultHours = 0,
        defaultMinutes = 5,
        defaultSeconds = 0,
        showSeconds = true,
    ),
    FOCUS(
        kind = AlertKind.FOCUS,
        label = "FOCUS",
        defaultHours = 0,
        defaultMinutes = 25,
        defaultSeconds = 0,
        showSeconds = false,
    );

    companion object {
        fun from(name: String?): CountdownMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TIMER
    }
}

/** Preset chips, in milliseconds. */
val CountdownPresets: List<Pair<String, Long>> = listOf(
    "30s" to 30_000L,
    "1m" to 60_000L,
    "5m" to 300_000L,
    "10m" to 600_000L,
    "25m" to 1_500_000L,
    "1h" to 3_600_000L,
)

data class CountdownUiState(
    val mode: CountdownMode = CountdownMode.TIMER,
    val hours: Int = 0,
    val minutes: Int = 5,
    val seconds: Int = 0,
    val running: Boolean = false,
    val paused: Boolean = false,
    val leftMillis: Long = 0L,
    val totalMillis: Long = 0L,
) {
    val pickedMillis: Long
        get() = (hours * 3600L + minutes * 60L + seconds) * 1000L

    val canStart: Boolean get() = pickedMillis > 0L

    val progress: Float
        get() = if (totalMillis <= 0L) 0f else (leftMillis.toFloat() / totalMillis).coerceIn(0f, 1f)

    /** hh:mm:ss while running, hiding the hour field when it is zero. */
    val display: String
        get() {
            val total = ((leftMillis + 999) / 1000).coerceAtLeast(0)
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }
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
            hours = mode.defaultHours,
            minutes = mode.defaultMinutes,
            seconds = mode.defaultSeconds,
        ),
    )
    val state: StateFlow<CountdownUiState> = _state.asStateFlow()

    private var ticker: Job? = null

    /** Absolute end timestamp. The single source of truth while running. */
    private var endMillis: Long = 0L

    fun setHours(value: Int) = updatePicker { it.copy(hours = value.coerceIn(0, 23)) }
    fun setMinutes(value: Int) = updatePicker { it.copy(minutes = value.coerceIn(0, 59)) }
    fun setSeconds(value: Int) = updatePicker { it.copy(seconds = value.coerceIn(0, 59)) }

    fun applyPreset(millis: Long) {
        if (_state.value.running) return
        val totalSeconds = (millis / 1000).toInt()
        _state.update {
            it.copy(
                hours = totalSeconds / 3600,
                minutes = (totalSeconds % 3600) / 60,
                seconds = totalSeconds % 60,
            )
        }
    }

    private inline fun updatePicker(block: (CountdownUiState) -> CountdownUiState) {
        if (_state.value.running) return
        _state.update(block)
    }

    fun start() {
        val total = _state.value.pickedMillis
        if (total <= 0L) return
        endMillis = System.currentTimeMillis() + total
        _state.update { it.copy(running = true, paused = false, leftMillis = total, totalMillis = total) }
        arm()
        startTicker()
    }

    fun pause() {
        if (!_state.value.running || _state.value.paused) return
        val left = (endMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        ticker?.cancel()
        disarm()
        _state.update { it.copy(paused = true, leftMillis = left) }
    }

    fun resume() {
        val s = _state.value
        if (!s.running || !s.paused) return
        endMillis = System.currentTimeMillis() + s.leftMillis
        _state.update { it.copy(paused = false) }
        arm()
        startTicker()
    }

    /** Timer: reset back to the picker. Focus: end the session and fire the takeover. */
    fun reset() {
        ticker?.cancel()
        disarm()
        if (mode == CountdownMode.FOCUS) {
            focus.end()
            _state.update {
                it.copy(
                    running = false,
                    paused = false,
                    leftMillis = 0L,
                    totalMillis = 0L,
                    hours = mode.defaultHours,
                    minutes = mode.defaultMinutes,
                    seconds = mode.defaultSeconds,
                )
            }
            AlertService.fireNow(context, AlertKind.FOCUS, mode.label)
        } else {
            _state.update {
                it.copy(
                    running = false,
                    paused = false,
                    leftMillis = 0L,
                    totalMillis = 0L,
                    hours = mode.defaultHours,
                    minutes = mode.defaultMinutes,
                    seconds = mode.defaultSeconds,
                )
            }
        }
    }

    private fun arm() {
        scheduler.schedule(mode.kind, endMillis, mode.label)
        CountdownService.start(context, mode.kind, endMillis, mode.label)
        if (mode == CountdownMode.FOCUS) focus.begin()
    }

    private fun disarm() {
        scheduler.cancel(mode.kind, mode.label)
        CountdownService.stop(context, mode.kind)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val left = endMillis - System.currentTimeMillis()
                if (left <= 0L) {
                    _state.update { it.copy(leftMillis = 0L, running = false, paused = false) }
                    if (mode == CountdownMode.FOCUS) focus.end()
                    break
                }
                _state.update { it.copy(leftMillis = left) }
                delay(250)
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}
