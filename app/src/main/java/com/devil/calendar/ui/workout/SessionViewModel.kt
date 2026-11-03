package com.devil.calendar.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.SessionRepository
import com.devil.calendar.domain.workout.ActiveSession
import com.devil.calendar.domain.workout.SessionExerciseItem
import com.devil.calendar.domain.workout.SessionSet
import com.devil.calendar.service.RestTimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val restTimer: RestTimerController,
) : ViewModel() {

    val session: StateFlow<ActiveSession?> = sessions.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rest: StateFlow<RestTimerController.RestState> = restTimer.state

    /**
     * Ticks a set off and starts the rest timer.
     *
     * In a superset only the last exercise of the round earns a full rest; the
     * earlier ones run straight into the next movement.
     */
    fun toggleSet(sessionId: Long, exercise: SessionExerciseItem, set: SessionSet) {
        viewModelScope.launch {
            val completing = !set.completed
            sessions.setCompleted(set.id, completing)
            if (!completing) return@launch

            val restDue = sessions.isLastOfSupersetRound(sessionId, exercise.id)
            if (restDue && set.restSeconds > 0) {
                restTimer.start(set.restSeconds, exercise.name)
            }
        }
    }

    fun addSet(sessionExerciseId: Long) {
        viewModelScope.launch { sessions.addSet(sessionExerciseId) }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { sessions.deleteSet(setId) }
    }

    fun adjustWeight(setId: Long, delta: Double) {
        viewModelScope.launch { sessions.adjustWeight(setId, delta) }
    }

    fun adjustReps(setId: Long, delta: Int) {
        viewModelScope.launch { sessions.adjustReps(setId, delta) }
    }

    fun cycleType(setId: Long) {
        viewModelScope.launch { sessions.cycleSetType(setId) }
    }

    fun removeExercise(sessionExerciseId: Long) {
        viewModelScope.launch { sessions.removeExercise(sessionExerciseId) }
    }

    fun moveExercise(sessionId: Long, sessionExerciseId: Long, delta: Int) {
        viewModelScope.launch { sessions.moveExercise(sessionId, sessionExerciseId, delta) }
    }

    fun toggleSuperset(sessionId: Long, sessionExerciseId: Long) {
        viewModelScope.launch { sessions.toggleSupersetWithPrevious(sessionId, sessionExerciseId) }
    }

    fun adjustRest(delta: Int) = restTimer.adjust(delta)

    fun skipRest() = restTimer.skip()

    fun finish(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            restTimer.finish()
            sessions.finishSession(sessionId)
            onDone()
        }
    }
}
