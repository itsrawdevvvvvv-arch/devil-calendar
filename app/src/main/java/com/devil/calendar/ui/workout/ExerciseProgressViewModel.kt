package com.devil.calendar.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.HistoryRepository
import com.devil.calendar.domain.workout.ProgressPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val name: String = "",
    val points: List<ProgressPoint> = emptyList(),
)

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
) : ViewModel() {

    private val exerciseId: Long = savedStateHandle.get<Long>("exerciseId") ?: 0L
    private val name = MutableStateFlow("")

    init {
        viewModelScope.launch { name.value = history.exerciseName(exerciseId) }
    }

    val state: StateFlow<ProgressUiState> =
        history.progress(exerciseId)
            .map { ProgressUiState(name = name.value, points = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}
