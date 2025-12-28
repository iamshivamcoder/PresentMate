package com.example.presentmate.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.example.presentmate.db.AttendanceRecord
import com.example.presentmate.di.getEntryPoint
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Maps geofence error codes to user-friendly messages
 */
object GeofenceErrorMessages {
    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> 
                "Geofence service not available. Please enable Location Services in Settings."
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> 
                "Too many geofences registered. Please remove some."
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> 
                "Too many pending intents. Please restart the app."
            GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION ->
                "Location permission required. Please grant background location access."
            else -> "Geofence error (code: $errorCode)"
        }
    }
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = getEntryPoint(context)
        val applicationScope = entryPoint.applicationScope()
        val attendanceDao = entryPoint.attendanceDao()

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceErrorMessages.getErrorMessage(geofencingEvent.errorCode)
            Log.e("GeofenceReceiver", "Geofencing event error: $errorMessage")
            GeofenceNotificationUtils.showGeofenceErrorNotification(context, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("GeofenceReceiver", "Geofence Entered")
            applicationScope.launch {
                try {
                    val now = System.currentTimeMillis()
                    val ongoingSession = attendanceDao.getOngoingSessionFlow().first()
                    if (ongoingSession == null) {
                        attendanceDao.insertRecord(
                            AttendanceRecord(date = now, timeIn = now, timeOut = null)
                        )
                        Log.d("GeofenceReceiver", "Session started via geofence")
                        GeofenceNotificationUtils.showGeofenceEnterNotification(
                            context,
                            "Work Location"
                        )
                    } else {
                        Log.d("GeofenceReceiver", "Session already active")
                    }
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Error starting session via geofence", e)
                    GeofenceNotificationUtils.showGeofenceErrorNotification(
                        context,
                        "Error starting session: ${e.message}"
                    )
                }
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GeofenceReceiver", "Geofence Exited")
            applicationScope.launch {
                try {
                    val ongoingSession = attendanceDao.getOngoingSessionFlow().first()
                    if (ongoingSession != null) {
                        attendanceDao.updateRecord(
                            ongoingSession.copy(timeOut = System.currentTimeMillis())
                        )
                        Log.d("GeofenceReceiver", "Session ended via geofence")
                        GeofenceNotificationUtils.showGeofenceExitNotification(context, "Work Location")
                    } else {
                        Log.d("GeofenceReceiver", "No ongoing session to end")
                    }
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Error ending session via geofence", e)
                    GeofenceNotificationUtils.showGeofenceErrorNotification(
                        context,
                        "Error ending session: ${e.message}"
                    )
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

    /**
     * Checks if location services are enabled
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun addGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        pendingIntent: PendingIntent
    ) {
        // Pre-check: Location services must be enabled
        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services are disabled")
            GeofenceNotificationUtils.showGeofenceErrorNotification(
                context,
                "Please enable Location Services in Settings to use geofencing."
            )
            return
        }

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
                    "Background location permission required. Go to Settings > Apps > PresentMate > Permissions > Location > Allow all the time"
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
        removeGeofence(pendingIntent) { _ ->
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added successfully for ID: $id")
                    GeofenceNotificationUtils.showGeofenceEnterNotification(
                        context,
                        "Geofence activated for $id"
                    )
                }
                .addOnFailureListener { exception ->
                    val errorCode = (exception as? com.google.android.gms.common.api.ApiException)?.statusCode ?: -1
                    val errorMessage = GeofenceErrorMessages.getErrorMessage(errorCode)
                    Log.e(TAG, "Failed to add geofence: $errorMessage", exception)
                    GeofenceNotificationUtils.showGeofenceErrorNotification(context, errorMessage)
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
