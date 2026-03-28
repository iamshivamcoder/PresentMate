package com.example.presentmate.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.presentmate.MainActivity
import com.example.presentmate.R

object StudyCheckpointNotificationUtils {
    private const val CHANNEL_ID = "study_checkpoint_channel"
    private const val NOTIFICATION_ID_BASE = 2000 // Base ID, added to log ID
    
    // Action Constants
    const val ACTION_COMPLETED = "com.example.presentmate.ACTION_COMPLETED"
    const val ACTION_PARTIAL = "com.example.presentmate.ACTION_PARTIAL"
    const val ACTION_SKIPPED = "com.example.presentmate.ACTION_SKIPPED"
    const val ACTION_OPEN_RECAP = "com.example.presentmate.ACTION_OPEN_RECAP"
    const val EXTRA_LOG_ID = "extra_log_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Study Checkpoints"
            val descriptionText = "Notifications to verify study sessions"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCheckpointNotification(
        context: Context,
        logId: Int,
        title: String,
        subject: String?,
        topic: String?
    ) {
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        // --- Intents ---

        // Body tap → open recap dialog in MainActivity
        val recapIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_RECAP
            putExtra(EXTRA_LOG_ID, logId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val recapPendingIntent = PendingIntent.getActivity(
            context,
            logId * 10 + 1,
            recapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Studied, Done!" button → same open-recap intent (same request code so same PendingIntent)
        val donePendingIntent = PendingIntent.getActivity(
            context,
            logId * 10 + 1,
            recapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skippedIntent = Intent(context, CheckpointActionReceiver::class.java).apply {
            action = ACTION_SKIPPED
            putExtra(EXTRA_LOG_ID, logId)
        }
        val skippedPendingIntent = PendingIntent.getBroadcast(
            context,
            logId * 10 + 3,
            skippedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val partialIntent = Intent(context, CheckpointActionReceiver::class.java).apply {
            action = ACTION_PARTIAL
            putExtra(EXTRA_LOG_ID, logId)
        }
        val partialPendingIntent = PendingIntent.getBroadcast(
            context,
            logId * 10 + 2,
            partialIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content Text
        val contentText = if (subject != null && topic != null) {
            "$subject · $topic  — Tap to log what you studied"
        } else {
            "Tap to log what you studied in today's session"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(recapPendingIntent)
            .addAction(0, "Studied, Done! 📖", donePendingIntent)
            .addAction(0, "⚖️ Partial", partialPendingIntent)
            .addAction(0, "❌ Skipped", skippedPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + logId, notification)
    }
    
    fun dismissNotification(context: Context, logId: Int) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BASE + logId)
    }
}
