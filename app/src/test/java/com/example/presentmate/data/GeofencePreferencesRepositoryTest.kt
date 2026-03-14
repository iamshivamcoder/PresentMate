package com.example.presentmate.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GeofencePreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
    }

    @Test
    fun `isGeofenceEnabled returns default value when not set`() {
        every { sharedPreferences.getBoolean("geofence_enabled", false) } returns false
        
        val result = GeofencePreferencesRepository.isGeofenceEnabled(context)
        
        assertEquals(false, result)
        verify { sharedPreferences.getBoolean("geofence_enabled", false) }
    }

    @Test
    fun `isGeofenceEnabled returns saved value`() {
        every { sharedPreferences.getBoolean("geofence_enabled", false) } returns true
        
        val result = GeofencePreferencesRepository.isGeofenceEnabled(context)
        
        assertEquals(true, result)
    }

    @Test
    fun `setGeofenceEnabled saves value`() {
        GeofencePreferencesRepository.setGeofenceEnabled(context, true)
        
        verify { editor.putBoolean("geofence_enabled", true) }
        verify { editor.apply() }
    }

    @Test
    fun `getGeofenceRadius returns default value when not set`() {
        every { sharedPreferences.getFloat("geofence_radius", 200f) } returns 200f
        
        val result = GeofencePreferencesRepository.getGeofenceRadius(context)
        
        assertEquals(200f, result)
        verify { sharedPreferences.getFloat("geofence_radius", 200f) }
    }

    @Test
    fun `getGeofenceRadius returns saved value`() {
        every { sharedPreferences.getFloat("geofence_radius", 200f) } returns 500f
        
        val result = GeofencePreferencesRepository.getGeofenceRadius(context)
        
        assertEquals(500f, result)
    }

    @Test
    fun `setGeofenceRadius saves value`() {
        GeofencePreferencesRepository.setGeofenceRadius(context, 300f)
        
        verify { editor.putFloat("geofence_radius", 300f) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getGeofenceLatitude returns default value when not set`() {
        every { sharedPreferences.getFloat("geofence_latitude", 0f) } returns 0f
        
        val result = GeofencePreferencesRepository.getGeofenceLatitude(context)
        
        assertEquals(0f, result)
    }
    
    @Test
    fun `getGeofenceLongitude returns default value when not set`() {
        every { sharedPreferences.getFloat("geofence_longitude", 0f) } returns 0f
        
        val result = GeofencePreferencesRepository.getGeofenceLongitude(context)
        
        assertEquals(0f, result)
    }
    
    @Test
    fun `setGeofenceLocation saves values`() {
        GeofencePreferencesRepository.setGeofenceLocation(context, 12.34f, 56.78f)
        
        verify { editor.putFloat("geofence_latitude", 12.34f) }
        verify { editor.putFloat("geofence_longitude", 56.78f) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getGeofencePlaceId returns default value when not set`() {
        every { sharedPreferences.getInt("geofence_place_id", -1) } returns -1
        
        val result = GeofencePreferencesRepository.getGeofencePlaceId(context)
        
        assertEquals(-1, result)
    }
    
    @Test
    fun `setGeofencePlaceId saves value`() {
        GeofencePreferencesRepository.setGeofencePlaceId(context, 42)
        
        verify { editor.putInt("geofence_place_id", 42) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getGeofencePlaceName returns default value when not set`() {
        every { sharedPreferences.getString("geofence_place_name", "Work Location") } returns "Work Location"
        
        val result = GeofencePreferencesRepository.getGeofencePlaceName(context)
        
        assertEquals("Work Location", result)
    }
    
    @Test
    fun `setGeofencePlaceName saves value`() {
        GeofencePreferencesRepository.setGeofencePlaceName(context, "Home")
        
        verify { editor.putString("geofence_place_name", "Home") }
        verify { editor.apply() }
    }
    
    @Test
    fun `saveGeofenceSettings performs bulk save`() {
        GeofencePreferencesRepository.saveGeofenceSettings(context, 10.0f, 20.0f, 150f, true)
        
        verify { editor.putFloat("geofence_latitude", 10.0f) }
        verify { editor.putFloat("geofence_longitude", 20.0f) }
        verify { editor.putFloat("geofence_radius", 150f) }
        verify { editor.putBoolean("geofence_enabled", true) }
        verify { editor.apply() }
    }

}
