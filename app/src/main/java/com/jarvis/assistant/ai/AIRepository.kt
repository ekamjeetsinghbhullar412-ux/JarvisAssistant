package com.jarvis.assistant.ai

import com.jarvis.assistant.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException

sealed class AIResult {
    data class Success(val reply: String) : AIResult()
    data class Failure(val userMessage: String) : AIResult()
}

private const val SYSTEM_PROMPT = """
You are Jarvis, a helpful, futuristic AI assistant embedded in an Android app.
Be concise, warm, and precise. If asked to do something the Android app cannot
actually perform (e.g. bypassing security, accessing data without permission),
say so plainly instead of pretending. Only reference information the user has
explicitly shared or asked about in this conversation.
"""

class AIRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun sendConversation(history: List<Message>): AIResult = withContext(Dispatchers.IO) {
        try {
            val dtoMessages = buildList {
                add(ChatMessageDto("system", SYSTEM_PROMPT.trim()))
                addAll(
                    history.takeLast(20).map {
                        ChatMessageDto(
                            role = if (it.role == Message.Role.USER) "user" else "assistant",
                            content = it.text
                        )
                    }
                )
            }
            val response = api.chat(ChatRequest(messages = dtoMessages))
            AIResult.Success(response.reply)
        } catch (e: SocketTimeoutException) {
            AIResult.Failure("The request timed out. Check your connection and try again.")
        } catch (e: IOException) {
            AIResult.Failure("I couldn't reach the server — are you online?")
        } catch (e: Exception) {
            AIResult.Failure("Something went wrong talking to the AI backend: ${e.message}")
        }
    }
}
