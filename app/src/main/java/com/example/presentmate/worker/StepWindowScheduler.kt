package com.example.presentmate.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.presentmate.services.StepActivityService
import java.util.Calendar

/**
 * Schedules daily AlarmManager alarms to start/stop [StepActivityService] at the
 * two activity-detection windows:
 *   • Morning : 9:00 AM  – 10:00 AM
 *   • Evening : 8:00 PM  – 9:00 PM
 */
object StepWindowScheduler {

    const val ACTION_START_MORNING = "com.example.presentmate.STEP_START_MORNING"
    const val ACTION_STOP_MORNING  = "com.example.presentmate.STEP_STOP_MORNING"
    const val ACTION_START_EVENING = "com.example.presentmate.STEP_START_EVENING"
    const val ACTION_STOP_EVENING  = "com.example.presentmate.STEP_STOP_EVENING"

    private const val RC_START_MORNING = 7001
    private const val RC_STOP_MORNING  = 7002
    private const val RC_START_EVENING = 7003
    private const val RC_STOP_EVENING  = 7004

    private val windows = listOf(
        Triple(9, 0, RC_START_MORNING)  to ACTION_START_MORNING,
        Triple(10, 0, RC_STOP_MORNING)  to ACTION_STOP_MORNING,
        Triple(20, 0, RC_START_EVENING) to ACTION_START_EVENING,
        Triple(21, 0, RC_STOP_EVENING)  to ACTION_STOP_EVENING
    )

    /** Schedule all four daily window alarms (idempotent). */
    fun ensureScheduled(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for ((time, action) in windows) {
            val (hour, minute, requestCode) = time
            scheduleDaily(context, alarmManager, hour, minute, requestCode, action)
        }
        Log.d("StepWindowScheduler", "Step detection windows scheduled")
    }

    /** Cancel all window alarms. */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val allActions = listOf(ACTION_START_MORNING, ACTION_STOP_MORNING, ACTION_START_EVENING, ACTION_STOP_EVENING)
        val allRcs = listOf(RC_START_MORNING, RC_STOP_MORNING, RC_START_EVENING, RC_STOP_EVENING)
        allRcs.zip(allActions).forEach { (rc, action) ->
            val pi = PendingIntent.getBroadcast(
                context, rc,
                Intent(context, StepWindowAlarmReceiver::class.java).apply { this.action = action },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return@forEach
            alarmManager.cancel(pi)
        }
    }

    private fun scheduleDaily(
        context: Context, alarmManager: AlarmManager,
        hour: Int, minute: Int, requestCode: Int, action: String
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, StepWindowAlarmReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}
