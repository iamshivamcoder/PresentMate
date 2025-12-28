package com.example.presentmate.utils

import android.content.Context
import android.location.LocationManager

/**
 * Utility object for location-related operations.
 * Centralizes location services check pattern that was duplicated across screens.
 */
object LocationUtils {
    
    /**
     * Checks if location services (GPS or Network) are enabled on the device.
     * 
     * @param context The application context
     * @return true if either GPS or Network location provider is enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
