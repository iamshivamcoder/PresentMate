package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "developer_ideas")
data class DeveloperIdea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val title: String,
    val description: String,
    val category: String, // "Idea", "Bug", "Feature", "Chore"
    val priority: String, // "Low", "Medium", "High"
    val status: String,   // "Todo", "Done"
    val createdAt: Long = System.currentTimeMillis()
)
