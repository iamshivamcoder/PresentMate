package com.example.presentmate.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.db.PresentMateDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SessionReminderScheduler.ACTION_ALARM -> {
                // Show the notification
                SessionReminderNotificationUtils.showSessionReminderNotification(context)
                // Schedule next alarm
                SessionReminderScheduler.scheduleNext(context)
            }
            SessionReminderNotificationUtils.ACTION_YESS -> {
                // Dismiss notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
                nm.cancel(SessionReminderNotificationUtils.NOTIFICATION_ID)

                // Start a session in the database
                val db = PresentMateDatabase.getDatabase(context)
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                scope.launch {
                    try {
                        // getOngoingSession is blocking – call from IO dispatcher
                        val ongoing = withContext(Dispatchers.IO) {
                            db.attendanceDao().getOngoingSession()
                        }
                        if (ongoing == null) {
                            val now = System.currentTimeMillis()
                            // Use today's midnight for 'date' field to match the ViewModel convention
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = now
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }
                            val todayMidnight = calendar.timeInMillis
                            db.attendanceDao().insertRecord(
                                AttendanceRecord(date = todayMidnight, timeIn = now, timeOut = null)
                            )
                            Log.d("SessionReminder", "Session started via 'Yess' notification")
                        } else {
                            Log.d("SessionReminder", "Session already in progress, skipping")
                        }
                    } catch (e: Exception) {
                        Log.e("SessionReminder", "Failed to start session", e)
                    }
                }
            }
        }
    }
}
