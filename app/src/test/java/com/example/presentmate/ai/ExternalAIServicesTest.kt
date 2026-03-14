package com.example.presentmate.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAIServicesTest {

    // Since parseAttendanceData is private but essential to the functionality,
    // we would ideally test the public method with a MockWebServer to return a fixed JSON.
    // However, to quickly test the parsing logic we will use reflection for this unit test.
    
    @Test
    fun `parseAttendanceData extracts correctly formatted data`() {
        // Use reflection to access the private top-level function in ExternalAIServices.kt
        val method = Class.forName("com.example.presentmate.ai.ExternalAIServicesKt")
            .getDeclaredMethod("parseAttendanceData", String::class.java)
        method.isAccessible = true
        
        val aiResponseMock = """
            Here is your requested data:
            [ATTENDANCE_DATA]
            DATE: 2023-10-15, IN: 09:00, OUT: 17:00
            DATE: 2023-10-16, IN: 09:15, OUT: 17:30
            [/ATTENDANCE_DATA]
            Have a nice day!
        """.trimIndent()
        
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(null, aiResponseMock) as List<ParsedAttendance>
        
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
        val method = Class.forName("com.example.presentmate.ai.ExternalAIServicesKt")
            .getDeclaredMethod("parseAttendanceData", String::class.java)
        method.isAccessible = true
        
        // Missing the [ATTENDANCE_DATA] tags entirely
        val aiResponseMock = """
            DATE: 2023-10-15, IN: 09:00, OUT: 17:00
        """.trimIndent()
        
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(null, aiResponseMock) as List<ParsedAttendance>
        
        assertTrue(result.isEmpty())
    }
}
