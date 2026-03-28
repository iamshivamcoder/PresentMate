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

/**
 * Notification helpers for the stair-step activity detection feature.
 */
object StepActivityNotificationUtils {

    private const val CHANNEL_ID = "step_activity_channel"
    private const val NOTIFICATION_ID_GOING_OUT  = 6001
    private const val NOTIFICATION_ID_GOING_HOME = 6002

    // Action constants handled by StepActivityReceiver
    const val ACTION_STEP_YES_START = "com.example.presentmate.ACTION_STEP_YES_START"
    const val ACTION_STEP_YES_END   = "com.example.presentmate.ACTION_STEP_YES_END"
    const val ACTION_STEP_NOT_NOW   = "com.example.presentmate.ACTION_STEP_NOT_NOW"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Activity Detection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart prompt when stair activity is detected"
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return true
    }

    /** Shows "Heading to the library? Start session!" notification (morning window). */
    fun showGoingOutNotification(context: Context) {
        if (!hasPermission(context)) return
        createChannel(context)

        val startIntent = Intent(context, StepActivityReceiver::class.java).apply {
            action = ACTION_STEP_YES_START
        }
        val startPending = PendingIntent.getBroadcast(
            context, 6011, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notNowIntent = Intent(context, StepActivityReceiver::class.java).apply {
            action = ACTION_STEP_NOT_NOW
        }
        val notNowPending = PendingIntent.getBroadcast(
            context, 6013, notNowIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context, 6014, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Heading to the library? 🏛️")
            .setContentText("Stair activity detected — shall I start your session now?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .addAction(0, "Yes, Start 📚", startPending)
            .addAction(0, "Not now", notNowPending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GOING_OUT, notification)
    }

    /** Shows "Heading home? Mark session done!" notification (evening window). */
    fun showGoingHomeNotification(context: Context) {
        if (!hasPermission(context)) return
        createChannel(context)

        val endIntent = Intent(context, StepActivityReceiver::class.java).apply {
            action = ACTION_STEP_YES_END
        }
        val endPending = PendingIntent.getBroadcast(
            context, 6021, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notNowIntent = Intent(context, StepActivityReceiver::class.java).apply {
            action = ACTION_STEP_NOT_NOW
        }
        val notNowPending = PendingIntent.getBroadcast(
            context, 6023, notNowIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Heading home? 🏠")
            .setContentText("Stair activity detected — shall I mark your session as done?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Yes, Done ✅", endPending)
            .addAction(0, "Not now", notNowPending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GOING_HOME, notification)
    }

    fun dismissAll(context: Context) {
        NotificationManagerCompat.from(context).apply {
            cancel(NOTIFICATION_ID_GOING_OUT)
            cancel(NOTIFICATION_ID_GOING_HOME)
        }
    }
}
