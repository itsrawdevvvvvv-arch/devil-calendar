package com.devil.calendar.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.prefs.UserProfile
import com.devil.calendar.data.repository.DayRepository
import com.devil.calendar.data.repository.ProfileRepository
import com.devil.calendar.domain.StatsCalculator
import com.devil.calendar.util.DateUtils
import com.devil.calendar.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val profile: UserProfile = UserProfile(),
    val crossed: Set<String> = emptySet(),
    val month: YearMonth = YearMonth.from(DateUtils.today()),
    val streak: Int = 0,
    val todayKey: String = DateUtils.todayKey(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    dayRepository: DayRepository,
    profileRepository: ProfileRepository,
    private val widget: WidgetRefresher,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(DateUtils.today()))

    val state: StateFlow<HomeUiState> = combine(
        dayRepository.crossedDays,
        profileRepository.profile,
        month,
    ) { crossed, profile, month ->
        HomeUiState(
            profile = profile,
            crossed = crossed,
            month = month,
            streak = StatsCalculator.streak(crossed),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggle(date: String) {
        viewModelScope.launch {
            dayRepository.toggle(date)
            widget.refresh()
        }
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }
}
