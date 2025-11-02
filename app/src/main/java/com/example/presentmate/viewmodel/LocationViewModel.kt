package com.example.presentmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationViewModel(context: Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE)

    private val _isTrackingEnabled = MutableStateFlow(prefs.getBoolean("geofence_enabled", false))
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled

    fun setTrackingEnabled(isEnabled: Boolean) {
        _isTrackingEnabled.value = isEnabled
        prefs.edit().putBoolean("geofence_enabled", isEnabled).apply()
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