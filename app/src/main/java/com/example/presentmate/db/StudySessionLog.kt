package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_session_logs")
data class StudySessionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val calendarEventId: Long,          // CalendarContract event ID
    val eventTitle: String,             // Raw title from calendar
    val subject: String?,               // Extracted subject (e.g., "Modern History")
    val topic: String?,                 // Extracted topic (e.g., "Anglo Carnatic Wars")
    val scheduledStartTime: Long,       // Event start millis
    val scheduledEndTime: Long,         // Event end millis
    val status: String = "PENDING",     // PENDING | COMPLETED | PARTIAL | SKIPPED
    val actualDurationMinutes: Int? = null, // For PARTIAL: how many minutes studied
    val recallNote: String? = null,     // Optional one-sentence recall
    val loggedAt: Long? = null          // When the user responded
)
