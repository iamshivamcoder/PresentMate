package com.example.presentmate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Centralized repository for all geofence-related SharedPreferences access.
 * This eliminates duplicate SharedPreferences initialization across the codebase.
 */
object GeofencePreferencesRepository {
    private const val PREFS_NAME = "geofence_prefs"
    
    // Keys
    private const val KEY_ENABLED = "geofence_enabled"
    private const val KEY_RADIUS = "geofence_radius"
    private const val KEY_LATITUDE = "geofence_latitude"
    private const val KEY_LONGITUDE = "geofence_longitude"
    private const val KEY_PLACE_ID = "geofence_place_id"
    
    // Default values
    private const val DEFAULT_RADIUS = 200f
    private const val DEFAULT_ENABLED = false
    private const val DEFAULT_PLACE_ID = -1
    
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // --- Enabled State ---
    fun isGeofenceEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
    }
    
    fun setGeofenceEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }
    
    // --- Radius ---
    fun getGeofenceRadius(context: Context): Float {
        return getPreferences(context).getFloat(KEY_RADIUS, DEFAULT_RADIUS)
    }
    
    fun setGeofenceRadius(context: Context, radius: Float) {
        getPreferences(context).edit { putFloat(KEY_RADIUS, radius) }
    }
    
    // --- Location ---
    fun getGeofenceLatitude(context: Context): Float {
        return getPreferences(context).getFloat(KEY_LATITUDE, 0f)
    }
    
    fun getGeofenceLongitude(context: Context): Float {
        return getPreferences(context).getFloat(KEY_LONGITUDE, 0f)
    }
    
    fun setGeofenceLocation(context: Context, latitude: Float, longitude: Float) {
        getPreferences(context).edit {
            putFloat(KEY_LATITUDE, latitude)
            putFloat(KEY_LONGITUDE, longitude)
        }
    }
    
    // --- Place ID ---
    fun getGeofencePlaceId(context: Context): Int {
        return getPreferences(context).getInt(KEY_PLACE_ID, DEFAULT_PLACE_ID)
    }
    
    fun setGeofencePlaceId(context: Context, placeId: Int) {
        getPreferences(context).edit { putInt(KEY_PLACE_ID, placeId) }
    }
    
    // --- Bulk save ---
    fun saveGeofenceSettings(
        context: Context,
        latitude: Float,
        longitude: Float,
        radius: Float,
        enabled: Boolean
    ) {
        getPreferences(context).edit {
            putFloat(KEY_LATITUDE, latitude)
            putFloat(KEY_LONGITUDE, longitude)
            putFloat(KEY_RADIUS, radius)
            putBoolean(KEY_ENABLED, enabled)
        }
    }
}
