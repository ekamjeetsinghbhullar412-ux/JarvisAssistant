package com.jarvis.assistant.ai

import retrofit2.http.Body
import retrofit2.http.POST

data class ChatMessageDto(val role: String, val content: String)
data class ChatRequest(val messages: List<ChatMessageDto>, val assistantName: String = "Jarvis")
data class ChatResponse(val reply: String)

/**
 * Talks ONLY to your own backend (see /backend in the project root), never directly to
 * the AI provider. This keeps your real API key off the device entirely.
 */
interface ApiService {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
