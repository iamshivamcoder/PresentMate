package com.example.presentmate.calendar

object CalendarEventFilter {

    /**
     * Checks if the event title matches any of the whitelist keywords.
     * comparison is case-insensitive.
     * 
     * If keywords list is empty, it returns true (accepts all).
     */
    fun matchesKeywords(eventTitle: String, keywords: List<String>): Boolean {
        if (keywords.isEmpty()) return true
        
        val normalizeTitle = eventTitle.lowercase()
        return keywords.any { keyword ->
            normalizeTitle.contains(keyword.trim().lowercase())
        }
    }
}
