package com.example.presentmate.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Centralized date and time formatters to eliminate duplicate SimpleDateFormat instances.
 * 
 * Note: SimpleDateFormat is not thread-safe, but since these are used on the main thread
 * for UI formatting, this is acceptable. For background thread usage, consider using
 * ThreadLocal or java.time APIs.
 */
object DateTimeFormatters {
    
    /** Format: "hh:mm a" (e.g., "02:30 PM") - for displaying time in UI */
    val timeFormat: SimpleDateFormat by lazy { 
        SimpleDateFormat("hh:mm a", Locale.getDefault()) 
    }
    
    /** Format: "MMM dd, yyyy" (e.g., "Dec 23, 2024") - for displaying dates in UI */
    val dateFormat: SimpleDateFormat by lazy { 
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) 
    }
    
    /** Format: "yyyy-MM-dd" - for export/import operations */
    val exportDateFormat: SimpleDateFormat by lazy { 
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) 
    }
    
    /** Format: "HH:mm:ss" - for export/import operations */
    val exportTimeFormat: SimpleDateFormat by lazy { 
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()) 
    }
    
    /**
     * Formats a timestamp to time string (e.g., "02:30 PM")
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a timestamp to date string (e.g., "Dec 23, 2024")
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Formats duration in milliseconds to human-readable string.
     * Examples: "2h 30m", "45m", "<1m"
     */
    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0) return "0m"
        
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
    
    /**
     * Formats duration from timeIn/timeOut pair to human-readable string.
     * Returns empty string if times are invalid.
     */
    fun formatDurationFromTimes(timeIn: Long?, timeOut: Long?): String {
        if (timeIn == null || timeOut == null || timeOut <= timeIn) {
            return ""
        }
        return formatDuration(timeOut - timeIn)
    }
}
