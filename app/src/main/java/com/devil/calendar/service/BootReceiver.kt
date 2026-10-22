package com.devil.calendar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devil.calendar.data.repository.AlarmRepository
import com.devil.calendar.widget.WidgetRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Re-arms every saved alarm after a reboot, an app update, a timezone change or
 * a manual clock change — any of which invalidates the stored trigger time.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarms: AlarmRepository
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var widget: WidgetRefresher

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                scheduler.rescheduleAll(alarms.enabled())
                scheduler.scheduleWidgetMidnight(nextMidnight())
                widget.refresh()
            }
            pending.finish()
        }
    }

    private fun nextMidnight(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
