package com.devil.calendar.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.data.repository.WalkRepository
import com.devil.calendar.domain.walk.DeadReckoning
import com.devil.calendar.domain.walk.WordOfDay
import com.devil.calendar.util.DateUtils
import com.devil.calendar.walk.StepSensor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkUiState(
    val stepsToday: Int = 0,
    val goal: Int = 6000,
    val word: String = "",
    val walking: Boolean = false,
    val walkSteps: Int = 0,
    val path: List<Pair<Double, Double>> = emptyList(),
    val hasSensor: Boolean = true,
    val weekSteps: List<Int> = emptyList(),
) {
    val progress: Float get() = WordOfDay.progress(stepsToday, goal)
    val goalReached: Boolean get() = WordOfDay.isGoalReached(stepsToday, goal)
    val distanceMetres: Double get() = walkSteps * 0.7
}

@HiltViewModel
class WalkViewModel @Inject constructor(
    private val repository: WalkRepository,
    private val sensors: StepSensor,
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WalkUiState(
            word = WordOfDay.forDate(DateUtils.today()),
            hasSensor = sensors.hasStepCounter || sensors.hasStepDetector,
        ),
    )
    val state: StateFlow<WalkUiState> = _state.asStateFlow()

    val goal: StateFlow<Int> = prefs.stepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 6000)

    private val reckoning = DeadReckoning()
    private var lastCounter = -1L
    private var startedAt = 0L
    private var stepsAtStart = 0

    init {
        viewModelScope.launch {
            val goal = prefs.stepGoal.first()
            val height = prefs.heightCm.first()
            reckoning.setStepLength(DeadReckoning.stepLengthForHeight(height))
            _state.value = _state.value.copy(goal = goal, word = WordOfDay.forDate(DateUtils.today()))
            repository.weekSteps().collect { week ->
                _state.value = _state.value.copy(weekSteps = week)
            }
        }
    }

    fun startWalk() {
        if (_state.value.walking) return
        startedAt = System.currentTimeMillis()
        stepsAtStart = _state.value.walkSteps
        reckoning.reset()
        _state.value = _state.value.copy(walking = true, path = emptyList())
        sensors.start(
            onCounter = { raw -> publishToday(raw) },
            onStep = {
                if (_state.value.walking) {
                    reckoning.onStep()
                    _state.value = _state.value.copy(
                        walkSteps = _state.value.walkSteps + 1,
                        path = reckoning.path.toList(),
                    )
                }
            },
            onHeading = { degrees -> reckoning.onHeading(degrees) },
        )
    }

    fun stopWalk() {
        if (!_state.value.walking) return
        sensors.stop()
        val current = _state.value
        viewModelScope.launch {
            runCatching {
                repository.saveWalk(
                    startedAt = startedAt,
                    endedAt = System.currentTimeMillis(),
                    steps = current.walkSteps,
                    path = reckoning.path,
                    style = (DateUtils.today().dayOfYear % 6),
                )
            }
        }
        _state.value = current.copy(walking = false)
    }

    private fun publishToday(raw: Long) {
        lastCounter = raw
        viewModelScope.launch {
            val today = runCatching { repository.stepsToday(raw) }.getOrDefault(_state.value.stepsToday)
            _state.value = _state.value.copy(stepsToday = today)
        }
    }

    fun setGoal(value: Int) {
        viewModelScope.launch {
            prefs.setStepGoal(value)
            _state.value = _state.value.copy(goal = value.coerceIn(500, 50_000))
        }
    }

    override fun onCleared() {
        sensors.stop()
        super.onCleared()
    }
}
