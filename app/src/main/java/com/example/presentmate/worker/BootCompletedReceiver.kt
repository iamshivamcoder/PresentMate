package com.example.presentmate.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restores all scheduled alarms after device reboot.
 * AlarmManager alarms are cleared on reboot — this receiver re-creates them.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule 9:30 AM session reminder
            SessionReminderScheduler.scheduleNext(context)

            // Re-schedule morning/evening step-detection windows (9–10 AM, 8–9 PM)
            StepWindowScheduler.ensureScheduled(context)

            // Re-enqueue periodic step-count sync (WorkManager survives reboot, but being explicit)
            StepSyncWorker.schedulePeriodicSync(context)
        }
    }
}
