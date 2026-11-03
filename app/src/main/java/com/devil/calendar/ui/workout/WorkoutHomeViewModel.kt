package com.devil.calendar.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.SessionRepository
import com.devil.calendar.data.repository.WorkoutRepository
import com.devil.calendar.domain.workout.RoutineSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutHomeViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val sessions: SessionRepository,
) : ViewModel() {

    val routines: StateFlow<List<RoutineSummary>> = repository.routines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    /** Creates a routine and hands the new id back so the caller can open it. */
    fun createRoutine(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createRoutine("New routine"))
        }
    }

    /** Starts a session from a routine, or empty when [routineId] is null. */
    fun startSession(routineId: Long?, onStarted: () -> Unit) {
        viewModelScope.launch {
            sessions.startSession(routineId)
            onStarted()
        }
    }

    fun duplicate(id: Long) {
        viewModelScope.launch { repository.duplicateRoutine(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteRoutine(id) }
    }
}
