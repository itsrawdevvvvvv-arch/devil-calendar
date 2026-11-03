package com.devil.calendar.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.WorkoutRepository
import com.devil.calendar.domain.workout.ExerciseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<Long>("routineId") ?: 0L
    private val query = MutableStateFlow("")

    val exercises: StateFlow<List<ExerciseItem>> = query
        .flatMapLatest { repository.exercises(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun addToRoutine(exerciseId: Long) {
        viewModelScope.launch { repository.addExercise(routineId, exerciseId) }
    }

    fun createCustom(name: String, muscleGroup: String, onDone: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCustomExercise(name, muscleGroup)
            onDone()
        }
    }

    fun deleteCustom(id: Long) {
        viewModelScope.launch { repository.deleteCustomExercise(id) }
    }
}
