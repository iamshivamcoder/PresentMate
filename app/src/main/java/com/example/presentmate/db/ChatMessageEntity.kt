package com.example.presentmate.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val userId: String,
    val content: String,
    val isFromUser: Boolean,
    val imageUriString: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
