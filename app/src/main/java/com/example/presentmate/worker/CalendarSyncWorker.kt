package com.example.presentmate.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.presentmate.calendar.CalendarEventFilter
import com.example.presentmate.calendar.CalendarRepository
import com.example.presentmate.calendar.EventMetadataExtractor
import com.example.presentmate.data.CalendarSyncPreferences
import com.example.presentmate.db.StudySessionLog
import com.example.presentmate.db.StudySessionLogDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import android.util.Log

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarRepository: CalendarRepository,
    private val studySessionLogDao: StudySessionLogDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        
        // Check if feature enabled
        if (!CalendarSyncPreferences.isCalendarSyncEnabled(context)) {
            return Result.success() // Or failure to stop? Success is fine, just don't do anything.
        }

        val calendarId = CalendarSyncPreferences.getSelectedCalendarId(context)
        val keywords = CalendarSyncPreferences.getWhitelistKeywords(context)
        val delayMinutes = CalendarSyncPreferences.getDelayMinutes(context)

        return try {
            val events = calendarRepository.getTodayEvents(calendarId)

            for (event in events) {
                // Check if already logged
                val existingLog = studySessionLogDao.getByEventId(event.id)
                if (existingLog == null) {
                    val (subject, topic) = EventMetadataExtractor.extract(event.title)
                    
                    val newLog = StudySessionLog(
                        calendarEventId = event.id,
                        eventTitle = event.title,
                        subject = subject,
                        topic = topic,
                        scheduledStartTime = event.startTime,
                        scheduledEndTime = event.endTime,
                        status = "PENDING"
                    )
                    
                    studySessionLogDao.insert(newLog)
                    
                    // Schedule check worker if Progress Report preference is enabled
                    val prefs = context.getSharedPreferences("session_reminder_prefs", Context.MODE_PRIVATE)
                    val progressReportEnabled = prefs.getBoolean("progress_report_enabled", true)
                    
                    if (progressReportEnabled) {
                        val now = System.currentTimeMillis()
                        val triggerTime = event.endTime + TimeUnit.MINUTES.toMillis(delayMinutes.toLong())
                        val delay = (triggerTime - now).coerceAtLeast(0)
                        
                        val workRequest = OneTimeWorkRequestBuilder<StudySessionCheckWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(workDataOf(
                                "calendar_event_id" to event.id
                            ))
                            .build()

                        WorkManager.getInstance(context).enqueue(workRequest)
                        Log.d("CalendarSyncWorker", "Scheduled check for ${event.title} in ${delay/1000}s")
                    } else {
                        Log.d("CalendarSyncWorker", "Skipped scheduling check for ${event.title} (Disabled)")
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("CalendarSyncWorker", "Error syncing calendar", e)
            Result.failure()
        }
    }
}
