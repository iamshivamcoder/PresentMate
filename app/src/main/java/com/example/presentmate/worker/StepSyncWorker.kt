package com.example.presentmate.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.db.StepActivityLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that reads a 5-second burst of the Android Step Counter sensor
 * and logs the result to [StepActivityLog].
 *
 * Default interval: 30 minutes (configurable via SharedPreferences key "step_sync_interval_minutes").
 */
@HiltWorker
class StepSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val isManual = inputData.getBoolean(KEY_MANUAL, false)
        Log.d(TAG, "StepSyncWorker started — manual=$isManual")

        return try {
            val steps = readStepDelta()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val window = when (hour) {
                in 9..9   -> "MORNING"
                in 20..20 -> "EVENING"
                else      -> "BACKGROUND"
            }

            val log = StepActivityLog(
                detectedAt    = System.currentTimeMillis(),
                stepCount     = steps,
                windowMinutes = getSyncInterval(appContext),
                type          = if (isManual) "MANUAL_SYNC" else "PERIODIC_SYNC",
                window        = window,
                triggered     = false
            )

            val db = PresentMateDatabase.getDatabase(appContext)
            db.stepActivityLogDao().insert(log)

            // Prune entries older than 30 days
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            db.stepActivityLogDao().deleteOlderThan(cutoff)

            Log.d(TAG, "Logged $steps steps (delta) in window=$window")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "StepSyncWorker failed", e)
            Result.failure()
        }
    }

    /**
     * Fix #8: StepActivityService increments "last_step_total" in SharedPrefs on every step.
     * We read the delta since the previous sync, then save the new baseline.
     * This avoids registering a sensor listener in WorkManager (which Android blocks when screen is off).
     */
    private fun readStepDelta(): Int {
        val prefs        = appContext.getSharedPreferences(STEP_PREFS, Context.MODE_PRIVATE)
        val currentTotal = prefs.getLong(KEY_LAST_STEP_TOTAL, -1L)
        val prevSyncBase = prefs.getLong(KEY_SYNC_BASE, -1L)

        if (currentTotal < 0) return 0          // service never ran / no sensor

        val delta = if (prevSyncBase < 0) 0
                    else (currentTotal - prevSyncBase).toInt().coerceAtLeast(0)

        // Save new baseline for next sync
        prefs.edit().putLong(KEY_SYNC_BASE, currentTotal).apply()
        return delta
    }

    companion object {
        private const val TAG                = "StepSyncWorker"
        private const val STEP_PREFS         = "step_sync_prefs"
        private const val KEY_LAST_STEP_TOTAL = "last_step_total"   // written by StepActivityService
        private const val KEY_SYNC_BASE       = "sync_base_total"   // last value read by this worker
        const val KEY_MANUAL                 = "is_manual"
        const val WORK_NAME_PERIODIC        = "step_sync_periodic"
        const val WORK_NAME_ONETIME         = "step_sync_manual"

        /** Default sync interval in minutes. */
        const val DEFAULT_INTERVAL_MINUTES  = 30

        private const val PREFS_NAME        = "step_sync_settings"
        const val PREF_INTERVAL_KEY         = "step_sync_interval_minutes"

        fun getSyncInterval(context: Context): Int {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(PREF_INTERVAL_KEY, DEFAULT_INTERVAL_MINUTES)
        }

        fun setSyncInterval(context: Context, minutes: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(PREF_INTERVAL_KEY, minutes.coerceAtLeast(15)).apply()
        }

        /** Enqueue/re-enqueue the periodic worker with the currently saved interval. */
        fun schedulePeriodicSync(context: Context) {
            val intervalMinutes = getSyncInterval(context).toLong()
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .addTag("step_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,   // re-enqueue with new interval when settings change
                request
            )
            Log.d(TAG, "Periodic step sync scheduled every $intervalMinutes min")
        }

        /** Cancel the periodic sync worker. */
        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }

        /** Run a one-shot manual sync immediately. */
        fun runManualSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<StepSyncWorker>()
                .setInputData(workDataOf(KEY_MANUAL to true))
                .addTag("step_sync_manual")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Manual step sync triggered")
        }
    }
}
