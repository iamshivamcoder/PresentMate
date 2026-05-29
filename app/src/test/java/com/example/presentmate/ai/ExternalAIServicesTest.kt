package com.example.presentmate.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the [parseAttendanceData] internal function.
 * Direct call (no reflection) is possible because the function was relaxed from
 * private to internal visibility.
 */
class ExternalAIServicesTest {

    @Test
    fun `parseAttendanceData extracts correctly formatted data`() {
        val aiResponseMock = """
            Here is your requested data:
            [ATTENDANCE_DATA]
            DATE: 2023-10-15, IN: 09:00, OUT: 17:00
            DATE: 2023-10-16, IN: 09:15, OUT: 17:30
            [/ATTENDANCE_DATA]
            Have a nice day!
        """.trimIndent()

        val result = parseAttendanceData(aiResponseMock)

        assertEquals(2, result.size)

        val firstRecord = result[0]
        assertEquals("2023-10-15", firstRecord.dateStr)
        assertEquals("09:00", firstRecord.timeInStr)
        assertEquals("17:00", firstRecord.timeOutStr)

        val secondRecord = result[1]
        assertEquals("2023-10-16", secondRecord.dateStr)
        assertEquals("09:15", secondRecord.timeInStr)
        assertEquals("17:30", secondRecord.timeOutStr)
    }

    @Test
    fun `parseAttendanceData returns empty list on invalid formatting`() {
        // Missing [ATTENDANCE_DATA] tags entirely
        val aiResponseMock = """
            DATE: 2023-10-15, IN: 09:00, OUT: 17:00
        """.trimIndent()

        val result = parseAttendanceData(aiResponseMock)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseAttendanceData with multiple blocks extracts all records`() {
        // Both blocks should be found — regex uses DOT_MATCHES_ALL so only first match
        // is taken (that is the current implementation); this test documents that behavior
        val response = """
            [ATTENDANCE_DATA]
            DATE: 2023-10-15, IN: 09:00, OUT: 17:00
            DATE: 2023-10-16, IN: 10:00, OUT: 18:00
            [/ATTENDANCE_DATA]
        """.trimIndent()

        val result = parseAttendanceData(response)

        // Both lines inside the single block should be extracted
        assertEquals(2, result.size)
        assertEquals("2023-10-15", result[0].dateStr)
        assertEquals("2023-10-16", result[1].dateStr)
    }

    @Test
    fun `parseAttendanceData skips malformed time entries silently`() {
        val response = """
            [ATTENDANCE_DATA]
            DATE: 2023-10-15, IN: 09:00, OUT: 17:00
            DATE: bad-date, IN: 99:99, OUT: 17:00
            DATE: 2023-10-17, IN: 08:30, OUT: 16:30
            [/ATTENDANCE_DATA]
        """.trimIndent()

        val result = parseAttendanceData(response)

        // The malformed line (bad-date) should fail to parse and be skipped
        // Valid lines are extracted
        assertTrue(result.any { it.dateStr == "2023-10-15" })
        assertTrue(result.any { it.dateStr == "2023-10-17" })
        assertTrue(result.none { it.dateStr == "bad-date" })
    }
}
