package com.devil.calendar.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.prefs.UserProfile
import com.devil.calendar.data.repository.ProfileRepository
import com.devil.calendar.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val widget: WidgetRefresher,
) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

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
}
