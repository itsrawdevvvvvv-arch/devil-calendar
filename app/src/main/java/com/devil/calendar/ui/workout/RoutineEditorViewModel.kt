package com.devil.calendar.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.local.workout.ExerciseMode
import com.devil.calendar.data.repository.SessionRepository
import com.devil.calendar.data.repository.WorkoutRepository
import com.devil.calendar.domain.workout.RoutineDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val sessions: SessionRepository,
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<Long>("routineId") ?: 0L

    val routine: StateFlow<RoutineDetail?> = repository.routine(routineId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _newName = MutableStateFlow("")
    val newName: StateFlow<String> = _newName.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private var suggestJob: Job? = null

    fun onNewNameChange(value: String) {
        _newName.value = value
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            // Small debounce so typing does not hammer the database.
            delay(180)
            _suggestions.value = repository.suggestNames(value)
                .filterNot { it.equals(value.trim(), ignoreCase = true) }
        }
    }

    fun addExercise(name: String = _newName.value) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addExerciseByName(routineId, trimmed)
            _newName.value = ""
            _suggestions.value = emptyList()
        }
    }

    fun rename(name: String) {
        viewModelScope.launch { repository.renameRoutine(routineId, name) }
    }

    fun removeLine(lineId: Long) {
        viewModelScope.launch { repository.removeRoutineExercise(lineId) }
    }

    fun move(lineId: Long, delta: Int) {
        viewModelScope.launch { repository.moveRoutineExercise(routineId, lineId, delta) }
    }

    fun setRest(lineId: Long, seconds: Int) {
        viewModelScope.launch { repository.setRestSeconds(lineId, seconds) }
    }

    fun setSets(lineId: Long, sets: Int) {
        viewModelScope.launch { repository.setDefaultSets(lineId, sets) }
    }

    fun setMode(lineId: Long, mode: ExerciseMode, holdSeconds: Int) {
        viewModelScope.launch { repository.setMode(lineId, mode, holdSeconds) }
    }

    fun startSession(onStarted: () -> Unit) {
        viewModelScope.launch {
            sessions.startSession(routineId)
            onStarted()
        }
    }

    fun duplicate(onDone: (Long) -> Unit) {
        viewModelScope.launch {
            repository.duplicateRoutine(routineId)?.let(onDone)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            onDone()
        }
    }
}
