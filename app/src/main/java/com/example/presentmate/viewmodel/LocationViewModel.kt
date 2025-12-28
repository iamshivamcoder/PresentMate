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

