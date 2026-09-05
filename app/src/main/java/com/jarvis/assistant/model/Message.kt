package com.jarvis.assistant.model

/** A single turn in the conversation, used both for UI display and for building AI context. */
data class Message(
    val id: Long = System.currentTimeMillis(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}
