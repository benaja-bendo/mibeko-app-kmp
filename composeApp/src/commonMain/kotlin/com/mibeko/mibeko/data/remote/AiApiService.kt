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
    val stream: Boolean = false,
    val mode: String? = null,
    val references: List<AiChatReference>? = null
)

/** Document épinglé (« @ ») restreignant le périmètre de recherche de l'IA. */
@Serializable
data class AiChatReference(
    val id: String,
    val type: String = "document"
)

/** Document proposé par l'autocomplétion du sélecteur de références. */
@Serializable
data class AssistantReference(
    val id: String,
    val title: String,
    val type_code: String? = null,
    val type_name: String? = null
)

@Serializable
data class AssistantReferencesResponse(
    val data: List<AssistantReference> = emptyList()
)

/** Modes de réponse supportés par l'assistant (alignés sur le web pro). */
enum class AiMode(val apiValue: String, val label: String) {
    CONCISE("concise", "Concis"),
    ANALYSIS("analysis", "Analyse")
}

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
    // La liste de l'historique est volontairement légère côté serveur :
    // seuls id/title/created_at/updated_at sont garantis.
    val user_id: String? = null,
    val title: String = "Conversation",
    val summary: String? = null,
    val created_at: String = "",
    val updated_at: String = "",
    val messages: List<AgentConversationMessage>? = null
)

@Serializable
data class AgentConversationMessage(
    val id: String,
    // Absent du détail d'une conversation (le serveur n'envoie que
    // id/role/content/meta/created_at par message).
    val conversation_id: String? = null,
    val role: String,
    val content: String,
    val created_at: String = "",
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
    data class Status(val message: String) : AiStreamEvent()
    data class Error(val message: String) : AiStreamEvent()
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

    /** Autocomplétion des documents épinglables dans le sélecteur « @ ». */
    suspend fun searchReferences(query: String? = null): List<AssistantReference> {
        return client.get("$baseUrl/v1/assistant/references") {
            if (!query.isNullOrBlank()) parameter("q", query)
        }.body<AssistantReferencesResponse>().data
    }

    suspend fun sendMessage(
        message: String,
        conversationId: String? = null,
        mode: AiMode = AiMode.CONCISE,
        references: List<AiChatReference> = emptyList()
    ): AiChatResponse {
        val url = if (conversationId != null) {
            "$baseUrl/v1/assistant/chat/$conversationId"
        } else {
            "$baseUrl/v1/assistant/chat"
        }

        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(
                AiChatRequest(
                    message = message,
                    stream = false,
                    mode = mode.apiValue,
                    references = references.ifEmpty { null }
                )
            )
        }.body()
    }

    suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        mode: AiMode = AiMode.CONCISE,
        references: List<AiChatReference> = emptyList(),
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
                setBody(
                    AiChatRequest(
                        message = message,
                        stream = true,
                        mode = mode.apiValue,
                        references = references.ifEmpty { null }
                    )
                )
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
                    if (type == "status") {
                        try {
                            val json = lenientJson.decodeFromString<JsonObject>(data)
                            val statusMsg = json["message"]?.jsonPrimitive?.content
                            if (statusMsg != null) {
                                emit(AiStreamEvent.Status(statusMsg))
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    } else if (type == "sources") {
                        try {
                            val sources = lenientJson.decodeFromString<List<ArticleSource>>(data)
                            emit(AiStreamEvent.Sources(sources))
                        } catch (e: Exception) {
                            // ignore malformed sources
                        }
                    } else if (type == "error") {
                        try {
                            val json = lenientJson.decodeFromString<JsonObject>(data)
                            val errorMsg = json["message"]?.jsonPrimitive?.content
                            if (errorMsg != null) {
                                emit(AiStreamEvent.Error(errorMsg))
                            }
                        } catch (e: Exception) {
                            // ignore
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
