package com.example.presentmate.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.presentmate.data.GeofencePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationViewModel(private val context: Context) : ViewModel() {

    private val prefs = GeofencePreferencesRepository.getPreferences(context)

    private val _isTrackingEnabled = MutableStateFlow(GeofencePreferencesRepository.isGeofenceEnabled(context))
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled

    // Listener to sync state when preferences change from other screens
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "geofence_enabled") {
            _isTrackingEnabled.value = GeofencePreferencesRepository.isGeofenceEnabled(context)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    fun setTrackingEnabled(isEnabled: Boolean) {
        _isTrackingEnabled.value = isEnabled
        GeofencePreferencesRepository.setGeofenceEnabled(context, isEnabled)

        val geofenceManager = com.example.presentmate.geofence.GeofenceManager(context)
        val pendingIntent = com.example.presentmate.geofence.GeofenceUtils.createGeofencePendingIntent(context)

        if (isEnabled) {
            val placeId = prefs.getInt("geofence_place_id", -1)
            val lat = prefs.getFloat("geofence_latitude", 0f).toDouble()
            val lon = prefs.getFloat("geofence_longitude", 0f).toDouble()
            val radius = prefs.getFloat("geofence_radius", 200f)

            if (placeId != -1) {
                geofenceManager.addGeofence(
                    placeId.toString(),
                    lat,
                    lon,
                    radius,
                    pendingIntent
                )
            }
        } else {
            geofenceManager.removeGeofence(pendingIntent)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LocationViewModel(context) as T
                }
            }
        }
    }
}

