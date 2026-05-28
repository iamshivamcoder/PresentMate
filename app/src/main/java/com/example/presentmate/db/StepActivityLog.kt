package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a single step-activity detection event (stair burst or periodic sync snapshot).
 *
 * [detectedAt]    Epoch millis when the burst/sync was recorded
 * [stepCount]     Total steps counted in this sync window
 * [windowMinutes] Duration of the window in minutes (e.g. 30 for periodic sync)
 * [type]          "STAIR_BURST" | "PERIODIC_SYNC" | "MANUAL_SYNC"
 * [window]        "MORNING" | "EVENING" | "BACKGROUND"
 * [triggered]     Whether a notification was sent for this event
 */
@Entity(tableName = "step_activity_logs")
data class StepActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val detectedAt: Long = System.currentTimeMillis(),
    val stepCount: Int = 0,
    val windowMinutes: Int = 30,
    val type: String = "PERIODIC_SYNC",   // STAIR_BURST | PERIODIC_SYNC | MANUAL_SYNC
    val window: String = "BACKGROUND",    // MORNING | EVENING | BACKGROUND
    val triggered: Boolean = false        // did this event send a notification?
)
