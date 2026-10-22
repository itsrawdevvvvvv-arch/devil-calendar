package com.devil.calendar.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.data.prefs.UserProfile
import com.devil.calendar.data.repository.ProfileRepository
import com.devil.calendar.service.AlertKind
import com.devil.calendar.service.AlarmScheduler
import com.devil.calendar.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val prefs: UserPreferences,
    private val widget: WidgetRefresher,
    private val scheduler: AlarmScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val use24Hour: StateFlow<Boolean> = prefs.use24Hour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setName(value: String) {
        viewModelScope.launch {
            repository.setName(value)
            widget.refresh()
        }
    }

    fun setGoal(value: String) {
        viewModelScope.launch {
            repository.setGoal(value)
            widget.refresh()
        }
    }

    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            repository.saveAvatar(uri)
            widget.refresh()
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            repository.clearAvatar()
            widget.refresh()
        }
    }

    fun setUse24Hour(value: Boolean) {
        viewModelScope.launch { prefs.setUse24Hour(value) }
    }

    /** Fires a real alarm five seconds out, through the production code path. */
    fun testAlarm() {
        scheduler.schedule(AlertKind.ALARM, System.currentTimeMillis() + 5_000, "Test alarm")
    }
}
