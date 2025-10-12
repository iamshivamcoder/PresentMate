package com.example.presentmate.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e("GeofenceReceiver", "Geofencing event has error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("GeofenceReceiver", "Geofence Entered")
            triggeringGeofences?.forEach { geofence ->
                startSession(context)
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GeofenceReceiver", "Geofence Exited")
            triggeringGeofences?.forEach { geofence ->
                endSession(context)
            }
        }
    }

    private fun startSession(context: Context) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            db.attendanceDao().insertRecord(
                AttendanceRecord(date = now, timeIn = now, timeOut = null)
            )
        }
    }

    private fun endSession(context: Context) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val ongoingSession = db.attendanceDao().getOngoingSession()
            ongoingSession?.let {
                db.attendanceDao().updateRecord(
                    it.copy(timeOut = System.currentTimeMillis())
                )
            }
        }
    }
}