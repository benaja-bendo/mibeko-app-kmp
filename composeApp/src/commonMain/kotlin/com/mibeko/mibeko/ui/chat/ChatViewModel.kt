package com.mibeko.mibeko.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.AgentConversationMessage
import com.mibeko.mibeko.data.remote.AiApiService
import com.mibeko.mibeko.data.remote.AiChatReference
import com.mibeko.mibeko.data.remote.AiMode
import com.mibeko.mibeko.data.remote.AiStreamEvent
import com.mibeko.mibeko.data.remote.AssistantReference
import com.mibeko.mibeko.getCurrentTimeMillis
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

sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    object Error : ChatState()
    data class Content(
        val messages: List<AgentConversationMessage>,
        val conversationId: String?,
        val isTyping: Boolean = false
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
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

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

    fun sendMessage(message: String) {
        if (message.isBlank()) return

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
                            streamContent = if (streamContent.startsWith("🔄 ") || streamContent.isEmpty()) {
                                event.message
                            } else {
                                "$streamContent\n\n[Erreur: ${event.message}]"
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
            } catch (e: Exception) {
                // If it fails completely and we have no content, show an error message
                val lastIndex = currentMessages.indexOfLast { it.id == aiMessageId }
                if (lastIndex != -1 && currentMessages[lastIndex].content.isEmpty()) {
                    currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                        content = "Une erreur est survenue lors de la génération de la réponse."
                    )
                }
                _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId, isTyping = false)
            }
        }
    }
}
