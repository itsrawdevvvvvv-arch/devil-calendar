package com.devil.calendar.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.calendar.data.prefs.UserPreferences
import com.devil.calendar.data.prefs.UserProfile
import com.devil.calendar.data.repository.ProfileRepository
import com.devil.calendar.data.ai.AiResult
import com.devil.calendar.data.ai.AiRoutineRepository
import com.devil.calendar.data.security.AiKeyStore
import com.devil.calendar.service.island.IslandController
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
    private val island: IslandController,
    private val keyStore: AiKeyStore,
    private val ai: AiRoutineRepository,
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

    private val _aiKeyInput = MutableStateFlow("")
    val aiKeyInput: StateFlow<String> = _aiKeyInput.asStateFlow()

    private val _aiStatus = MutableStateFlow(AiKeyStatus.NOT_SET)
    val aiStatus: StateFlow<AiKeyStatus> = _aiStatus.asStateFlow()

    private val _aiStatusText = MutableStateFlow("Not set")
    val aiStatusText: StateFlow<String> = _aiStatusText.asStateFlow()

    val aiHasKey: StateFlow<Boolean> = keyStore.hasKey
        .onEach { has ->
            if (!has && _aiStatus.value != AiKeyStatus.CHECKING) {
                _aiStatus.value = AiKeyStatus.NOT_SET
                _aiStatusText.value = "Not set"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onAiKeyChange(value: String) {
        _aiKeyInput.value = value
        if (_aiStatus.value != AiKeyStatus.CHECKING) {
            _aiStatus.value = AiKeyStatus.NOT_SET
            _aiStatusText.value = if (value.isBlank()) "Not set" else "Not saved"
        }
    }

    fun saveAiKey() {
        val raw = _aiKeyInput.value
        viewModelScope.launch {
            val saved = keyStore.save(raw)
            _aiKeyInput.value = ""
            if (saved == null) {
                _aiStatus.value = AiKeyStatus.ERROR
                _aiStatusText.value = "Key looks empty"
            } else {
                _aiStatus.value = AiKeyStatus.NOT_SET
                _aiStatusText.value = "Saved"
            }
        }
    }

    fun testAiKey() {
        viewModelScope.launch {
            _aiStatus.value = AiKeyStatus.CHECKING
            _aiStatusText.value = "Checking"
            when (val result = ai.testConnection()) {
                is AiResult.Success -> {
                    _aiStatus.value = AiKeyStatus.CONNECTED
                    _aiStatusText.value = "Connected"
                }
                is AiResult.Failure -> {
                    _aiStatus.value = AiKeyStatus.ERROR
                    _aiStatusText.value = result.error.message
                }
            }
        }
    }

    fun clearAiKey() {
        viewModelScope.launch {
            keyStore.clear()
            _aiStatus.value = AiKeyStatus.NOT_SET
            _aiStatusText.value = "Not set"
        }
    }

    val islandOn: StateFlow<Boolean> = prefs.islandEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setIslandEnabled(value: Boolean) {
        viewModelScope.launch {
            prefs.setIslandEnabled(value)
            island.setEnabled(value)
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
