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
        val conversationId: String?
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
        _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId)

        viewModelScope.launch {
            try {
                val response = aiApiService.sendMessage(message, currentConversationId)
                currentConversationId = response.conversation_id
                
                val aiMessage = AgentConversationMessage(
                    id = "temp_ai_${getCurrentTimeMillis()}",
                    conversation_id = currentConversationId!!,
                    role = "assistant",
                    content = response.reply,
                    created_at = ""
                )
                currentMessages.add(aiMessage)
                _chatState.value = ChatState.Content(currentMessages.toList(), currentConversationId)
            } catch (e: Exception) {
                // Remove the temp message if failed or handle error
            }
        }
    }
}
