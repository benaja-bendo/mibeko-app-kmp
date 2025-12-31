package com.mibeko.mibeko.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.util.formatSize
import com.mibeko.mibeko.util.formatTimestampToDate
import com.mibeko.mibeko.util.getDatabaseSize
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Represents a document's download state in the settings screen.
 */
data class DocumentDownloadState(
    val id: String,
    val title: String,
    val typeCode: String,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
)

/**
 * UI State for the settings screen.
 */
data class SettingsUiState(
    val isOfflineModeEnabled: Boolean = false,
    val documents: List<DocumentDownloadState> = emptyList(),
    val isLoadingDocuments: Boolean = false,
    val currentTheme: UserPreferencesRepository.AppTheme = UserPreferencesRepository.AppTheme.SYSTEM,
    val isNotificationsEnabled: Boolean = false,
    val lastUpdateDate: String = "Jamais",
    val diskUsage: String = "0 B"
)

/**
 * ViewModel for the Settings screen.
 * Manages offline mode toggle, document download management, and application settings.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val legalRepository: LocalLegalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        refreshDiskUsage()
    }

    private fun loadInitialState() {
        // Load offline mode preference
        val offlineMode = userPreferencesRepository.isOfflineModeEnabled()
        val theme = userPreferencesRepository.getAppTheme()
        val notifications = userPreferencesRepository.isNotificationsEnabled()
        val lastSync = userPreferencesRepository.getLastSyncTimestamp()
        
        _uiState.value = _uiState.value.copy(
            isOfflineModeEnabled = offlineMode,
            currentTheme = theme,
            isNotificationsEnabled = notifications,
            lastUpdateDate = formatTimestampToDate(lastSync)
        )
        
        // Load available documents
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDocuments = true)
            legalRepository.getLawCodes().collect { lawCodes ->
                val downloadStates = lawCodes.map { code ->
                    DocumentDownloadState(
                        id = code.id,
                        title = code.title,
                        typeCode = code.icon, // icon stores type_code
                        isDownloaded = false, // TODO: Check is_downloaded field once added
                        isDownloading = false
                    )
                }
                _uiState.value = _uiState.value.copy(
                    documents = downloadStates,
                    isLoadingDocuments = false
                )
            }
        }
    }

    /**
     * Toggle offline-only mode.
     * When enabled, the app will not use network for searches.
     */
    fun setOfflineMode(enabled: Boolean) {
        userPreferencesRepository.setOfflineMode(enabled)
        _uiState.value = _uiState.value.copy(isOfflineModeEnabled = enabled)
    }

    /**
     * Update the application theme.
     */
    fun setTheme(theme: UserPreferencesRepository.AppTheme) {
        userPreferencesRepository.setAppTheme(theme)
        _uiState.value = _uiState.value.copy(currentTheme = theme)
    }

    /**
     * Toggle notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        userPreferencesRepository.setNotificationsEnabled(enabled)
        _uiState.value = _uiState.value.copy(isNotificationsEnabled = enabled)
    }

    /**
     * Refresh disk usage information.
     */
    fun refreshDiskUsage() {
        viewModelScope.launch {
            val size = getDatabaseSize()
            _uiState.value = _uiState.value.copy(diskUsage = formatSize(size))
        }
    }

    /**
     * Download a document for offline access.
     * This downloads the full structure and articles for the given document ID.
     */
    fun downloadDocument(documentId: String) {
        viewModelScope.launch {
            // Update UI to show downloading state
            updateDocumentState(documentId) { it.copy(isDownloading = true, downloadProgress = 0f) }
            
            try {
                // TODO: Implement actual download logic
                // This would call the API to get full document content
                // and store it in Room database
                
                // For now, just simulate a successful download
                kotlinx.coroutines.delay(1000)
                
                updateDocumentState(documentId) { 
                    it.copy(isDownloading = false, isDownloaded = true, downloadProgress = 1f) 
                }
            } catch (e: Exception) {
                updateDocumentState(documentId) { 
                    it.copy(isDownloading = false, downloadProgress = 0f) 
                }
            }
        }
    }

    /**
     * Delete a downloaded document to free up space.
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            // TODO: Implement actual delete logic
            // This would remove articles for this document from Room
            
            updateDocumentState(documentId) { 
                it.copy(isDownloaded = false, downloadProgress = 0f) 
            }
        }
    }

    private fun updateDocumentState(
        documentId: String, 
        update: (DocumentDownloadState) -> DocumentDownloadState
    ) {
        val currentDocs = _uiState.value.documents
        val updatedDocs = currentDocs.map { 
            if (it.id == documentId) update(it) else it 
        }
        _uiState.value = _uiState.value.copy(documents = updatedDocs)
    }
}
