package com.devil.calendar.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.repository.DayRepository
import com.devil.calendar.domain.StatsCalculator
import com.devil.calendar.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class StatRange { DAY, WEEK, MONTH }

data class StatsUiState(
    val range: StatRange = StatRange.WEEK,
    val dateLabel: String = "",
    val rangeLabel: String = "",
    val rangeValue: String = "",
    val bars: List<Boolean> = List(7) { false },
    val streak: Int = 0,
    val monthCrossed: Int = 0,
    val monthTotal: Int = 0,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    dayRepository: DayRepository,
) : ViewModel() {

    private val range = MutableStateFlow(StatRange.WEEK)

    val state: StateFlow<StatsUiState> = combine(
        dayRepository.crossedDays,
        range,
    ) { crossed, range ->
        val today = DateUtils.today()
        val month = YearMonth.from(today)
        val weekCount = StatsCalculator.weekCount(crossed, today)
        val monthCrossed = StatsCalculator.monthCount(crossed, month)
        val monthTotal = StatsCalculator.monthTotal(month)
        val isToday = StatsCalculator.isCrossedToday(crossed, today)

        StatsUiState(
            range = range,
            dateLabel = today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            rangeLabel = when (range) {
                StatRange.DAY -> "Today"
                StatRange.WEEK -> "This week"
                StatRange.MONTH -> "This month"
            },
            rangeValue = when (range) {
                StatRange.DAY -> if (isToday) "Done" else "Open"
                StatRange.WEEK -> "$weekCount/7"
                StatRange.MONTH -> "$monthCrossed/$monthTotal"
            },
            bars = StatsCalculator.weekBars(crossed, today).map { it.second },
            streak = StatsCalculator.streak(crossed, today),
            monthCrossed = monthCrossed,
            monthTotal = monthTotal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun selectRange(index: Int) {
        range.value = StatRange.entries[index]
    }
}
