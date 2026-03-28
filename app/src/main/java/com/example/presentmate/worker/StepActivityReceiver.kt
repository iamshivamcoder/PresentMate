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
import java.util.Calendar

/**
 * Handles user responses to the step-activity detection notifications.
 */
class StepActivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("StepActivityReceiver", "Received action: $action")

        when (action) {
            StepActivityNotificationUtils.ACTION_STEP_YES_START -> startSession(context)
            StepActivityNotificationUtils.ACTION_STEP_YES_END   -> endSession(context)
            StepActivityNotificationUtils.ACTION_STEP_NOT_NOW   -> { /* user dismissed — do nothing */ }
        }
    }

    private fun startSession(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = PresentMateDatabase.getDatabase(context)
                val ongoing = withContext(Dispatchers.IO) { db.attendanceDao().getOngoingSession() }
                if (ongoing == null) {
                    val now = System.currentTimeMillis()
                    val todayMidnight = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    db.attendanceDao().insertRecord(
                        AttendanceRecord(date = todayMidnight, timeIn = now, timeOut = null)
                    )
                    Log.d("StepActivityReceiver", "Session started via step detection")
                } else {
                    Log.d("StepActivityReceiver", "Session already in progress")
                }
            } catch (e: Exception) {
                Log.e("StepActivityReceiver", "Failed to start session", e)
            }
        }
    }

    private fun endSession(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = PresentMateDatabase.getDatabase(context)
                val ongoing = withContext(Dispatchers.IO) { db.attendanceDao().getOngoingSession() }
                if (ongoing != null) {
                    val now = System.currentTimeMillis()
                    db.attendanceDao().updateRecord(ongoing.copy(timeOut = now))
                    Log.d("StepActivityReceiver", "Session ended via step detection")
                }
            } catch (e: Exception) {
                Log.e("StepActivityReceiver", "Failed to end session", e)
            }
        }
    }
}
