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

object SessionReminderNotificationUtils {
    const val CHANNEL_ID = "session_reminder_channel"
    const val NOTIFICATION_ID = 3001

    const val ACTION_YESS = "com.example.presentmate.ACTION_YESS_SESSION"
    const val ACTION_OPEN_REMINDER_DIALOG = "open_reminder_dialog"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Session Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminder to start your study session"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showSessionReminderNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        createNotificationChannel(context)

        // "Yess" action — BroadcastReceiver will start session directly
        val yessIntent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = ACTION_YESS
        }
        val yessPendingIntent = PendingIntent.getBroadcast(
            context,
            5001,
            yessIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap notification body → open the 3-section reminder dialog in MainActivity
        val dialogIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_REMINDER_DIALOG
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val dialogPendingIntent = PendingIntent.getActivity(
            context,
            5002,
            dialogIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to head to the library? 📚")
            .setContentText("Tap to set a reminder, log your start time, or mark leave.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(dialogPendingIntent)
            .addAction(0, "Yess, Starting! ✅", yessPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
