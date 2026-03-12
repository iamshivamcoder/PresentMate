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
import com.example.presentmate.R

object SessionReminderNotificationUtils {
    const val CHANNEL_ID = "session_reminder_channel"
    const val NOTIFICATION_ID = 3001

    const val ACTION_YESS = "com.example.presentmate.ACTION_YESS_SESSION"

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

        // "Yess" action — BroadcastReceiver will start session
        val yessIntent = Intent(context, SessionReminderReceiver::class.java).apply {
            action = ACTION_YESS
        }
        val yessPendingIntent = PendingIntent.getBroadcast(
            context,
            5001,
            yessIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap notification → open app
        val openAppIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val openAppPending = PendingIntent.getActivity(
            context, 5002, openAppIntent ?: Intent(), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Started the session? 📚")
            .setContentText("Good morning! Start your study session for today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .addAction(0, "Yess ✅", yessPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
