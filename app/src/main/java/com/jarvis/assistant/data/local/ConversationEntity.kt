package com.jarvis.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,       // "USER" | "ASSISTANT" | "SYSTEM"
    val text: String,
    val timestamp: Long
)
