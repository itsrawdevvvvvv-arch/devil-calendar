package com.devil.calendar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.devil.calendar.data.repository.AlarmRepository
import com.devil.calendar.widget.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var alarms: AlarmRepository
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var widget: WidgetRefresher

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_MIDNIGHT -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    runCatching { widget.refresh() }
                    pending.finish()
                }
            }

            ACTION_FIRE -> {
                val kind = AlertKind.from(intent.getStringExtra(EXTRA_KIND))
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)

                ContextCompat.startForegroundService(
                    context,
                    AlertService.fireIntent(context, kind, label),
                )

                // A repeating alarm re-arms itself for its next occurrence.
                if (kind == AlertKind.ALARM && alarmId > 0L) {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.Default).launch {
                        runCatching {
                            alarms.byId(alarmId)?.let { alarm ->
                                if (alarm.enabled) {
                                    val trigger = scheduler.scheduleAlarm(alarm)
                                    alarms.setEnabled(alarm.id, true, trigger)
                                }
                            }
                        }
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.devil.calendar.action.FIRE"
        const val ACTION_WIDGET_MIDNIGHT = "com.devil.calendar.action.WIDGET_MIDNIGHT"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
