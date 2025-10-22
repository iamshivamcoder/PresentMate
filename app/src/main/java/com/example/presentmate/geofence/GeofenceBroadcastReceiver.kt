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
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.di.getEntryPoint
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = getEntryPoint(context)
        val applicationScope = entryPoint.applicationScope()
        val db = entryPoint.appDatabase()

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e("GeofenceReceiver", "Geofencing event has error: ${geofencingEvent.errorCode}")
            // Show user-facing error notification
            GeofenceNotificationUtils.showGeofenceErrorNotification(
                context,
                "Geofencing error occurred: ${geofencingEvent.errorCode}"
            )
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("GeofenceReceiver", "Geofence Entered")
            // Handle enter event for each triggering geofence
            triggeringGeofences?.forEach { geofence ->
                applicationScope.launch {
                    try {
                        val now = System.currentTimeMillis()
                        // Check if there's already an ongoing session
                        val ongoingSession = db.attendanceDao().getOngoingSession()
                        if (ongoingSession == null) {
                            db.attendanceDao().insertRecord(
                                AttendanceRecord(date = now, timeIn = now, timeOut = null)
                            )
                            Log.d("GeofenceReceiver", "Session started for geofence: ${geofence.requestId}")
                            GeofenceNotificationUtils.showGeofenceEnterNotification(
                                context,
                                "Work Location"
                            )
                        } else {
                            Log.d("GeofenceReceiver", "Session already active")
                        }
                    } catch (e: Exception) {
                        Log.e("GeofenceReceiver", "Error starting session", e)
                        GeofenceNotificationUtils.showGeofenceErrorNotification(
                            context,
                            "Error starting session: ${e.message}"
                        )
                    }
                }
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GeofenceReceiver", "Geofence Exited")
            // Handle exit event for each triggering geofence
            triggeringGeofences?.forEach { geofence ->
                applicationScope.launch {
                    try {
                        val ongoingSession = db.attendanceDao().getOngoingSession()
                        if (ongoingSession != null) {
                            db.attendanceDao().updateRecord(
                                ongoingSession.copy(timeOut = System.currentTimeMillis())
                            )
                            Log.d("GeofenceReceiver", "Session ended")
                            GeofenceNotificationUtils.showGeofenceExitNotification(context, "Work Location")
                        } else {
                            Log.d("GeofenceReceiver", "No ongoing session to end")
                        }
                    } catch (e: Exception) {
                        Log.e("GeofenceReceiver", "Error ending session", e)
                        GeofenceNotificationUtils.showGeofenceErrorNotification(
                            context,
                            "Error ending session: ${e.message}"
                        )
                    }
                }
            }
        } else {
            Log.d("GeofenceReceiver", "Invalid transition type: $geofenceTransition")
        }
    }
}

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val TAG = "GeofenceManager"

    fun addGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        pendingIntent: PendingIntent
    ) {
        // Check fine location permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Fine location permission not granted.")
            GeofenceNotificationUtils.showGeofenceErrorNotification(
                context,
                "Location permission not granted"
            )
            return
        }

        // For Android 10+ (API 29+), we also need background location permission for geofencing
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Background location permission not granted for Android 10+.")
                GeofenceNotificationUtils.showGeofenceErrorNotification(
                    context,
                    "Background location permission required for geofencing"
                )
                return
            }
        }

        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setLoiteringDelay(60000) // 1 minute delay before triggering dwell events (if needed)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Remove existing geofence before adding new one to prevent conflicts
        removeGeofence(pendingIntent) { success ->
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added successfully for ID: $id")
                    GeofenceNotificationUtils.showGeofenceEnterNotification(
                        context,
                        "Geofence activated for $id"
                    )
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to add geofence for ID: $id", exception)
                    GeofenceNotificationUtils.showGeofenceErrorNotification(
                        context,
                        "Failed to activate geofence: ${exception.message}"
                    )
                }
        }
    }

    fun removeGeofence(pendingIntent: PendingIntent, callback: ((Boolean) -> Unit)? = null) {
        geofencingClient.removeGeofences(pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed successfully.")
                callback?.invoke(true)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove geofence.", exception)
                callback?.invoke(false)
            }
    }
}