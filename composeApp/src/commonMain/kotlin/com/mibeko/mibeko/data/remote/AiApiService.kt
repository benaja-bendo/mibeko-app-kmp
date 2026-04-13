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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class AiChatRequest(
    val message: String,
    val stream: Boolean = false
)

@Serializable
data class ArticleSource(
    val id: String,
    val number: String,
    val content: String,
    val document_id: String,
    val document_title: String,
    val document_type: String,
    val node_title: String,
    val breadcrumb: String,
    val score: Double = 0.0
)

@Serializable
data class AiChatResponse(
    val conversation_id: String,
    val reply: String,
    val sources: List<ArticleSource>? = null
)

@Serializable
data class MessageMeta(
    val sources: List<ArticleSource>? = null
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
    val created_at: String,
    val meta: kotlinx.serialization.json.JsonElement? = null
) {
    fun getSources(): List<ArticleSource>? {
        if (meta == null || meta !is kotlinx.serialization.json.JsonObject) return null
        val sourcesElement = meta["sources"] ?: return null
        if (sourcesElement !is kotlinx.serialization.json.JsonArray) return null
        return try {
            val lenientJson = Json { ignoreUnknownKeys = true }
            lenientJson.decodeFromJsonElement<List<ArticleSource>>(sourcesElement)
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
data class PaginatedConversations(
    val data: List<AgentConversation>,
    val current_page: Int,
    val last_page: Int
)

sealed class AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent()
    data class Sources(val sources: List<ArticleSource>) : AiStreamEvent()
}

class AiApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun getConversations(page: Int = 1, date: String? = null, title: String? = null): PaginatedConversations {
        return client.get("$baseUrl/v1/assistant/conversations") {
            parameter("page", page)
            if (!date.isNullOrBlank()) parameter("filter.date", date)
            if (!title.isNullOrBlank()) parameter("filter.title", title)
        }.body()
    }

    suspend fun getConversationDetails(id: String): AgentConversation {
        return client.get("$baseUrl/v1/assistant/conversations/$id").body()
    }

    suspend fun updateConversationTitle(id: String, title: String): AgentConversation {
        return client.put("$baseUrl/v1/assistant/conversations/$id") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("title" to title))
        }.body()
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
    ): Flow<AiStreamEvent> = flow {
        val url = if (conversationId != null) {
            "$baseUrl/v1/assistant/chat/$conversationId"
        } else {
            "$baseUrl/v1/assistant/chat"
        }
        
        val lenientJson = Json { ignoreUnknownKeys = true }

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
                val type = event.event
                if (data != null) {
                    if (data == "[DONE]") {
                        return@collect
                    }
                    if (type == "sources") {
                        try {
                            val sources = lenientJson.decodeFromString<List<ArticleSource>>(data)
                            emit(AiStreamEvent.Sources(sources))
                        } catch (e: Exception) {
                            // ignore malformed sources
                        }
                    } else {
                        try {
                            val json = lenientJson.decodeFromString<JsonObject>(data)
                            if (json["type"]?.jsonPrimitive?.content == "text_delta") {
                                val delta = json["delta"]?.jsonPrimitive?.content
                                if (delta != null) {
                                    emit(AiStreamEvent.Delta(delta))
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
}
