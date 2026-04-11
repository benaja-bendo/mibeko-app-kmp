package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class AiChatRequest(
    val message: String,
    val stream: Boolean = false
)

@Serializable
data class AiChatResponse(
    val conversation_id: String,
    val reply: String
)

@Serializable
data class AgentConversation(
    val id: String,
    val user_id: String,
    val title: String,
    val summary: String? = null,
    val created_at: String,
    val updated_at: String,
    val messages: List<AgentConversationMessage>? = null
)

@Serializable
data class AgentConversationMessage(
    val id: String,
    val conversation_id: String,
    val role: String,
    val content: String,
    val created_at: String
)

@Serializable
data class PaginatedConversations(
    val data: List<AgentConversation>,
    val current_page: Int,
    val last_page: Int
)

class AiApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun getConversations(page: Int = 1): PaginatedConversations {
        return client.get("$baseUrl/v1/assistant/conversations") {
            parameter("page", page)
        }.body()
    }

    suspend fun getConversationDetails(id: String): AgentConversation {
        return client.get("$baseUrl/v1/assistant/conversations/$id").body()
    }

    suspend fun sendMessage(message: String, conversationId: String? = null): AiChatResponse {
        val url = if (conversationId != null) {
            "$baseUrl/v1/assistant/chat/$conversationId"
        } else {
            "$baseUrl/v1/assistant/chat"
        }
        
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(AiChatRequest(message = message, stream = false))
        }.body()
    }
}
