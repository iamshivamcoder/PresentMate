package com.example.presentmate.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.presentmate.data.DriveSyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class DriveSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val driveSyncManager: DriveSyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DriveSyncWorker", "Starting background database backup to Google Drive...")
            
            val result = driveSyncManager.backupDatabaseToDrive()
            
            if (result.isSuccess) {
                Log.d("DriveSyncWorker", "Successfully backed up database to Drive.")
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                // If it's a UserRecoverableAuthIOException, we can't show UI from a Worker, 
                // so we fail the work. The user must initiate a manual sync from UI first.
                Log.e("DriveSyncWorker", "Failed to backup to Drive", exception)
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("DriveSyncWorker", "Exception in DriveSyncWorker", e)
            Result.retry()
        }
    }
}
