package com.devil.calendar.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.devil.calendar.data.local.AlarmEntity
import com.devil.calendar.domain.AlarmTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every alert is scheduled from an absolute timestamp via
 * [AlarmManager.setAlarmClock] - never a ticking loop - so it survives Doze, a
 * force-close and a reboot.
 *
 * Saved alarms each get a unique request code derived from their row id, so
 * they never overwrite one another. The trigger time doubles as the alertId,
 * which is what makes a duplicate fire a no-op downstream.
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

    // ---- timer / focus ----------------------------------------------------

    /** Fires [kind] at [triggerAtMillis], carrying the duration for the takeover copy. */
    fun schedule(
        kind: AlertKind,
        triggerAtMillis: Long,
        label: String = "",
        durationSeconds: Long = 0L,
    ) {
        val am = manager ?: return
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent(kind, label))
        val fire = fireIntent(
            kind = kind,
            label = label,
            alarmId = 0L,
            alertId = triggerAtMillis,
            hour = -1,
            minute = -1,
            durationSeconds = durationSeconds,
        )
        runCatching { am.setAlarmClock(info, fire) }
    }

    fun cancel(kind: AlertKind, label: String = "") {
        manager?.cancel(fireIntent(kind, label, alarmId = 0L, alertId = 0L))
    }

    // ---- saved alarms -----------------------------------------------------

    /** Schedules [alarm] at its next trigger and returns that timestamp. */
    fun scheduleAlarm(alarm: AlarmEntity, now: ZonedDateTime = ZonedDateTime.now()): Long {
        if (!alarm.enabled) return 0L
        val trigger = AlarmTime.nextTrigger(alarm.time, alarm.days, now).toInstant().toEpochMilli()
        val am = manager ?: return trigger
        val info = AlarmManager.AlarmClockInfo(trigger, showIntent(AlertKind.ALARM, alarm.label))
        val fire = fireIntent(
            kind = AlertKind.ALARM,
            label = alarm.label,
            alarmId = alarm.id,
            alertId = trigger,
            hour = alarm.hour,
            minute = alarm.minute,
        )
        runCatching { am.setAlarmClock(info, fire) }
        return trigger
    }

    fun cancelAlarm(alarmId: Long) {
        manager?.cancel(fireIntent(AlertKind.ALARM, "", alarmId, alertId = 0L))
    }

    /** Re-arms every enabled alarm. Used on boot, timezone and time changes. */
    fun rescheduleAll(alarms: List<AlarmEntity>, now: ZonedDateTime = ZonedDateTime.now()) {
        alarms.filter { it.enabled }.forEach { scheduleAlarm(it, now) }
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

    // ---- intents ----------------------------------------------------------

    private fun fireIntent(
        kind: AlertKind,
        label: String,
        alarmId: Long,
        alertId: Long,
        hour: Int = -1,
        minute: Int = -1,
        durationSeconds: Long = 0L,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_KIND, kind.name)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALERT_ID, alertId)
            putExtra(AlarmReceiver.EXTRA_HOUR, hour)
            putExtra(AlarmReceiver.EXTRA_MINUTE, minute)
            putExtra(AlarmReceiver.EXTRA_DURATION_SECONDS, durationSeconds)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(kind, alarmId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Tapping the alarm-clock icon opens the app (show intent). */
    private fun showIntent(kind: AlertKind, label: String): PendingIntent {
        val intent = Intent(context, Class.forName("com.devil.calendar.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_KIND, kind.name)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        return PendingIntent.getActivity(
            context,
            requestCode(kind, 0L) + 100_000,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun requestCode(kind: AlertKind, alarmId: Long): Int = when {
        kind == AlertKind.ALARM && alarmId > 0L -> ALARM_REQUEST_BASE + alarmId.toInt()
        kind == AlertKind.ALARM -> 1002
        kind == AlertKind.TIMER -> 1001
        else -> 1003
    }

    companion object {
        private const val REQ_WIDGET = 2_001
        private const val ALARM_REQUEST_BASE = 10_000
    }
}
