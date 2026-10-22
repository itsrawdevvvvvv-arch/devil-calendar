package com.devil.calendar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every alert is scheduled from an absolute end timestamp via
 * [AlarmManager.setAlarmClock] — never a ticking loop — so it survives Doze,
 * a force-close and a reboot.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: AlarmManager? get() = context.getSystemService()

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager?.canScheduleExactAlarms() == true
        } else {
            true
        }

    /** Fires [kind] at [triggerAtMillis]. Replaces any pending alert of that kind. */
    fun schedule(kind: AlertKind, triggerAtMillis: Long, label: String = "") {
        val am = manager ?: return
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent(kind, label))
        runCatching { am.setAlarmClock(info, fireIntent(kind, label)) }
    }

    fun cancel(kind: AlertKind, label: String = "") {
        manager?.cancel(fireIntent(kind, label))
    }

    /** Reschedules the widget refresh at the next midnight. */
    fun scheduleWidgetMidnight(triggerAtMillis: Long) {
        val am = manager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_WIDGET_MIDNIGHT
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQ_WIDGET,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        runCatching { am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, null), pi) }
    }

    private fun fireIntent(kind: AlertKind, label: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_KIND, kind.name)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(kind),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Tapping the alarm clock icon opens the app (show intent). */
    private fun showIntent(kind: AlertKind, label: String): PendingIntent {
        val intent = Intent(context, Class.forName("com.devil.calendar.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_KIND, kind.name)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        return PendingIntent.getActivity(
            context,
            requestCode(kind) + 100,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun requestCode(kind: AlertKind): Int = when (kind) {
        AlertKind.TIMER -> 1001
        AlertKind.ALARM -> 1002
        AlertKind.FOCUS -> 1003
    }

    companion object {
        private const val REQ_WIDGET = 2001
    }
}
