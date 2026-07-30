package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.sse.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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

    /**
     * Erreur émise en cours de stream. Ne transporte qu'un code machine
     * optionnel (ex. AI_RATE_LIMITED) : le libellé affiché est toujours choisi
     * localement, jamais le texte du serveur.
     */
    data class Error(val code: String?) : AiStreamEvent()
}

/**
 * Sous-ensemble de l'API assistant consommé par le chat — extrait en interface
 * pour rendre ChatViewModel testable sans client HTTP.
 */
interface AiChatApi {
    suspend fun getConversationDetails(id: String): AgentConversation
    suspend fun searchReferences(query: String? = null): List<AssistantReference>
    suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        mode: AiMode = AiMode.CONCISE,
        references: List<AiChatReference> = emptyList(),
        onConversationIdReceived: (String) -> Unit
    ): Flow<AiStreamEvent>
}

class AiApiService(
    private val client: HttpClient,
    private val baseUrl: String
) : AiChatApi {
    suspend fun getConversations(page: Int = 1, date: String? = null, title: String? = null): PaginatedConversations {
        return client.get("$baseUrl/v1/assistant/conversations") {
            parameter("page", page)
            if (!date.isNullOrBlank()) parameter("filter.date", date)
            if (!title.isNullOrBlank()) parameter("filter.title", title)
        }.body()
    }

    override suspend fun getConversationDetails(id: String): AgentConversation {
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
    override suspend fun searchReferences(query: String?): List<AssistantReference> {
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

    override suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        mode: AiMode,
        references: List<AiChatReference>,
        onConversationIdReceived: (String) -> Unit
    ): Flow<AiStreamEvent> = flow {
        val url = if (conversationId != null) {
            "$baseUrl/v1/assistant/chat/$conversationId"
        } else {
            "$baseUrl/v1/assistant/chat"
        }

        client.sse(
            urlString = url,
            request = {
                method = HttpMethod.Post
                // Le timeout global (60 s) tuerait un stream de génération long.
                // Le timeout de socket, lui, reste FINI : il borne l'attente
                // entre deux jetons. Sans lui, une perte de réseau en cours de
                // réponse (sans coupure TCP franche) laisserait le chat sur
                // « l'assistant écrit… » indéfiniment, sans bouton d'annulation.
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    socketTimeoutMillis = 120_000
                }
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
                AiStreamEventParser.parse(event.event, event.data)?.let { emit(it) }
            }
        }
    }
}
