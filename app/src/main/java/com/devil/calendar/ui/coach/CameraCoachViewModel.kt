package com.devil.calendar.ui.coach

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.camera.CameraView
import com.devil.calendar.camera.CoachFrame
import com.devil.calendar.camera.RepCounter
import com.devil.calendar.data.repository.CoachConfigRepository
import com.devil.calendar.data.repository.SessionRepository
import com.devil.calendar.service.RestTimerController
import com.devil.calendar.domain.coach.BuiltInCoachTemplates
import com.devil.calendar.domain.coach.CoachConfig
import com.devil.calendar.domain.coach.CoachMode
import com.devil.calendar.domain.coach.RepPhase
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
    val frame: CoachFrame? = null,
    val reps: Int = 0,
    val holdSeconds: Int = 0,
    val phase: RepPhase = RepPhase.UNKNOWN,
    val ready: Boolean = false,
    val view: CameraView = CameraView.UNKNOWN,
    val hint: String = "",
    val debug: Boolean = false,
    val fps: Int = 0,
    val signal: Float? = null,
    val low: Float = 0f,
    val high: Float = 0f,
) {
    val isHold: Boolean get() = config?.mode == CoachMode.HOLD
}

@HiltViewModel
class CameraCoachViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CoachConfigRepository,
    private val sessions: SessionRepository,
    private val restTimer: RestTimerController,
) : ViewModel() {

    private val setId: Long = savedStateHandle.get<Long>("setId") ?: -1L
    private val restSeconds: Int = savedStateHandle.get<Int>("restSeconds") ?: 90
    private var saved = false

    private val exerciseName: String = savedStateHandle.get<String>("exerciseName").orEmpty()

    private val _state = MutableStateFlow(CoachUiState(exerciseName = exerciseName))
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    private var counter: RepCounter? = null

    init {
        viewModelScope.launch {
            val config = runCatching { repository.configFor(exerciseName) }
                .getOrElse { BuiltInCoachTemplates.find("squat")!!.copy(exerciseKey = exerciseName) }
            counter = RepCounter(config)
            _state.value = _state.value.copy(config = config, loading = false)
        }
        viewModelScope.launch { runCatching { repository.upgradeInBackground(exerciseName) } }
    }

    fun toggleDebug() {
        _state.value = _state.value.copy(debug = !_state.value.debug)
    }

    /** Called from the analyser thread. */
    fun onFrame(frame: CoachFrame) {
        val engine = counter ?: return
        val current = _state.value

        // Nothing is counted until the body has been framed for ten frames.
        val phase = if (frame.ready && frame.signal != null) {
            engine.onSignal(frame.signal, frame.nowMillis)
        } else {
            current.phase
        }

        _state.value = current.copy(
            frame = frame,
            reps = engine.reps,
            holdSeconds = engine.holdSeconds,
            phase = phase,
            ready = frame.ready,
            view = frame.view,
            hint = hintFor(frame, engine),
            fps = frame.fps,
            signal = frame.signal,
            low = engine.lowThreshold,
            high = engine.highThreshold,
        )
    }

    private fun hintFor(frame: CoachFrame, engine: RepCounter): String {
        if (frame.points.isEmpty()) return "Step back so your whole body is in frame"
        if (!frame.ready) return "Show your shoulders, arms and hands"
        return when {
            frame.view == CameraView.FRONT && frame.signal == null ->
                "Turn side-on for best accuracy"
            engine.lastRepWasShallow -> "Go lower"
            frame.view == CameraView.FRONT ->
                "Head-on view - turn side-on for better counting"
            else -> "Side view - good"
        }
    }

    fun adjustReps(delta: Int) {
        counter?.adjust(delta)
        _state.value = _state.value.copy(reps = counter?.reps ?: 0)
    }

    fun reset() {
        counter?.reset()
        _state.value = _state.value.copy(reps = 0, holdSeconds = 0)
    }

    /**
     * Writes the count into the set and starts the rest, exactly as tapping the
     * set does. Idempotent, so FINISH followed by a back press cannot double it.
     */
    fun saveAndFinish(onDone: () -> Unit) {
        val current = _state.value
        val reps = current.reps
        val hold = current.holdSeconds
        viewModelScope.launch {
            if (setId > 0 && !saved) {
                saved = sessions.completeSetWithReps(setId, reps, hold)
                if (saved && reps + hold > 0) {
                    restTimer.start(restSeconds, current.exerciseName)
                }
            }
            onDone()
        }
    }

    /** Draft save, so a back press or a kill never loses the count. */
    fun saveDraft() {
        val current = _state.value
        if (setId <= 0 || saved) return
        viewModelScope.launch {
            runCatching { sessions.saveDraftReps(setId, current.reps, current.holdSeconds) }
        }
    }
}
