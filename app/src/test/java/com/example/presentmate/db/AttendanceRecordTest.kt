package com.example.presentmate.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceRecordTest {

    @Test
    fun `AttendanceRecord properties are set correctly`() {
        val date = 1672531200000L // Example date
        val timeIn = 1672560000000L
        val timeOut = 1672596000000L
        
        val record = AttendanceRecord(
            id = 1,
            date = date,
            timeIn = timeIn,
            timeOut = timeOut
        )
        
        assertEquals(1, record.id)
        assertEquals(date, record.date)
        assertEquals(timeIn, record.timeIn)
        assertEquals(timeOut, record.timeOut)
    }

    @Test
    fun `AttendanceRecord defaults to zero id and null times`() {
        val date = 1672531200000L
        
        val record = AttendanceRecord(
            date = date
        )
        
        assertEquals(0, record.id)
        assertNull(record.timeIn)
        assertNull(record.timeOut)
    }
    
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
            date = date,
            timeIn = timeIn,
            timeOut = timeOut,
            deletedAt = deletedAt
        )
        
        assertEquals(10, record.id)
        assertEquals(originalId, record.originalId)
        assertEquals(date, record.date)
        assertEquals(timeIn, record.timeIn)
        assertEquals(timeOut, record.timeOut)
        assertEquals(deletedAt, record.deletedAt)
    }

    @Test
    fun `DeletedRecord defaults to zero id and null times`() {
        val originalId = 5
        val date = 1672531200000L
        
        // deletedAt is evaluated at instantiation, so we can't test it for exact value easily without a small delta or mocking time
        val record = DeletedRecord(
            originalId = originalId,
            date = date
        )
        
        assertEquals(0, record.id)
        assertNull(record.timeIn)
        assertNull(record.timeOut)
        // deletedAt should be close to System.currentTimeMillis()
        val now = System.currentTimeMillis()
        assertEquals(true, now - record.deletedAt < 1000)
    }
}
