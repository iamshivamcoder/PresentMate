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

                    val prefs = context.getSharedPreferences("session_reminder_prefs", Context.MODE_PRIVATE)
                    val progressReportEnabled = prefs.getBoolean("progress_report_enabled", true)

                    if (progressReportEnabled) {
                        val now = System.currentTimeMillis()
                        val triggerTime = event.endTime + TimeUnit.MINUTES.toMillis(delayMinutes.toLong())
                        val delay = (triggerTime - now).coerceAtLeast(0)

                        // Fix #16 — tag by event ID so we can cancel it if event is removed
                        val eventTag = "check_event_${event.id}"
                        val workRequest = OneTimeWorkRequestBuilder<StudySessionCheckWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(workDataOf("calendar_event_id" to event.id))
                            .addTag(eventTag)
                            .build()

                        WorkManager.getInstance(context).enqueueUniqueWork(
                            eventTag,
                            androidx.work.ExistingWorkPolicy.KEEP,  // don't re-schedule if already queued
                            workRequest
                        )
                        Log.d("CalendarSyncWorker", "Scheduled check for ${event.title} in ${delay/1000}s [tag=$eventTag]")
                    } else {
                        Log.d("CalendarSyncWorker", "Skipped scheduling check for ${event.title} (Disabled)")
                    }
                }
            }

            // Fix #16 — cancel check-workers for events that have been removed from the calendar
            val currentEventIds = events.map { it.id }.toSet()
            val allLogs = studySessionLogDao.getPendingLogs()
            for (log in allLogs) {
                if (log.calendarEventId !in currentEventIds) {
                    Log.d("CalendarSyncWorker", "Event ${log.calendarEventId} removed — cancelling check worker")
                    WorkManager.getInstance(context).cancelUniqueWork("check_event_${log.calendarEventId}")
                    studySessionLogDao.deleteByEventId(log.calendarEventId)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("CalendarSyncWorker", "Error syncing calendar", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME_IMMEDIATE = "calendar_sync_immediate"

        /** Run a one-shot CalendarSyncWorker immediately (e.g. on app launch). */
        fun runImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .addTag("calendar_sync_immediate")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                androidx.work.ExistingWorkPolicy.KEEP,  // don't re-run if already queued
                request
            )
        }
    }
}
