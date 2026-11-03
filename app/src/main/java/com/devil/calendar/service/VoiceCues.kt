package com.devil.calendar.service

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.core.content.getSystemService
import com.devil.calendar.data.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spoken cues during a session. Off by default; the toggle lives in Profile.
 *
 * TTS is initialised lazily on first use and shut down with the process - a
 * held engine is a battery leak.
 */
@Singleton
class VoiceCues @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine: TextToSpeech? = null
    private var ready = false
    private var lastSpoken: String = ""

    fun speak(text: String) {
        if (text == lastSpoken) return
        scope.launch {
            if (!prefs.voiceCues.first()) return@launch
            lastSpoken = text
            ensureEngine()
            if (ready) {
                engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "devil-cue")
            }
        }
    }

    private fun ensureEngine() {
        if (engine != null) return
        val created = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.language = Locale.UK
            }
        }
        engine = created
    }
}
