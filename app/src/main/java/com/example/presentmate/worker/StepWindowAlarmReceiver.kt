package com.example.presentmate.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.presentmate.services.StepActivityService

/**
 * Receives the window-start / window-stop alarms from [StepWindowScheduler]
 * and starts or stops [StepActivityService] accordingly.
 * Also re-schedules the next day's alarm so the cycle repeats.
 */
class StepWindowAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("StepWindowAlarmReceiver", "Received: $action")

        when (action) {
            StepWindowScheduler.ACTION_START_MORNING,
            StepWindowScheduler.ACTION_START_EVENING -> {
                val serviceIntent = Intent(context, StepActivityService::class.java).apply {
                    this.action = action
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            StepWindowScheduler.ACTION_STOP_MORNING,
            StepWindowScheduler.ACTION_STOP_EVENING -> {
                context.stopService(Intent(context, StepActivityService::class.java))
            }
        }

        // Re-schedule the same alarm for tomorrow
        StepWindowScheduler.ensureScheduled(context)
    }
}
