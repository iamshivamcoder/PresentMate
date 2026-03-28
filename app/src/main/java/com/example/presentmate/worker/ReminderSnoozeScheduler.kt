package com.example.presentmate.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Schedules a one-shot snooze alarm that re-fires the session reminder notification.
 */
object ReminderSnoozeScheduler {

    private const val REQUEST_CODE = 4010
    private const val TAG = "ReminderSnoozeScheduler"

    /**
     * Schedule a one-shot alarm to fire the session reminder after [delayMinutes].
     */
    fun scheduleSnooze(context: Context, delayMinutes: Int) {
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        Log.d(TAG, "Snooze scheduled in $delayMinutes min (trigger at $triggerAt)")

        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = SessionReminderScheduler.ACTION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    /** Cancel any pending snooze. */
    fun cancel(context: Context) {
        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = SessionReminderScheduler.ACTION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }
}
