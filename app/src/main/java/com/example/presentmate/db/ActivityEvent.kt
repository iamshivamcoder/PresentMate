package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_events")
data class ActivityEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val metadataJson: String? = null,
    val isSynced: Boolean = false
)
