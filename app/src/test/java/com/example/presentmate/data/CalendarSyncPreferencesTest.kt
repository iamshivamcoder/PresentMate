package com.example.presentmate.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalendarSyncPreferencesTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("calendar_sync_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
    }

    @Test
    fun `isCalendarSyncEnabled returns default value when not set`() {
        every { sharedPreferences.getBoolean("calendar_sync_enabled", false) } returns false
        
        val result = CalendarSyncPreferences.isCalendarSyncEnabled(context)
        
        assertEquals(false, result)
        verify { sharedPreferences.getBoolean("calendar_sync_enabled", false) }
    }

    @Test
    fun `isCalendarSyncEnabled returns saved value`() {
        every { sharedPreferences.getBoolean("calendar_sync_enabled", false) } returns true
        
        val result = CalendarSyncPreferences.isCalendarSyncEnabled(context)
        
        assertEquals(true, result)
    }

    @Test
    fun `setCalendarSyncEnabled saves value`() {
        CalendarSyncPreferences.setCalendarSyncEnabled(context, true)
        
        verify { editor.putBoolean("calendar_sync_enabled", true) }
        verify { editor.apply() }
    }

    @Test
    fun `getSelectedCalendarId returns default value when not set`() {
        every { sharedPreferences.getLong("calendar_id", -1L) } returns -1L
        
        val result = CalendarSyncPreferences.getSelectedCalendarId(context)
        
        assertEquals(-1L, result)
        verify { sharedPreferences.getLong("calendar_id", -1L) }
    }

    @Test
    fun `getSelectedCalendarId returns saved value`() {
        every { sharedPreferences.getLong("calendar_id", -1L) } returns 123L
        
        val result = CalendarSyncPreferences.getSelectedCalendarId(context)
        
        assertEquals(123L, result)
    }

    @Test
    fun `setSelectedCalendarId saves value`() {
        CalendarSyncPreferences.setSelectedCalendarId(context, 456L)
        
        verify { editor.putLong("calendar_id", 456L) }
        verify { editor.apply() }
    }

    @Test
    fun `getWhitelistKeywords returns default value when not set`() {
        every { sharedPreferences.getString("whitelist_keywords", "UPSC,Polity,GS,Optional,CSAT") } returns "UPSC,Polity,GS,Optional,CSAT"
        
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        
        assertEquals(listOf("UPSC", "Polity", "GS", "Optional", "CSAT"), result)
        verify { sharedPreferences.getString("whitelist_keywords", "UPSC,Polity,GS,Optional,CSAT") }
    }

    @Test
    fun `getWhitelistKeywords returns customized value`() {
        every { sharedPreferences.getString("whitelist_keywords", any()) } returns " Meeting , Work ,  Important "
        
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        
        assertEquals(listOf("Meeting", "Work", "Important"), result)
    }

    @Test
    fun `getWhitelistKeywords handles empty string`() {
        every { sharedPreferences.getString("whitelist_keywords", any()) } returns ""
        
        val result = CalendarSyncPreferences.getWhitelistKeywords(context)
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun `setWhitelistKeywords saves value`() {
        CalendarSyncPreferences.setWhitelistKeywords(context, listOf("Meeting", "Work"))
        
        verify { editor.putString("whitelist_keywords", "Meeting,Work") }
        verify { editor.apply() }
    }

    @Test
    fun `getDelayMinutes returns default value when not set`() {
        every { sharedPreferences.getInt("delay_minutes", 2) } returns 2
        
        val result = CalendarSyncPreferences.getDelayMinutes(context)
        
        assertEquals(2, result)
        verify { sharedPreferences.getInt("delay_minutes", 2) }
    }

    @Test
    fun `getDelayMinutes returns saved value`() {
        every { sharedPreferences.getInt("delay_minutes", 2) } returns 5
        
        val result = CalendarSyncPreferences.getDelayMinutes(context)
        
        assertEquals(5, result)
    }

    @Test
    fun `setDelayMinutes saves value`() {
        CalendarSyncPreferences.setDelayMinutes(context, 10)
        
        verify { editor.putInt("delay_minutes", 10) }
        verify { editor.apply() }
    }
}
