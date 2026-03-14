package com.example.presentmate.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudySessionLogTest {

    @Test
    fun `StudySessionLog properties are set correctly`() {
        val calendarEventId = 100L
        val eventTitle = "History Class"
        val subject = "Modern History"
        val topic = "Revolt of 1857"
        val scheduledStartTime = 1672560000000L
        val scheduledEndTime = 1672596000000L
        val status = "COMPLETED"
        val actualDurationMinutes = 60
        val recallNote = "Started from Meerut"
        val loggedAt = 1672600000000L
        
        val record = StudySessionLog(
            id = 1,
            calendarEventId = calendarEventId,
            eventTitle = eventTitle,
            subject = subject,
            topic = topic,
            scheduledStartTime = scheduledStartTime,
            scheduledEndTime = scheduledEndTime,
            status = status,
            actualDurationMinutes = actualDurationMinutes,
            recallNote = recallNote,
            loggedAt = loggedAt
        )
        
        assertEquals(1, record.id)
        assertEquals(calendarEventId, record.calendarEventId)
        assertEquals(eventTitle, record.eventTitle)
        assertEquals(subject, record.subject)
        assertEquals(topic, record.topic)
        assertEquals(scheduledStartTime, record.scheduledStartTime)
        assertEquals(scheduledEndTime, record.scheduledEndTime)
        assertEquals(status, record.status)
        assertEquals(actualDurationMinutes, record.actualDurationMinutes)
        assertEquals(recallNote, record.recallNote)
        assertEquals(loggedAt, record.loggedAt)
    }

    @Test
    fun `StudySessionLog defaults are set correctly`() {
        val calendarEventId = 200L
        val eventTitle = "Math Practice"
        val scheduledStartTime = 1672560000000L
        val scheduledEndTime = 1672596000000L
        
        val record = StudySessionLog(
            calendarEventId = calendarEventId,
            eventTitle = eventTitle,
            subject = null,
            topic = null,
            scheduledStartTime = scheduledStartTime,
            scheduledEndTime = scheduledEndTime
        )
        
        assertEquals(0, record.id)
        assertEquals("PENDING", record.status)
        assertNull(record.actualDurationMinutes)
        assertNull(record.recallNote)
        assertNull(record.loggedAt)
    }
}
