package com.devil.calendar.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.ai.AiResult
import com.devil.calendar.data.ai.AiRoutineRepository
import com.devil.calendar.data.ai.RoutineDraft
import com.devil.calendar.data.local.workout.RoutineKind
import com.devil.calendar.data.repository.WorkoutRepository
import com.devil.calendar.data.security.AiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiImportUiState(
    val pasted: String = "",
    val goal: String = "",
    val draft: RoutineDraft? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class AiImportViewModel @Inject constructor(
    private val ai: AiRoutineRepository,
    private val workouts: WorkoutRepository,
    keyStore: AiKeyStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AiImportUiState())
    val state: StateFlow<AiImportUiState> = _state.asStateFlow()

    val hasKey: StateFlow<Boolean> = keyStore.hasKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onPastedChange(value: String) {
        _state.value = _state.value.copy(pasted = value, message = null, isError = false)
    }

    fun onGoalChange(value: String) {
        _state.value = _state.value.copy(goal = value, message = null, isError = false)
    }

    /** Fully offline: JSON or the plain-text grammar. */
    fun parsePasted() {
        val text = _state.value.pasted
        val draft = ai.parsePasted(text)
        _state.value = if (draft == null) {
            _state.value.copy(
                draft = null,
                message = "Could not read that. Try JSON, or a line like \"Push-ups 4x12 rest 60s\".",
                isError = true,
            )
        } else {
            _state.value.copy(draft = draft, message = "Parsed ${draft.exercises.size} exercises.", isError = false)
        }
    }

    fun generate() {
        val goal = _state.value.goal
        if (goal.isBlank()) return
        _state.value = _state.value.copy(busy = true, message = null, isError = false)
        viewModelScope.launch {
            when (val result = ai.generate(goal)) {
                is AiResult.Success ->
                    _state.value = _state.value.copy(
                        busy = false,
                        draft = result.value,
                        message = "Generated ${result.value.exercises.size} exercises.",
                        isError = false,
                    )
                is AiResult.Failure ->
                    _state.value = _state.value.copy(
                        busy = false,
                        message = result.error.message,
                        isError = true,
                    )
            }
        }
    }

    fun save(kind: RoutineKind, onSaved: (Long) -> Unit) {
        val draft = _state.value.draft ?: return
        _state.value = _state.value.copy(busy = true, message = null, isError = false)
        viewModelScope.launch {
            runCatching { workouts.saveDraft(draft, kind) }
                .onSuccess { id ->
                    _state.value = _state.value.copy(busy = false)
                    onSaved(id)
                }
                .onFailure { error ->
                    // Never let a save failure look like nothing happened.
                    _state.value = _state.value.copy(
                        busy = false,
                        message = "Could not save: ${error.message ?: "unknown error"}",
                        isError = true,
                    )
                }
        }
    }

    fun discard() {
        _state.value = _state.value.copy(draft = null, message = null, isError = false)
    }
}
