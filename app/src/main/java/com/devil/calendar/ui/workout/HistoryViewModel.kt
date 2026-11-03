package com.devil.calendar.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.HistoryRepository
import com.devil.calendar.domain.workout.HistoryItem
import com.devil.calendar.domain.workout.MuscleVolume
import com.devil.calendar.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<HistoryItem> = emptyList(),
    val weekValues: List<Float> = List(7) { 0f },
    val weekLabels: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
    val weekVolume: Double = 0.0,
    val muscleVolume: List<MuscleVolume> = emptyList(),
) {
    val hasHistory: Boolean get() = sessions.isNotEmpty()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    history: HistoryRepository,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = history.history()
        .map { sessions -> buildState(sessions, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private suspend fun buildState(
        sessions: List<HistoryItem>,
        history: HistoryRepository,
    ): HistoryUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val week = DateUtils.weekOf(today)

        val byDay = sessions.groupBy { item ->
            Instant.ofEpochMilli(item.startTime).atZone(zone).toLocalDate()
        }
        val weekValues = week.map { day ->
            byDay[day].orEmpty().sumOf { it.volumeKg }.toFloat()
        }
        val labels = week.map { it.dayOfWeek.name.take(1) }

        val monthAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        return HistoryUiState(
            sessions = sessions,
            weekValues = weekValues,
            weekLabels = labels,
            weekVolume = weekValues.sumOf { it.toDouble() },
            muscleVolume = history.muscleVolume(monthAgo).first(),
        )
    }
}
