package com.example.presentmate.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object GeofenceUtils {
    private const val GEOFENCE_REQUEST_CODE = 1001

    fun createGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}