package com.example.presentmate.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceRecordTest {

    // ---------------------------------------------------------------
    // AttendanceRecord
    // ---------------------------------------------------------------

    @Test
    fun `AttendanceRecord properties are set correctly`() {
        val date = 1672531200000L
        val timeIn = 1672560000000L
        val timeOut = 1672596000000L

        val record = AttendanceRecord(
            id = 1,
            userId = "test_user",
            date = date,
            timeIn = timeIn,
            timeOut = timeOut
        )

        assertEquals(1, record.id)
        assertEquals("test_user", record.userId)
        assertEquals(date, record.date)
        assertEquals(timeIn, record.timeIn)
        assertEquals(timeOut, record.timeOut)
    }

    @Test
    fun `AttendanceRecord defaults to zero id and null times`() {
        val date = 1672531200000L

        val record = AttendanceRecord(
            userId = "test_user",
            date = date
        )

        assertEquals(0, record.id)
        assertEquals("test_user", record.userId)
        assertNull(record.timeIn)
        assertNull(record.timeOut)
    }

    @Test
    fun `AttendanceRecord userId defaults to empty string when not set`() {
        val record = AttendanceRecord(date = 1672531200000L)
        assertEquals("", record.userId)
    }

    @Test
    fun `record with userId persists correctly`() {
        val record = AttendanceRecord(
            userId = "user_abc123",
            date = 1672531200000L,
            timeIn = 1672560000000L,
            timeOut = 1672596000000L
        )
        assertEquals("user_abc123", record.userId)
        // Copying the record must preserve userId
        val copy = record.copy(timeOut = null)
        assertEquals("user_abc123", copy.userId)
        assertNull(copy.timeOut)
    }

    // ---------------------------------------------------------------
    // DeletedRecord
    // ---------------------------------------------------------------

    @Test
    fun `DeletedRecord properties are set correctly`() {
        val originalId = 5
        val date = 1672531200000L
        val timeIn = 1672560000000L
        val timeOut = 1672596000000L
        val deletedAt = 1672600000000L

        val record = DeletedRecord(
            id = 10,
            originalId = originalId,
            userId = "test_user",
            date = date,
            timeIn = timeIn,
            timeOut = timeOut,
            deletedAt = deletedAt
        )

        assertEquals(10, record.id)
        assertEquals(originalId, record.originalId)
        assertEquals("test_user", record.userId)
        assertEquals(date, record.date)
        assertEquals(timeIn, record.timeIn)
        assertEquals(timeOut, record.timeOut)
        assertEquals(deletedAt, record.deletedAt)
    }

    @Test
    fun `DeletedRecord defaults to zero id and null times`() {
        val originalId = 5
        val date = 1672531200000L

        val record = DeletedRecord(
            originalId = originalId,
            userId = "test_user",
            date = date
        )

        assertEquals(0, record.id)
        assertEquals("test_user", record.userId)
        assertNull(record.timeIn)
        assertNull(record.timeOut)
        // deletedAt should be close to System.currentTimeMillis()
        val now = System.currentTimeMillis()
        assertEquals(true, now - record.deletedAt < 1000)
    }
}
