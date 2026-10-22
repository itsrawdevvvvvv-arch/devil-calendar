package com.devil.calendar.ui.alarm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.local.AlarmEntity
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.data.repository.AlarmRepository
import com.devil.calendar.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class AlarmEditUiState(
    val id: Long = 0L,
    /** Always 24-hour internally. */
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val days: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val use24Hour: Boolean = false,
    val loaded: Boolean = false,
) {
    val isNew: Boolean get() = id == 0L
}

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    prefs: UserPreferences,
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>("alarmId") ?: -1L

    private val _state = MutableStateFlow(AlarmEditUiState())
    val state: StateFlow<AlarmEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.use24Hour.collect { use24 ->
                _state.update { it.copy(use24Hour = use24) }
            }
        }
        viewModelScope.launch {
            if (alarmId > 0L) {
                repository.byId(alarmId)?.let { alarm ->
                    _state.update {
                        it.copy(
                            id = alarm.id,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            label = alarm.label,
                            days = alarm.days,
                            enabled = alarm.enabled,
                            loaded = true,
                        )
                    }
                } ?: _state.update { it.copy(loaded = true) }
            } else {
                _state.update { it.copy(loaded = true) }
            }
        }
    }

    fun setHour24(hour: Int) = _state.update { it.copy(hour = hour.coerceIn(0, 23)) }
    fun setMinute(minute: Int) = _state.update { it.copy(minute = minute.coerceIn(0, 59)) }
    fun setLabel(label: String) = _state.update { it.copy(label = label.take(40)) }

    fun toggleDay(day: DayOfWeek) = _state.update { current ->
        val next = if (day in current.days) current.days - day else current.days + day
        current.copy(days = next)
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val entity = AlarmEntity(
                id = s.id,
                hour = s.hour,
                minute = s.minute,
                label = s.label,
                enabled = true,
                repeatDays = AlarmEntity.encodeDays(s.days),
            )
            val id = repository.save(entity)
            val saved = entity.copy(id = if (s.id == 0L) id else s.id)
            val trigger = scheduler.scheduleAlarm(saved)
            repository.setEnabled(saved.id, true, trigger)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _state.value.id
        viewModelScope.launch {
            if (id > 0L) {
                scheduler.cancelAlarm(id)
                repository.deleteById(id)
            }
            onDone()
        }
    }
}
