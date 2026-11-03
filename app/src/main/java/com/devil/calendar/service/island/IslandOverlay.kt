package com.devil.calendar.service.island

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.getSystemService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.devil.calendar.service.ServiceLifecycleOwner

/**
 * Hosts the island pill in a window-manager overlay.
 *
 * Two Android facts worth being explicit about:
 *
 * 1. **An overlay cannot draw over the status bar.** `TYPE_APPLICATION_OVERLAY`
 *    windows are z-ordered below the system status bar, so the pill sits just
 *    *under* it rather than replacing the clock. An iPhone-style island that
 *    swallows the status bar is not possible without system privileges.
 * 2. The window is `WRAP_CONTENT`, so only the pill itself is touchable -
 *    everything outside it passes through. A fixed-size window would swallow
 *    touches across the whole rectangle, including transparent areas.
 */
class IslandOverlay(
    private val context: Context,
    private val owner: ServiceLifecycleOwner,
) {
    private var view: ComposeView? = null
    private val windowManager: WindowManager? get() = context.getSystemService()

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    fun show(content: @Composable () -> Unit): Boolean {
        if (!canDraw()) return false
        if (view != null) return true

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent(content)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topOffsetPx()
        }

        return runCatching {
            windowManager?.addView(composeView, params)
            view = composeView
            true
        }.getOrDefault(false)
    }

    /** Forces a re-measure after the pill changes size. */
    fun refresh() {
        val current = view ?: return
        runCatching { windowManager?.updateViewLayout(current, current.layoutParams) }
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager?.removeView(current) }
        view = null
    }

    /**
     * Status bar height plus a small gap. Read from the system dimen because
     * DisplayCutout insets are not available until the window is attached.
     */
    private fun topOffsetPx(): Int {
        val res = context.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        val statusBar = if (id > 0) {
            res.getDimensionPixelSize(id)
        } else {
            (24 * res.displayMetrics.density).toInt()
        }
        return statusBar + (6 * res.displayMetrics.density).toInt()
    }
}
