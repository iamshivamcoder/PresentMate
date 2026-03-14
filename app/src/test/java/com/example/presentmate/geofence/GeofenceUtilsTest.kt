package com.example.presentmate.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class GeofenceUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `createGeofencePendingIntent returns configured PendingIntent`() {
        val pendingIntent = GeofenceUtils.createGeofencePendingIntent(context)
        
        assertNotNull(pendingIntent)
        
        val shadowPendingIntent = shadowOf(pendingIntent)
        assertTrue(shadowPendingIntent.isBroadcastIntent)
        
        val savedIntent = shadowPendingIntent.savedIntent
        assertEquals(GeofenceBroadcastReceiver::class.java.name, savedIntent.component?.className)
        
        // requestCode 1001 is used internally in GeofenceUtils
        assertEquals(1001, shadowPendingIntent.requestCode)
        
        // flags: FLAG_UPDATE_CURRENT (134217728) or FLAG_MUTABLE (33554432)
        val flags = shadowPendingIntent.flags
        assertTrue((flags and PendingIntent.FLAG_UPDATE_CURRENT) != 0)
        assertTrue((flags and PendingIntent.FLAG_MUTABLE) != 0)
    }
}
