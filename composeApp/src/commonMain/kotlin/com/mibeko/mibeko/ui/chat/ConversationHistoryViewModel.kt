package com.mibeko.mibeko.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.AgentConversation
import com.mibeko.mibeko.data.remote.AiApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HistoryState {
    object Loading : HistoryState()
    object Error : HistoryState()
    data class Content(val conversations: List<AgentConversation>) : HistoryState()
}

class ConversationHistoryViewModel(
    private val aiApiService: AiApiService
) : ViewModel() {

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = HistoryState.Loading
            try {
                val response = aiApiService.getConversations(1)
                _historyState.value = HistoryState.Content(response.data)
            } catch (e: Exception) {
                _historyState.value = HistoryState.Error
            }
        }
    }
}
