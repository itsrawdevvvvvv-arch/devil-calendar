package com.devil.calendar

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devil.calendar.service.AlertKind
import com.devil.calendar.service.AlertService
import com.devil.calendar.ui.takeover.TakeoverScreen
import com.devil.calendar.ui.theme.DevilTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The full-screen takeover. Shown over the lock screen, wakes the display and
 * blocks the back button until the user holds to dismiss.
 */
@AndroidEntryPoint
class TakeoverActivity : ComponentActivity() {

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

        val kind = AlertKind.from(intent.getStringExtra(EXTRA_KIND))

        setContent {
            DevilTheme(darkTheme = true) {
                TakeoverScreen(label = kind.takeoverLabel, onDismiss = ::dismiss)
            }
        }
    }

    private fun dismiss() {
        AlertService.dismiss(this)
        finishAndRemoveTask()
    }

    @Deprecated("The takeover must not be dismissed with the back button.")
    override fun onBackPressed() {
        // Intentionally ignored — dismissal requires the hold gesture.
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
