package com.example.presentmate.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

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

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)?.run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence added successfully.")
            }
            addOnFailureListener {
                Log.e("GeofenceManager", "Failed to add geofence.", it)
            }
        }
    }

    fun removeGeofence(pendingIntent: PendingIntent) {
        geofencingClient.removeGeofences(pendingIntent)?.run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence removed successfully.")
            }
            addOnFailureListener {
                Log.e("GeofenceManager", "Failed to remove geofence.", it)
            }
        }
    }
}