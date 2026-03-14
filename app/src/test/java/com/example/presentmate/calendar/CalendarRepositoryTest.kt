package com.example.presentmate.calendar

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
class CalendarRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: CalendarRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = CalendarRepository(context)
    }

    @Test
    fun `getCalendarList returns empty when permission not granted`() = runBlocking {
        // By default Robolectric does not grant permissions unless explicitly asked
        val shadowApp = shadowOf(context as android.app.Application)
        shadowApp.denyPermissions(Manifest.permission.READ_CALENDAR)
        
        val calendars = repository.getCalendarList()
        
        assertTrue(calendars.isEmpty())
    }

    @Test
    fun `getTodayEvents returns empty when permission not granted`() = runBlocking {
        val shadowApp = shadowOf(context as android.app.Application)
        shadowApp.denyPermissions(Manifest.permission.READ_CALENDAR)
        
        val events = repository.getTodayEvents()
        
        assertTrue(events.isEmpty())
    }
}
