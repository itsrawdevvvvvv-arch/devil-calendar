package com.devil.calendar.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Draws the takeover as a full-screen window on top of whatever the user is
 * looking at (the SYSTEM_ALERT_WINDOW layer). This is one of the three
 * takeover layers; the full-screen intent and the TakeoverActivity are the
 * other two.
 */
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val windowManager: WindowManager?
        get() = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var view: ComposeView? = null
    private var owner: ServiceLifecycleOwner? = null

    val canDrawOverlays: Boolean get() = Settings.canDrawOverlays(context)

    /** Shows [content] over everything, replacing any existing overlay. */
    fun show(content: @Composable () -> Unit): Boolean {
        if (!canDrawOverlays) return false
        hide()
        val wm = windowManager ?: return false
        val lifecycleOwner = ServiceLifecycleOwner().also { it.onCreate(); it.onStart() }
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent(content)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        return runCatching {
            wm.addView(composeView, params)
            view = composeView
            owner = lifecycleOwner
            true
        }.getOrDefault(false)
    }

    fun hide() {
        view?.let { v -> runCatching { windowManager?.removeViewImmediate(v) } }
        view = null
        owner?.onDestroy()
        owner = null
    }
}
