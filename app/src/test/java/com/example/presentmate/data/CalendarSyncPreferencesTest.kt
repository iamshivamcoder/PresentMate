package com.example.presentmate.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Uses real Robolectric-backed SharedPreferences instead of mocks.
 * Read-after-write pattern: set a value, then read it back and assert equality.
 * This approach is more reliable than verifying mock editor method calls.
 */
@RunWith(RobolectricTestRunner::class)
class CalendarSyncPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test to prevent inter-test pollution
        context.getSharedPreferences("calendar_sync_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun `isCalendarSyncEnabled returns false by default`() {
        assertEquals(false, CalendarSyncPreferences.isCalendarSyncEnabled(context))
    }

    @Test
    fun `setCalendarSyncEnabled persists value across read`() {
        CalendarSyncPreferences.setCalendarSyncEnabled(context, true)
        assertEquals(true, CalendarSyncPreferences.isCalendarSyncEnabled(context))

        CalendarSyncPreferences.setCalendarSyncEnabled(context, false)
        assertEquals(false, CalendarSyncPreferences.isCalendarSyncEnabled(context))
    }

    @Test
    fun `getSelectedCalendarId returns -1L by default`() {
        assertEquals(-1L, CalendarSyncPreferences.getSelectedCalendarId(context))
    }

    @Test
    fun `setSelectedCalendarId persists value across read`() {
        CalendarSyncPreferences.setSelectedCalendarId(context, 123L)
        assertEquals(123L, CalendarSyncPreferences.getSelectedCalendarId(context))
    }

    @Test
    fun `getWhitelistKeywords returns default keywords when not set`() {
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        // Default is "UPSC,Polity,GS,Optional,CSAT"
        assertEquals(listOf("UPSC", "Polity", "GS", "Optional", "CSAT"), result)
    }

    @Test
    fun `setWhitelistKeywords persists and returns correctly split list`() {
        CalendarSyncPreferences.setWhitelistKeywords(context, listOf("Meeting", "Work"))
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        assertEquals(listOf("Meeting", "Work"), result)
    }

    @Test
    fun `setWhitelistKeywords with spaces trims entries on read`() {
        // Store with spaces intentionally
        CalendarSyncPreferences.setWhitelistKeywords(context, listOf(" Meeting ", " Work ", "  Important "))
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        // getWhitelistKeywords should trim each entry
        assertEquals(listOf("Meeting", "Work", "Important"), result)
    }

    @Test
    fun `setWhitelistKeywords with empty list results in empty on read`() {
        CalendarSyncPreferences.setWhitelistKeywords(context, emptyList())
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getDelayMinutes returns 2 by default`() {
        assertEquals(2, CalendarSyncPreferences.getDelayMinutes(context))
    }

    @Test
    fun `setDelayMinutes persists value across read`() {
        CalendarSyncPreferences.setDelayMinutes(context, 10)
        assertEquals(10, CalendarSyncPreferences.getDelayMinutes(context))
    }
}
