package com.devil.calendar.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The ONE owner of alert sound and vibration.
 *
 * Nothing else in the app plays audio or vibrates: the takeover activity and
 * the overlay only observe [state]. [start] is idempotent per alertId, so a
 * duplicate trigger for the same event cannot start a second sound.
 */
@Singleton
class AlertController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Everything the takeover needs to render. */
    data class AlertState(
        val alertId: Long = 0L,
        val kind: AlertKind = AlertKind.TIMER,
        val headline: String = "",
        val subtitle: String = "",
        val goal: String? = null,
        val showSnooze: Boolean = false,
        val active: Boolean = false,
    )

    private val _state = MutableStateFlow(AlertState())
    val state: StateFlow<AlertState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var activeId: Long = 0L

    val isActive: Boolean get() = _state.value.active

    /** Starts sound and vibration for [state]. Repeat calls for the same id are ignored. */
    fun start(state: AlertState) {
        if (activeId == state.alertId && _state.value.active) return
        activeId = state.alertId
        _state.value = state.copy(active = true)
        requestAudioFocus()
        startSound()
        startVibration()
    }

    /** The single stop path: every dismissal route ends up here. */
    fun stop() {
        activeId = 0L
        _state.value = _state.value.copy(active = false)
        stopSound()
        stopVibration()
        abandonAudioFocus()
    }

    // ---- sound ------------------------------------------------------------

    private fun requestAudioFocus() {
        val am = context.getSystemService<AudioManager>() ?: return
        audioManager = am
        runCatching {
            // Make sure the alarm stream is audible.
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val request = AudioFocusRequest
                    .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { }
                    .build()
                focusRequest = request
                am.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        runCatching {
            val am = audioManager ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
        focusRequest = null
        audioManager = null
    }

    private fun startSound() {
        if (player != null) return
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }
    }

    private fun stopSound() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    // ---- vibration --------------------------------------------------------

    private fun startVibration() {
        if (vibrator != null) return
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService<Vibrator>()
            } ?: return
            vibrator = v
            v.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0))
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    companion object {
        private val VIBRATION_PATTERN = longArrayOf(0, 600, 400, 600, 400, 600)
    }
}
