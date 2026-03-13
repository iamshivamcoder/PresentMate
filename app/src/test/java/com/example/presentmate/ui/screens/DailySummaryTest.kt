package com.example.presentmate.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryTest {

    @Test
    fun durationString_calculatesCorrectly() {
        val summary1 = DailySummary(date = 0, totalDurationMillis = 0L)
        assertEquals("0m", summary1.durationString)

        val summary2 = DailySummary(date = 0, totalDurationMillis = 45 * 60 * 1000L) // 45 minutes
        assertEquals("45m", summary2.durationString)

        val summary3 = DailySummary(date = 0, totalDurationMillis = 60 * 60 * 1000L) // 1 hour
        assertEquals("1h 0m", summary3.durationString)

        val summary4 = DailySummary(date = 0, totalDurationMillis = 90 * 60 * 1000L) // 1 hour 30 mins
        assertEquals("1h 30m", summary4.durationString)

        val summary5 = DailySummary(date = 0, totalDurationMillis = (2 * 60 * 60 * 1000L) + (5 * 60 * 1000L)) // 2h 5m
        assertEquals("2h 5m", summary5.durationString)
    }
}
