package com.devil.calendar.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.local.AlarmEntity
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.data.repository.AlarmRepository
import com.devil.calendar.service.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    prefs: UserPreferences,
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = repository.alarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val use24Hour: StateFlow<Boolean> = prefs.use24Hour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _lastDeleted = MutableStateFlow<AlarmEntity?>(null)
    val lastDeleted: StateFlow<AlarmEntity?> = _lastDeleted.asStateFlow()

    /** Turns an alarm on (re-arming it) or off (cancelling it). */
    fun toggle(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val trigger = scheduler.scheduleAlarm(alarm.copy(enabled = true))
                repository.setEnabled(alarm.id, true, trigger)
            } else {
                scheduler.cancelAlarm(alarm.id)
                repository.setEnabled(alarm.id, false, 0L)
            }
        }
    }

    fun delete(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarm.id)
            repository.delete(alarm)
            _lastDeleted.value = alarm
        }
    }

    /** Undo for a swipe-to-delete. */
    fun undoDelete() {
        val alarm = _lastDeleted.value ?: return
        viewModelScope.launch {
            repository.save(alarm)
            if (alarm.enabled) {
                val trigger = scheduler.scheduleAlarm(alarm)
                repository.setEnabled(alarm.id, true, trigger)
            }
            _lastDeleted.value = null
        }
    }

    fun clearUndo() {
        _lastDeleted.value = null
    }
}
