package com.example.presentmate.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.example.presentmate.db.AppDatabase
import com.example.presentmate.db.AttendanceRecord
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
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
            triggeringGeofences?.forEach { _ ->
                startSession(context)
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GeofenceReceiver", "Geofence Exited")
            triggeringGeofences?.forEach { _ ->
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

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    fun addGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        pendingIntent: PendingIntent
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("GeofenceManager", "Fine location permission not granted.")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence added successfully.")
            }
            .addOnFailureListener {
                Log.e("GeofenceManager", "Failed to add geofence.", it)
            }
    }

    fun removeGeofence(pendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(pendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence removed successfully.")
            }
            .addOnFailureListener {
                Log.e("GeofenceManager", "Failed to remove geofence.", it)
            }
    }
}