package com.devil.calendar.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.WorkoutRepository
import com.devil.calendar.domain.workout.RoutineDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<Long>("routineId") ?: 0L

    val routine: StateFlow<RoutineDetail?> = repository.routine(routineId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) {
        viewModelScope.launch { repository.renameRoutine(routineId, name) }
    }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch { repository.addExercise(routineId, exerciseId) }
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
