package com.mibeko.mibeko.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Instant as DateTimeInstant
import com.mibeko.mibeko.data.repository.NotificationRepository
import com.mibeko.mibeko.getCurrentTimeMillis

/**
 * Modèle de données pour une notification dans l'UI.
 */
data class NotificationUiModel(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // 'legal_update', 'dossier_alert', 'info'
    val timestamp: Long,
    val isRead: Boolean,
    val data: Map<String, String>? = null
) {
    /**
     * Slug du texte visé par la notification, quand elle en désigne un.
     * Les alertes de synthèse (« N nouveaux textes ») n'en portent pas.
     */
    fun targetSlug(): String? = data?.get("slug")?.takeIf { it.isNotBlank() }
}

/**
 * État de l'UI pour l'écran des notifications.
 */
data class NotificationsUiState(
    val notifications: List<NotificationUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    /**
     * Charge les notifications depuis le repository.
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getNotifications().collect { remotes ->
                val models = remotes.map { remote ->
                    NotificationUiModel(
                        id = remote.id,
                        title = remote.title,
                        message = remote.message,
                        type = remote.type,
                        timestamp = try {
                                DateTimeInstant.parse(remote.created_at).toEpochMilliseconds()
                            } catch (e: Exception) {
                                getCurrentTimeMillis()
                            },
                        isRead = remote.read_at != null,
                        data = remote.data
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    notifications = models,
                    isLoading = false
                )
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
            val updatedList = _uiState.value.notifications.map {
                if (it.id == id) it.copy(isRead = true) else it
            }
            _uiState.value = _uiState.value.copy(notifications = updatedList)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
            val updatedList = _uiState.value.notifications.map { it.copy(isRead = true) }
            _uiState.value = _uiState.value.copy(notifications = updatedList)
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
            val updatedList = _uiState.value.notifications.filter { it.id != id }
            _uiState.value = _uiState.value.copy(notifications = updatedList)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
