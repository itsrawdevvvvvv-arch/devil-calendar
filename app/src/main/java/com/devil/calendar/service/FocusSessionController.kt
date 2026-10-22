package com.devil.calendar.service

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the focus-lock side effects: screen pinning (driven from the Activity)
 * and Do Not Disturb. The previous interruption filter is saved and restored.
 */
@Singleton
class FocusSessionController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private var savedFilter: Int? = null

    private val notificationManager: NotificationManager?
        get() = context.getSystemService()

    fun hasPolicyAccess(): Boolean = notificationManager?.isNotificationPolicyAccessGranted == true

    fun begin() {
        _active.value = true
        runCatching {
            val nm = notificationManager ?: return@runCatching
            if (nm.isNotificationPolicyAccessGranted) {
                savedFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        }
    }

    fun end() {
        _active.value = false
        runCatching {
            val nm = notificationManager ?: return@runCatching
            savedFilter?.let { nm.setInterruptionFilter(it) }
            savedFilter = null
        }
    }

    /** Restores the ringer/alarm volume after a session, if it was lowered. */
    fun restoreVolume(previousAlarmVolume: Int?) {
        if (previousAlarmVolume == null) return
        runCatching {
            context.getSystemService<AudioManager>()
                ?.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0)
        }
    }
}
