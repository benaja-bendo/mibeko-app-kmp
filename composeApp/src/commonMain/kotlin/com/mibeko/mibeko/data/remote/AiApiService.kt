package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    suspend fun deleteConversation(id: String) {
        client.delete("$baseUrl/v1/assistant/conversations/$id")
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

    suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        onConversationIdReceived: (String) -> Unit
    ): Flow<String> = flow {
        val url = if (conversationId != null) {
            "$baseUrl/v1/assistant/chat/$conversationId"
        } else {
            "$baseUrl/v1/assistant/chat"
        }

        client.sse(
            urlString = url,
            request = {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(AiChatRequest(message = message, stream = true))
                headers {
                    append(HttpHeaders.Accept, "text/event-stream")
                }
            }
        ) {
            val receivedId = call.response.headers["X-Conversation-Id"]
            if (receivedId != null) {
                onConversationIdReceived(receivedId)
            }

            incoming.collect { event ->
                val data = event.data
                if (data != null) {
                    if (data == "[DONE]") {
                        return@collect
                    }
                    try {
                        val json = Json.decodeFromString<JsonObject>(data)
                        if (json["type"]?.jsonPrimitive?.content == "text_delta") {
                            val delta = json["delta"]?.jsonPrimitive?.content
                            if (delta != null) {
                                emit(delta)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore malformed JSON or other events
                    }
                }
            }
        }
    }
}
