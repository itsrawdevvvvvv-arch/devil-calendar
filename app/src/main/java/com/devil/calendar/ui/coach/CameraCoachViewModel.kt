package com.devil.calendar.ui.coach

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.camera.RepStateMachine
import com.devil.calendar.camera.SkeletonFrame
import com.devil.calendar.data.repository.CoachConfigRepository
import com.devil.calendar.domain.coach.CoachConfig
import com.devil.calendar.domain.coach.CoachMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val exerciseName: String = "",
    val config: CoachConfig? = null,
    val loading: Boolean = true,
    val skeleton: SkeletonFrame? = null,
    val reps: Int = 0,
    val holdSeconds: Int = 0,
    val formValid: Boolean = false,
) {
    val isHold: Boolean get() = config?.mode == CoachMode.HOLD
}

@HiltViewModel
class CameraCoachViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CoachConfigRepository,
) : ViewModel() {

    private val exerciseName: String = savedStateHandle.get<String>("exerciseName").orEmpty()

    private val _state = MutableStateFlow(CoachUiState(exerciseName = exerciseName))
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    private var machine: RepStateMachine? = null

    init {
        viewModelScope.launch {
            // configFor cannot throw and cannot block on the network, but a
            // belt-and-braces fallback means the camera never fails to open
            // because a config could not be resolved.
            val config = runCatching { repository.configFor(exerciseName) }
                .getOrElse {
                    com.devil.calendar.domain.coach.BuiltInCoachTemplates.find("squat")!!
                        .copy(exerciseKey = exerciseName)
                }
            machine = RepStateMachine(config)
            _state.value = _state.value.copy(config = config, loading = false)
        }
        viewModelScope.launch {
            // Runs after the camera is up; a failure here is irrelevant.
            runCatching { repository.upgradeInBackground(exerciseName) }
        }
    }

    /** Called from the analyser thread; state updates are cheap and atomic. */
    fun onSkeleton(frame: SkeletonFrame) {
        val engine = machine ?: return
        val phase = engine.onFrame(frame.angle, System.currentTimeMillis())
        _state.value = _state.value.copy(
            skeleton = frame,
            reps = engine.reps,
            holdSeconds = engine.holdSeconds,
            formValid = frame.valid && engine.formIsValid(),
        )
        if (phase == com.devil.calendar.domain.coach.RepPhase.UNKNOWN) return
    }

    fun adjustReps(delta: Int) {
        machine?.adjustReps(delta)
        _state.value = _state.value.copy(reps = machine?.reps ?: 0)
    }

    fun reset() {
        machine?.reset()
        _state.value = _state.value.copy(reps = 0, holdSeconds = 0)
    }
}
