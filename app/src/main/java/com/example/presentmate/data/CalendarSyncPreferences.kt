package com.example.presentmate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object CalendarSyncPreferences {
    private const val PREFS_NAME = "calendar_sync_prefs"
    
    // Keys
    private const val KEY_ENABLED = "calendar_sync_enabled"
    private const val KEY_CALENDAR_ID = "calendar_id"
    private const val KEY_KEYWORDS = "whitelist_keywords"
    private const val KEY_DELAY_MINUTES = "delay_minutes"
    
    // Default values
    private const val DEFAULT_ENABLED = false
    private const val DEFAULT_CALENDAR_ID = -1L // -1 means all calendars (or none selected yet)
    private const val DEFAULT_KEYWORDS = "UPSC,Polity,GS,Optional,CSAT"
    private const val DEFAULT_DELAY_MINUTES = 2
    
    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // --- Enabled State ---
    fun isCalendarSyncEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
    }
    
    fun setCalendarSyncEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }
    
    // --- Calendar ID ---
    fun getSelectedCalendarId(context: Context): Long {
        return getPreferences(context).getLong(KEY_CALENDAR_ID, DEFAULT_CALENDAR_ID)
    }
    
    fun setSelectedCalendarId(context: Context, calendarId: Long) {
        getPreferences(context).edit { putLong(KEY_CALENDAR_ID, calendarId) }
    }
    
    // --- Keywords ---
    fun getWhitelistKeywords(context: Context): List<String> {
        val keywordsString = getPreferences(context).getString(KEY_KEYWORDS, DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS
        return keywordsString.split(",").filter { it.isNotBlank() }.map { it.trim() }
    }
    
    fun setWhitelistKeywords(context: Context, keywords: List<String>) {
        val keywordsString = keywords.joinToString(",")
        getPreferences(context).edit { putString(KEY_KEYWORDS, keywordsString) }
    }

    // --- Delay ---
    fun getDelayMinutes(context: Context): Int {
        return getPreferences(context).getInt(KEY_DELAY_MINUTES, DEFAULT_DELAY_MINUTES)
    }
    
    fun setDelayMinutes(context: Context, minutes: Int) {
        getPreferences(context).edit { putInt(KEY_DELAY_MINUTES, minutes) }
    }
}
