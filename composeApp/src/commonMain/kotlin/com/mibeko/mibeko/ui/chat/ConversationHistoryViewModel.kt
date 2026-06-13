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

    private var currentFilterDate: String? = null
    private var currentFilterTitle: String? = null

    init {
        loadHistory()
    }

    fun loadHistory(date: String? = currentFilterDate, title: String? = currentFilterTitle) {
        currentFilterDate = date
        currentFilterTitle = title
        viewModelScope.launch {
            // Spinner plein écran uniquement au premier chargement : un
            // rafraîchissement garde la liste affichée, sans saut visuel.
            if (_historyState.value !is HistoryState.Content) {
                _historyState.value = HistoryState.Loading
            }
            try {
                val response = aiApiService.getConversations(1, date, title)
                _historyState.value = HistoryState.Content(response.data)
            } catch (e: Exception) {
                if (_historyState.value !is HistoryState.Content) {
                    _historyState.value = HistoryState.Error
                }
            }
        }
    }

    fun updateConversationTitle(id: String, newTitle: String) {
        // Mise à jour optimiste : le titre change immédiatement, le serveur
        // est notifié en arrière-plan ; en cas d'échec on resynchronise.
        val current = _historyState.value
        if (current is HistoryState.Content) {
            _historyState.value = HistoryState.Content(
                current.conversations.map { if (it.id == id) it.copy(title = newTitle) else it }
            )
        }
        viewModelScope.launch {
            try {
                aiApiService.updateConversationTitle(id, newTitle)
            } catch (e: Exception) {
                loadHistory()
            }
        }
    }

    fun deleteConversation(id: String) {
        // Suppression optimiste : la ligne disparaît tout de suite.
        val current = _historyState.value
        if (current is HistoryState.Content) {
            _historyState.value = HistoryState.Content(
                current.conversations.filterNot { it.id == id }
            )
        }
        viewModelScope.launch {
            try {
                aiApiService.deleteConversation(id)
            } catch (e: Exception) {
                loadHistory()
            }
        }
    }
}
