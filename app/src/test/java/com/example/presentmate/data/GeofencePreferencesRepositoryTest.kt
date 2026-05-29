package com.example.presentmate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Uses a real Robolectric-backed SharedPreferences instead of mocks.
 * This is more accurate and avoids fragile mock-chain verification of the
 * androidx.core.content.edit { } extension.
 */
@RunWith(RobolectricTestRunner::class)
class GeofencePreferencesRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test to prevent inter-test pollution
        context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun `isGeofenceEnabled returns false by default`() {
        assertFalse(GeofencePreferencesRepository.isGeofenceEnabled(context))
    }

    @Test
    fun `setGeofenceEnabled persists value across read`() {
        GeofencePreferencesRepository.setGeofenceEnabled(context, true)
        assertTrue(GeofencePreferencesRepository.isGeofenceEnabled(context))

        GeofencePreferencesRepository.setGeofenceEnabled(context, false)
        assertFalse(GeofencePreferencesRepository.isGeofenceEnabled(context))
    }

    @Test
    fun `getGeofenceRadius returns 200f by default`() {
        assertEquals(200f, GeofencePreferencesRepository.getGeofenceRadius(context))
    }

    @Test
    fun `setGeofenceRadius persists value across read`() {
        GeofencePreferencesRepository.setGeofenceRadius(context, 500f)
        assertEquals(500f, GeofencePreferencesRepository.getGeofenceRadius(context))
    }

    @Test
    fun `getGeofenceLatitude returns 0f by default`() {
        assertEquals(0f, GeofencePreferencesRepository.getGeofenceLatitude(context))
    }

    @Test
    fun `getGeofenceLongitude returns 0f by default`() {
        assertEquals(0f, GeofencePreferencesRepository.getGeofenceLongitude(context))
    }

    @Test
    fun `setGeofenceLocation persists lat and lng`() {
        GeofencePreferencesRepository.setGeofenceLocation(context, 12.34f, 56.78f)
        assertEquals(12.34f, GeofencePreferencesRepository.getGeofenceLatitude(context))
        assertEquals(56.78f, GeofencePreferencesRepository.getGeofenceLongitude(context))
    }

    @Test
    fun `getGeofencePlaceId returns -1 by default`() {
        assertEquals(-1, GeofencePreferencesRepository.getGeofencePlaceId(context))
    }

    @Test
    fun `setGeofencePlaceId persists value across read`() {
        GeofencePreferencesRepository.setGeofencePlaceId(context, 42)
        assertEquals(42, GeofencePreferencesRepository.getGeofencePlaceId(context))
    }

    @Test
    fun `getGeofencePlaceName returns Work Location by default`() {
        assertEquals("Work Location", GeofencePreferencesRepository.getGeofencePlaceName(context))
    }

    @Test
    fun `setGeofencePlaceName persists value across read`() {
        GeofencePreferencesRepository.setGeofencePlaceName(context, "Home")
        assertEquals("Home", GeofencePreferencesRepository.getGeofencePlaceName(context))
    }

    @Test
    fun `saveGeofenceSettings persists all values at once`() {
        GeofencePreferencesRepository.saveGeofenceSettings(context, 10.0f, 20.0f, 150f, true)

        assertEquals(10.0f, GeofencePreferencesRepository.getGeofenceLatitude(context))
        assertEquals(20.0f, GeofencePreferencesRepository.getGeofenceLongitude(context))
        assertEquals(150f, GeofencePreferencesRepository.getGeofenceRadius(context))
        assertTrue(GeofencePreferencesRepository.isGeofenceEnabled(context))
    }
}
