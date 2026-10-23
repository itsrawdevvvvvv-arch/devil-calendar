package com.devil.calendar

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.calendar.service.AlertController
import com.devil.calendar.service.AlertKind
import com.devil.calendar.service.AlertService
import com.devil.calendar.ui.takeover.TakeoverScreen
import com.devil.calendar.ui.theme.DevilTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The full-screen takeover. Shown over the lock screen, wakes the display and
 * blocks the back button until the user holds to dismiss.
 *
 * It renders [AlertController] state and never plays sound itself.
 */
@AndroidEntryPoint
class TakeoverActivity : ComponentActivity() {

    @Inject lateinit var controller: AlertController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
            ?.requestDismissKeyguard(this, null)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Back is blocked without overriding onBackPressed, which lint flags.
        onBackPressedDispatcher.addCallback(this) { /* intentionally ignored */ }

        // The fallback keeps the takeover meaningful if state is not ready yet.
        val fallbackKind = AlertKind.from(intent.getStringExtra(EXTRA_KIND))

        setContent {
            DevilTheme(darkTheme = true) {
                val state by controller.state.collectAsStateWithLifecycle()
                val shown = if (state.headline.isBlank()) {
                    state.copy(kind = fallbackKind, headline = fallbackKind.titleText)
                } else {
                    state
                }

                // Another dismissal route (notification Stop, snooze) ends us too.
                LaunchedEffect(state.active) {
                    if (!state.active) finishAndRemoveTask()
                }

                TakeoverScreen(
                    state = shown,
                    onDismiss = ::dismiss,
                    onSnooze = { AlertService.snoozeAlert(this@TakeoverActivity, shown.subtitle) },
                    contentInsets = WindowInsets.safeDrawing.asPaddingValues(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The heads-up has served its purpose once the takeover is visible.
        runCatching {
            NotificationManagerCompat.from(this).cancel(AlertService.ALERT_NOTIFICATION_ID)
        }
    }

    private fun dismiss() {
        AlertService.stopAlert(this)
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_KIND = "extra_kind"

        fun intent(context: Context, kind: AlertKind): Intent =
            Intent(context, TakeoverActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_KIND, kind.name)
            }
    }
}
