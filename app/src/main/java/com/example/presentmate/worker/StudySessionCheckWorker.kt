package com.example.presentmate.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.presentmate.db.StudySessionLogDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log

@HiltWorker
class StudySessionCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val studySessionLogDao: StudySessionLogDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val calendarEventId = inputData.getLong("calendar_event_id", -1)
        if (calendarEventId == -1L) return Result.failure()

        return try {
            val log = studySessionLogDao.getByEventId(calendarEventId)
            
            if (log != null && log.status == "PENDING") {
                // Confirm it's actually overdue? Worker delay should handle this, 
                // but we can double check strictly if needed.
                // Assuming worker triggers at correct time.
                
                StudyCheckpointNotificationUtils.showCheckpointNotification(
                    applicationContext,
                    log.id,
                    log.eventTitle,
                    log.subject,
                    log.topic
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("StudySessionCheckWorker", "Error checking session", e)
            Result.failure()
        }
    }
}
