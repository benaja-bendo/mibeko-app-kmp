package com.mibeko.mibeko.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.AgentConversationMessage
import com.mibeko.mibeko.data.remote.AiChatApi
import com.mibeko.mibeko.data.remote.AiChatReference
import com.mibeko.mibeko.data.remote.AiMode
import com.mibeko.mibeko.data.remote.AiStreamEvent
import com.mibeko.mibeko.data.remote.AssistantReference
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.RemoteEntitlements
import com.mibeko.mibeko.getCurrentTimeMillis
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.UiResult
import com.mibeko.mibeko.util.recordException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Nature d'un échec d'envoi, dérivée du transport — jamais du texte serveur.
 * QUOTA ne mentionne aucun abonnement : il n'existe pas de parcours d'achat
 * dans l'app (exigence Apple 3.1.1).
 */
enum class ChatErrorKind { QUOTA, NETWORK, GENERIC }

data class ChatInlineError(
    val kind: ChatErrorKind,
    val retryAfterSeconds: Int? = null,
    val scope: String? = null,
    val canRetry: Boolean = false
)

/**
 * Classification pure d'un échec de transport (testable sans Ktor) :
 * 429 → quota, aucune réponse reçue → réseau, autre statut → générique.
 * `scope` (`minute`/`day`/`month`, mibeko-dashboard#62) vient du corps JSON
 * du 429, lu séparément par l'appelant — voir [parseRateLimitScope].
 */
fun classifyChatFailure(statusCode: Int?, retryAfterHeader: String?, scope: String? = null): ChatInlineError {
    val kind = when (statusCode) {
        429 -> ChatErrorKind.QUOTA
        null -> ChatErrorKind.NETWORK
        else -> ChatErrorKind.GENERIC
    }
    return ChatInlineError(kind, retryAfterHeader?.toIntOrNull(), scope)
}

/**
 * Résumé du quota affiché AVANT épuisement — mibeko-app-kmp#29. Pur et
 * testable sans Ktor, comme [classifyChatFailure]. Un solde de crédits
 * n'est qu'une information : jamais un lien ni un parcours d'achat.
 */
fun assistantQuotaSummary(entitlements: RemoteEntitlements): String {
    val quota = entitlements.quotas.assistant
    val remaining = (quota.limit - quota.used).coerceAtLeast(0)
    val base = if (remaining > 0) {
        "$remaining question${if (remaining > 1) "s" else ""} restante${if (remaining > 1) "s" else ""}"
    } else {
        "Quota atteint"
    }
    val credits = entitlements.credits
    return if (credits != null && credits > 0) {
        "$base · $credits crédit${if (credits > 1) "s" else ""}"
    } else {
        base
    }
}

/**
 * Extrait le `scope` du corps JSON d'un 429 `AI_RATE_LIMITED`. Pur et
 * testable sans Ktor : l'appelant lit le corps de la réponse en amont.
 * Retourne null si absent ou illisible — le libellé générique reste le repli.
 */
fun parseRateLimitScope(body: String?): String? {
    if (body == null) return null
    return runCatching {
        Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(body)["scope"]?.jsonPrimitive?.content
    }.getOrNull()
}

/**
 * Libellé d'un 429 IA, dérivé du `scope` — jamais du texte serveur (même
 * choix que pour [AiStreamEvent.Error], cf. AiApiService.kt). `day` et
 * `month` n'utilisent PAS `retryAfterSeconds` en minutes : sur un plafond
 * mensuel, Retry-After peut valoir ~30 jours et produirait un texte
 * illisible du type « Réessayez dans 43200 min. ».
 */
fun quotaErrorMessage(error: ChatInlineError): String = when (error.scope) {
    "day" -> "Plafond journalier de requêtes IA atteint. Réessayez demain."
    "month" -> {
        val days = error.retryAfterSeconds?.let { (it + 86_399) / 86_400 }
        if (days != null && days > 0) {
            "Plafond mensuel de requêtes IA atteint. Réessayez dans $days jour${if (days > 1) "s" else ""}."
        } else {
            "Plafond mensuel de requêtes IA atteint. Réessayez le mois prochain."
        }
    }
    else -> {
        val minutes = error.retryAfterSeconds?.let { (it + 59) / 60 }
        if (minutes != null && minutes > 0) {
            "Limite temporaire de requêtes atteinte. Réessayez dans $minutes min."
        } else {
            "Limite temporaire de requêtes atteinte. Réessayez dans quelques minutes."
        }
    }
}

sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    object Error : ChatState()
    data class Content(
        val messages: List<AgentConversationMessage>,
        val conversationId: String?,
        val isTyping: Boolean = false,
        val inlineError: ChatInlineError? = null
    ) : ChatState()
}

/** État du sélecteur de références « @ » (documents épinglés, max 5). */
data class ReferencePickerState(
    val isVisible: Boolean = false,
    val query: String = "",
    val suggestions: List<AssistantReference> = emptyList(),
    val isLoading: Boolean = false
)

class ChatViewModel(
    private val aiApiService: AiChatApi,
    private val analytics: MibekoAnalytics? = null,
    private val authApiService: AuthApiService? = null
) : ViewModel() {

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // mibeko-app-kmp#29 : quota affiché AVANT épuisement, jamais deviné d'un
    // rôle local — seule source : GET /v1/me/entitlements.
    private val _entitlements = MutableStateFlow<UiResult<RemoteEntitlements>>(UiResult.Loading)
    val entitlements: StateFlow<UiResult<RemoteEntitlements>> = _entitlements.asStateFlow()

    init {
        loadEntitlements()
    }

    fun loadEntitlements() {
        val service = authApiService ?: return
        viewModelScope.launch {
            _entitlements.value = UiResult.Loading
            try {
                val data = service.getEntitlements().data
                _entitlements.value = if (data != null) {
                    UiResult.Success(data)
                } else {
                    UiResult.Error(offline = false, retry = ::loadEntitlements)
                }
            } catch (e: Exception) {
                recordException(e, context = "ChatViewModel.loadEntitlements")
                _entitlements.value = UiResult.Error(offline = true, retry = ::loadEntitlements)
            }
        }
    }

    private val _mode = MutableStateFlow(AiMode.CONCISE)
    val mode: StateFlow<AiMode> = _mode.asStateFlow()

    private val _pinnedReferences = MutableStateFlow<List<AssistantReference>>(emptyList())
    val pinnedReferences: StateFlow<List<AssistantReference>> = _pinnedReferences.asStateFlow()

    private val _referencePicker = MutableStateFlow(ReferencePickerState())
    val referencePicker: StateFlow<ReferencePickerState> = _referencePicker.asStateFlow()

    private var currentConversationId: String? = null
    private val currentMessages = mutableListOf<AgentConversationMessage>()
    private var streamJob: Job? = null
    private var referenceSearchJob: Job? = null
    private var lastFailedPrompt: String? = null

    fun setMode(mode: AiMode) {
        _mode.value = mode
    }

    fun openReferencePicker() {
        _referencePicker.value = ReferencePickerState(isVisible = true, isLoading = true)
        searchReferences("")
    }

    fun closeReferencePicker() {
        referenceSearchJob?.cancel()
        _referencePicker.value = ReferencePickerState()
    }

    fun searchReferences(query: String) {
        _referencePicker.value = _referencePicker.value.copy(query = query, isLoading = true)
        referenceSearchJob?.cancel()
        referenceSearchJob = viewModelScope.launch {
            delay(250) // debounce de frappe
            try {
                val results = aiApiService.searchReferences(query.ifBlank { null })
                _referencePicker.value = _referencePicker.value.copy(
                    suggestions = results,
                    isLoading = false
                )
            } catch (e: Exception) {
                _referencePicker.value = _referencePicker.value.copy(
                    suggestions = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun pinReference(reference: AssistantReference) {
        val current = _pinnedReferences.value
        if (current.size >= 5 || current.any { it.id == reference.id }) return
        _pinnedReferences.value = current + reference
    }

    fun unpinReference(referenceId: String) {
        _pinnedReferences.value = _pinnedReferences.value.filterNot { it.id == referenceId }
    }

    fun initChat(conversationId: String?, initialPrompt: String?) {
        // Prevent re-initialization when navigating back from another screen (e.g. DocumentDetail)
        if (_chatState.value is ChatState.Content || _chatState.value is ChatState.Loading) {
            // Exception: si l'ID de conversation a changé (réutilisation du ViewModel avec SingleTop)
            if (conversationId != null && conversationId != currentConversationId) {
                // On laisse passer pour charger la nouvelle conversation
            } else {
                return
            }
        }

        if (conversationId != null) {
            loadConversation(conversationId)
        } else if (initialPrompt != null && initialPrompt.isNotBlank()) {
            sendMessage(initialPrompt)
        } else {
            _chatState.value = ChatState.Content(emptyList(), null)
        }
    }

    private fun loadConversation(id: String) {
        viewModelScope.launch {
            _chatState.value = ChatState.Loading
            try {
                val conversation = aiApiService.getConversationDetails(id)
                currentConversationId = conversation.id
                currentMessages.clear()
                if (conversation.messages != null) {
                    currentMessages.addAll(conversation.messages)
                }
                _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId)
            } catch (e: Exception) {
                _chatState.value = ChatState.Error
            }
        }
    }

    /** Renvoie le dernier message dont l'envoi a échoué (bouton « Réessayer »). */
    fun retryLastFailedMessage() {
        val prompt = lastFailedPrompt ?: return
        lastFailedPrompt = null
        // La bulle utilisateur du message échoué est retirée : sendMessage la
        // recrée, sinon elle apparaîtrait en double.
        val lastUserIndex = currentMessages.indexOfLast { it.role == "user" }
        if (lastUserIndex != -1 && currentMessages[lastUserIndex].content == prompt) {
            currentMessages.removeAt(lastUserIndex)
        }
        sendMessage(prompt)
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        lastFailedPrompt = null

        val userMessage = AgentConversationMessage(
            id = "temp_${getCurrentTimeMillis()}",
            conversation_id = currentConversationId ?: "",
            role = "user",
            content = message,
            created_at = ""
        )
        currentMessages.add(userMessage)
        
        // Add an empty AI message that will show "thinking..." initially
        val aiMessageId = "temp_ai_${getCurrentTimeMillis()}"
        val initialAiMessage = AgentConversationMessage(
            id = aiMessageId,
            conversation_id = currentConversationId ?: "",
            role = "assistant",
            content = "",
            created_at = "",
            meta = null
        )
        currentMessages.add(initialAiMessage)
        
        _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId, isTyping = true)

        analytics?.logEvent(
            AnalyticsEvents.CHAT_MESSAGE_SENT,
            mapOf(
                "mode" to _mode.value.apiValue,
                "reference_count" to _pinnedReferences.value.size
            )
        )

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                var streamContent = ""
                var streamMeta: JsonObject? = null
                var lastUpdateTime = 0L
                
                aiApiService.sendMessageStream(
                    message = message,
                    conversationId = currentConversationId,
                    mode = _mode.value,
                    references = _pinnedReferences.value.map { AiChatReference(id = it.id) },
                    onConversationIdReceived = { id ->
                        currentConversationId = id
                    }
                ).collect { event ->
                    when (event) {
                        is AiStreamEvent.Status -> {
                            // On peut afficher le status dans le contenu temporairement,
                            // ou dans une variable de state si l'UI le gère
                            if (streamContent.isEmpty()) {
                                streamContent = "🔄 ${event.message}..."
                            }
                        }
                        is AiStreamEvent.Sources -> {
                            val sourcesJsonArray = Json.encodeToJsonElement(event.sources) as JsonArray
                            streamMeta = JsonObject(mapOf("sources" to sourcesJsonArray))
                        }
                        is AiStreamEvent.Delta -> {
                            // Si on avait un message de status, on l'efface au premier vrai mot
                            if (streamContent.startsWith("🔄 ")) {
                                streamContent = ""
                            }
                            streamContent += event.text
                        }
                        is AiStreamEvent.Error -> {
                            // Libellé toujours local : le texte serveur peut
                            // contenir des détails techniques ou commerciaux.
                            val label = when (event.code) {
                                "AI_RATE_LIMITED" ->
                                    "Limite temporaire de requêtes atteinte. Réessayez dans quelques minutes."
                                else ->
                                    "L'assistant a rencontré une erreur. Veuillez réessayer."
                            }
                            streamContent = if (streamContent.startsWith("🔄 ") || streamContent.isEmpty()) {
                                label
                            } else {
                                "$streamContent\n\n[$label]"
                            }
                        }
                    }
                    
                    // Update the last message
                    val lastIndex = currentMessages.indexOfLast { it.id == aiMessageId }
                    if (lastIndex != -1) {
                        currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                            content = streamContent,
                            conversation_id = currentConversationId ?: "",
                            meta = streamMeta
                        )
                        
                        // Throttle updates to avoid flickering and excessive recompositions
                        val currentTime = getCurrentTimeMillis()
                        if (currentTime - lastUpdateTime > 200 || event is AiStreamEvent.Sources) {
                            _chatState.value = ChatState.Content(
                                currentMessages.toList(), 
                                currentConversationId, 
                                isTyping = true
                            )
                            lastUpdateTime = currentTime
                        }
                    }
                }
                
                // Final update after streaming is done
                _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId, isTyping = false)
            } catch (e: CancellationException) {
                // Un nouvel envoi annule le stream en cours : laisser le job
                // remplaçant piloter l'état au lieu de l'écraser ici.
                throw e
            } catch (e: Exception) {
                _chatState.value = buildFailureState(e, message, aiMessageId)
            }
        }
    }

    /**
     * Traduit un échec de transport en état d'erreur affichable. Le status et
     * le Retry-After sont lus sur la réponse portée par l'exception Ktor
     * (SSEClientException pour le stream, ResponseException sinon) ; sans
     * réponse, c'est une panne réseau. Le corps est lu en best-effort pour le
     * `scope` du 429 (mibeko-dashboard#62) : une lecture qui échoue ne doit
     * jamais empêcher l'affichage du message de repli.
     */
    private suspend fun buildFailureState(
        e: Exception,
        prompt: String,
        aiMessageId: String
    ): ChatState.Content {
        val response = when (e) {
            is SSEClientException -> e.response
            is ResponseException -> e.response
            else -> null
        }
        val scope = response?.let { runCatching { it.bodyAsText() }.getOrNull() }?.let(::parseRateLimitScope)
        val baseError = classifyChatFailure(
            statusCode = response?.status?.value,
            retryAfterHeader = response?.headers?.get("Retry-After"),
            scope = scope
        )

        // Rien n'a été reçu : on retire la bulle IA vide et on propose de
        // réessayer. Si un contenu partiel existe, il reste affiché tel quel.
        val emptyPlaceholderIndex = currentMessages.indexOfLast {
            it.id == aiMessageId && it.content.isEmpty()
        }
        val canRetry = emptyPlaceholderIndex != -1
        if (canRetry) {
            currentMessages.removeAt(emptyPlaceholderIndex)
            lastFailedPrompt = prompt
        }

        analytics?.logEvent(
            AnalyticsEvents.CHAT_ERROR,
            mapOf("kind" to baseError.kind.name.lowercase())
        )

        return ChatState.Content(
            messages = currentMessages.toList(),
            conversationId = currentConversationId,
            isTyping = false,
            inlineError = baseError.copy(canRetry = canRetry)
        )
    }
}
