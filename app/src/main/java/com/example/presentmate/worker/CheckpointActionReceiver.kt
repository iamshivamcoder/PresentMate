package com.example.presentmate.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.presentmate.di.GeofenceBroadcastReceiverEntryPoint // Re-using existing entry point or new one? Using existing for now, will rename/refactor later or add to it.
import com.example.presentmate.di.getEntryPoint
import com.example.presentmate.MainActivity
import kotlinx.coroutines.launch

class CheckpointActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val logId = intent.getIntExtra("extra_log_id", -1)
        
        if (logId == -1) return
        
        val entryPoint = getEntryPoint(context)
        val scope = entryPoint.applicationScope()
        val studySessionLogDao = entryPoint.studySessionLogDao() 

        // Dismiss notification immediately
        StudyCheckpointNotificationUtils.dismissNotification(context, logId)

        scope.launch {
            try {
                // Fetch the log first? Or just update fields needed?
                // Better to fetch to ensure existence
                // For simplicity, we directly call update status methods
                
                // Assuming we added updateStatus methods to DAO in next step or use existing update(log).
                val log = studySessionLogDao.getById(logId)
                if (log != null) {
                    val now = System.currentTimeMillis()
                    
                    when (action) {
                        StudyCheckpointNotificationUtils.ACTION_COMPLETED -> {
                            studySessionLogDao.update(log.copy(status = "COMPLETED", loggedAt = now))
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context.applicationContext, "Session marked as Completed via Checkpoint", Toast.LENGTH_SHORT).show()
                            }
                        }
                        StudyCheckpointNotificationUtils.ACTION_SKIPPED -> {
                            studySessionLogDao.update(log.copy(status = "SKIPPED", loggedAt = now))
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context.applicationContext, "Session marked as Skipped", Toast.LENGTH_SHORT).show()
                            }
                        }
                        StudyCheckpointNotificationUtils.ACTION_PARTIAL -> {
                            // Open Activity with dialog
                            val activityIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("partial_log_id", logId)
                            }
                            context.startActivity(activityIntent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CheckpointActionReceiver", "Error processing action: ${e.message}", e)
            }
        }
    }
}
