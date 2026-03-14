package com.example.presentmate.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.presentmate.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log

@HiltWorker
class CustomReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val message = inputData.getString("reminder_message") ?: "You have a custom reminder!"
        val notificationId = inputData.getInt("notification_id", System.currentTimeMillis().toInt())

        showNotification(applicationContext, message, notificationId)
        
        return Result.success()
    }

    private fun showNotification(context: Context, message: String, notificationId: Int) {
        val channelId = "custom_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Custom Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom scheduled reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a solid icon if available
            .setContentTitle("Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
        Log.d("CustomReminderWorker", "Fired custom reminder: $message")
    }
}
