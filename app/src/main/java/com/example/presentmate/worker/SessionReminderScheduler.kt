package com.example.presentmate.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object SessionReminderScheduler {

    const val ACTION_ALARM = "com.example.presentmate.ACTION_SESSION_REMINDER"
    private const val REQUEST_CODE = 4001

    /** Schedules the next Mon-Sat 9:30 AM alarm (skipping Sunday). */
    fun scheduleNext(context: Context) {
        val prefs = context.getSharedPreferences("session_reminder_prefs", Context.MODE_PRIVATE)
        val hourOfDay = prefs.getInt("reminder_hour", 9)
        val minute = prefs.getInt("reminder_minute", 30)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If we've already passed today's alarm time, move to tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Skip Sunday (Calendar.SUNDAY == 1)
        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerAtMillis = calendar.timeInMillis
        Log.d("SessionReminderScheduler", "Next reminder scheduled for: ${calendar.time}")

        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = ACTION_ALARM
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
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                // Fallback: inexact alarm
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** Call once on app startup to ensure alarm is always set. */
    fun ensureScheduled(context: Context) {
        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = ACTION_ALARM
        }
        val existingPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existingPendingIntent == null) {
            scheduleNext(context)
        }
    }

    /** Cancel the alarm (e.g. if user disables notifications). */
    fun cancel(context: Context) {
        val intent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = ACTION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
