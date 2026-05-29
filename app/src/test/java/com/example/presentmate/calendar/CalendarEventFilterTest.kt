package com.example.presentmate.calendar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventFilterTest {

    @Test
    fun `matchesKeywords returns true when title contains keyword`() {
        val title = "UPSC Polity Session"
        val keywords = listOf("UPSC", "GS")
        assertTrue(CalendarEventFilter.matchesKeywords(title, keywords))
    }

    @Test
    fun `matchesKeywords is case insensitive`() {
        val title = "upsc revision"
        val keywords = listOf("UPSC")
        assertTrue(CalendarEventFilter.matchesKeywords(title, keywords))
    }

    @Test
    fun `matchesKeywords returns false when no keyword matches`() {
        val title = "Dentist Appointment"
        val keywords = listOf("UPSC", "GS")
        assertFalse(CalendarEventFilter.matchesKeywords(title, keywords))
    }

    @Test
    fun `matchesKeywords returns true when keywords list is empty`() {
        val title = "Anything"
        val keywords = emptyList<String>()
        assertTrue(CalendarEventFilter.matchesKeywords(title, keywords))
    }
    
    @Test
    fun `matchesKeywords handles partial matches correctly`() {
        val title = "History"
        val keywords = listOf("His")
        assertTrue(CalendarEventFilter.matchesKeywords(title, keywords))
    }

    @Test
    fun `matchesKeywords with blank title does not crash`() {
        val keywords = listOf("UPSC")
        // Blank title should return false without throwing
        assertFalse(CalendarEventFilter.matchesKeywords("", keywords))
        assertFalse(CalendarEventFilter.matchesKeywords("   ", keywords))
    }

    @Test
    fun `matchesKeywords with single character keyword matches correctly`() {
        assertTrue(CalendarEventFilter.matchesKeywords("Exam A", listOf("A")))
        assertFalse(CalendarEventFilter.matchesKeywords("Exam B", listOf("A")))
    }
}
