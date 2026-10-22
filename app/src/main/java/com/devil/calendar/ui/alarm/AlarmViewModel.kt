package com.devil.calendar.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.local.AlarmEntity
import com.devil.calendar.data.repository.AlarmRepository
import com.devil.calendar.service.AlertKind
import com.devil.calendar.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class AlarmUiState(
    val hour: Int = 7,
    val minute: Int = 0,
    val armed: Boolean = false,
    val status: String = "Not set",
    val canScheduleExact: Boolean = true,
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmUiState())
    val state: StateFlow<AlarmUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(canScheduleExact = scheduler.canScheduleExact()) }
        viewModelScope.launch {
            repository.firstEnabled()?.let { alarm ->
                _state.update {
                    it.copy(
                        hour = alarm.hour,
                        minute = alarm.minute,
                        armed = true,
                        status = "Rings at ${format(alarm.hour, alarm.minute)}",
                    )
                }
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _state.update {
            it.copy(
                hour = hour,
                minute = minute,
                status = if (it.armed) "Rings at ${format(hour, minute)}" else "Not set",
            )
        }
    }

    fun toggle() {
        val current = _state.value
        if (current.armed) cancel() else arm()
    }

    private fun arm() {
        val s = _state.value
        val trigger = nextTrigger(s.hour, s.minute)
        scheduler.schedule(AlertKind.ALARM, trigger, "")
        viewModelScope.launch {
            repository.save(
                AlarmEntity(hour = s.hour, minute = s.minute, enabled = true, triggerAtMillis = trigger),
            )
        }
        _state.update {
            it.copy(
                armed = true,
                status = "Rings at ${format(s.hour, s.minute)}",
                canScheduleExact = scheduler.canScheduleExact(),
            )
        }
    }

    private fun cancel() {
        scheduler.cancel(AlertKind.ALARM)
        viewModelScope.launch {
            repository.firstEnabled()?.let { repository.delete(it) }
        }
        _state.update { it.copy(armed = false, status = "Not set") }
    }

    private fun nextTrigger(hour: Int, minute: Int): Long {
        var date = LocalDate.now()
        var time = LocalDateTime.of(date, LocalTime.of(hour, minute))
        if (time.isBefore(LocalDateTime.now())) {
            date = date.plusDays(1)
            time = LocalDateTime.of(date, LocalTime.of(hour, minute))
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun format(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
}
