package com.devil.calendar.ui.island

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.service.island.IslandPackageTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IslandSetupViewModel @Inject constructor(
    tracker: IslandPackageTracker,
    private val prefs: UserPreferences,
) : ViewModel() {

    /** Packages that have posted a notification while the listener was on. */
    val known: StateFlow<Set<String>> = tracker.known

    val allowed: StateFlow<Set<String>> = prefs.islandAllowedPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val tiltGlare: StateFlow<Boolean> = prefs.islandTilt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setTiltGlare(value: Boolean) {
        viewModelScope.launch { prefs.setIslandTilt(value) }
    }

    /** An empty allow list means "show everything eligible". */
    fun toggle(packageName: String) {
        viewModelScope.launch {
            val current = allowed.value
            val next = if (packageName in current) current - packageName else current + packageName
            prefs.setIslandAllowedPackages(next)
        }
    }
}
