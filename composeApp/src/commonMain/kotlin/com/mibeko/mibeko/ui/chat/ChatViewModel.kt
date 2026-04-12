package com.mibeko.mibeko.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.AgentConversationMessage
import com.mibeko.mibeko.data.remote.AiApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mibeko.mibeko.getCurrentTimeMillis

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

class ChatViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private var currentConversationId: String? = null
    private val currentMessages = mutableListOf<AgentConversationMessage>()

    fun initChat(conversationId: String?, initialPrompt: String?) {
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
            created_at = ""
        )
        currentMessages.add(initialAiMessage)
        
        _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId, isTyping = true)

        viewModelScope.launch {
            try {
                var streamContent = ""
                aiApiService.sendMessageStream(
                    message = message,
                    conversationId = currentConversationId,
                    onConversationIdReceived = { id ->
                        currentConversationId = id
                    }
                ).collect { delta ->
                    streamContent += delta
                    
                    // Update the last message
                    val lastIndex = currentMessages.indexOfLast { it.id == aiMessageId }
                    if (lastIndex != -1) {
                        currentMessages[lastIndex] = currentMessages[lastIndex].copy(
                            content = streamContent,
                            conversation_id = currentConversationId ?: ""
                        )
                        _chatState.value = ChatState.Content(
                            currentMessages.toList(), 
                            currentConversationId, 
                            isTyping = false // As soon as we receive data, we are no longer "thinking"
                        )
                    }
                }
                
                // Done streaming
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
