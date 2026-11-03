package com.devil.calendar.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.HistoryRepository
import com.devil.calendar.data.repository.SessionRepository
import com.devil.calendar.domain.workout.ActiveSession
import com.devil.calendar.domain.workout.GhostSet
import com.devil.calendar.domain.workout.SessionExerciseItem
import com.devil.calendar.domain.workout.SessionSet
import com.devil.calendar.service.RestTimerController
import com.devil.calendar.data.repository.MoodRepository
import com.devil.calendar.service.VoiceCues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val history: HistoryRepository,
    private val restTimer: RestTimerController,
    private val voice: VoiceCues,
    private val moods: MoodRepository,
) : ViewModel() {

    val session: StateFlow<ActiveSession?> = sessions.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rest: StateFlow<RestTimerController.RestState> = restTimer.state

    /** Set ids that broke a record this session, for the PR badge. */
    private val _recordSetIds = MutableStateFlow<Set<Long>>(emptySet())
    val recordSetIds: StateFlow<Set<Long>> = _recordSetIds.asStateFlow()

    /** Last session's numbers per exercise id, for the faint ghost hint. */
    private val _ghosts = MutableStateFlow<Map<Long, List<GhostSet>>>(emptyMap())
    val ghosts: StateFlow<Map<Long, List<GhostSet>>> = _ghosts.asStateFlow()

    private val _newExercise = MutableStateFlow("")
    val newExercise: StateFlow<String> = _newExercise.asStateFlow()

    fun onNewExerciseChange(value: String) {
        _newExercise.value = value
    }

    fun addExercise(sessionId: Long) {
        val name = _newExercise.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            sessions.addExerciseByName(sessionId, name)
            _newExercise.value = ""
        }
    }

    fun setHold(setId: Long, seconds: Int) {
        viewModelScope.launch { sessions.setHold(setId, seconds) }
    }

    fun startNextSet() = restTimer.startNext()

    fun saveMood(score: Int) {
        viewModelScope.launch { moods.save(score) }
    }

    /** Fills every uncompleted set from the ghost numbers. */
    fun copyLastSession(sessionId: Long) {
        viewModelScope.launch {
            val active = sessions.observeActiveSession().first() ?: return@launch
            val ghosts = _ghosts.value
            active.exercises.forEach { exercise ->
                val previous = ghosts[exercise.exerciseId] ?: return@forEach
                exercise.sets.filterNot { it.completed }.forEach { set ->
                    val ghost = previous.getOrNull(set.index) ?: return@forEach
                    sessions.adjustWeight(set.id, ghost.weight - set.weight)
                    sessions.adjustReps(set.id, ghost.reps - set.reps)
                }
            }
        }
    }

    fun stopRest() = restTimer.finish()

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

            val record = history.checkRecord(
                exerciseId = exercise.exerciseId,
                sessionId = sessionId,
                reps = set.reps,
                weight = set.weight,
                unit = set.unit,
            )
            if (record.isRecord) {
                _recordSetIds.value = _recordSetIds.value + set.id
                voice.speak("New record")
            } else if (completing) {
                voice.speak("Set done")
            }

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
