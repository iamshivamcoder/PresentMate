package com.example.presentmate.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailySummaryTest {

    @Test
    fun durationString_calculatesCorrectly() {
        val today = LocalDate.now()
        val summary1 = DailySummary(date = today, totalDurationMillis = 0L, records = emptyList())
        assertEquals("0m", summary1.durationString)

        val summary2 = DailySummary(date = today, totalDurationMillis = 45 * 60 * 1000L, records = emptyList()) // 45 minutes
        assertEquals("45m", summary2.durationString)

        val summary3 = DailySummary(date = today, totalDurationMillis = 60 * 60 * 1000L, records = emptyList()) // 1 hour
        assertEquals("1h 0m", summary3.durationString)

        val summary4 = DailySummary(date = today, totalDurationMillis = 90 * 60 * 1000L, records = emptyList()) // 1 hour 30 mins
        assertEquals("1h 30m", summary4.durationString)

        val summary5 = DailySummary(date = today, totalDurationMillis = (2 * 60 * 60 * 1000L) + (5 * 60 * 1000L), records = emptyList()) // 2h 5m
        assertEquals("2h 5m", summary5.durationString)
    }
}
