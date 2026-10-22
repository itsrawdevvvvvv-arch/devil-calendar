package com.devil.calendar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.devil.calendar.widget.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var widget: WidgetRefresher

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_MIDNIGHT -> {
                // Redraw the widget so "today" moves to the new date.
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    runCatching { widget.refresh() }
                    pending.finish()
                }
            }
            ACTION_FIRE -> {
                val kind = AlertKind.from(intent.getStringExtra(EXTRA_KIND))
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                ContextCompat.startForegroundService(
                    context,
                    AlertService.fireIntent(context, kind, label),
                )
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.devil.calendar.action.FIRE"
        const val ACTION_WIDGET_MIDNIGHT = "com.devil.calendar.action.WIDGET_MIDNIGHT"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_LABEL = "extra_label"
    }
}
