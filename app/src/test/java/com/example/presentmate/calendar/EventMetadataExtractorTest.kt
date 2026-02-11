package com.example.presentmate.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventMetadataExtractorTest {

    @Test
    fun `extract with dash separator returns subject and topic`() {
        val (subject, topic) = EventMetadataExtractor.extract("Modern History - Anglo Carnatic Wars")
        assertEquals("Modern History", subject)
        assertEquals("Anglo Carnatic Wars", topic)
    }

    @Test
    fun `extract with colon separator returns subject and topic`() {
        val (subject, topic) = EventMetadataExtractor.extract("Polity : Parliament Procedures")
        assertEquals("Polity", subject)
        assertEquals("Parliament Procedures", topic)
    }

    @Test
    fun `extract with pipe separator returns subject and topic`() {
        val (subject, topic) = EventMetadataExtractor.extract("GS Paper 2 | International Relations")
        assertEquals("GS Paper 2", subject)
        assertEquals("International Relations", topic)
    }
    
    @Test
    fun `extract with colons without spaces returns subject and topic`() {
        val (subject, topic) = EventMetadataExtractor.extract("Polity: Parliament")
        assertEquals("Polity", subject)
        assertEquals("Parliament", topic)
    }

    @Test
    fun `extract with no separator returns title as subject and null topic`() {
        val (subject, topic) = EventMetadataExtractor.extract("UPSC Revision")
        assertEquals("UPSC Revision", subject)
        assertNull(topic)
    }

    @Test
    fun `extract with empty title returns empty subject`() {
        val (subject, topic) = EventMetadataExtractor.extract("")
        assertEquals("", subject)
        assertNull(topic)
    }

    @Test
    fun `extract with multiple separators uses first one`() {
        val (subject, topic) = EventMetadataExtractor.extract("GS - Paper 1 - Geography")
        assertEquals("GS", subject)
        assertEquals("Paper 1 - Geography", topic)
    }
}
