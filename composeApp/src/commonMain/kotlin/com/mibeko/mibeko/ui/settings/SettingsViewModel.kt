package com.mibeko.mibeko.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.NotificationRepository
import com.mibeko.mibeko.util.NotificationManager
import com.mibeko.mibeko.util.formatSize
import com.mibeko.mibeko.util.formatTimestampToDate
import com.mibeko.mibeko.util.getDatabaseSize
import com.mibeko.mibeko.util.getDeviceId
import com.mibeko.mibeko.getPlatform
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mibeko.mibeko.getCurrentTimeMillis

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
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val currentTheme: UserPreferencesRepository.AppTheme = UserPreferencesRepository.AppTheme.SYSTEM,
    val isNotificationsEnabled: Boolean = false,
    val lastUpdateDate: String = "Jamais",
    val diskUsage: String = "0 B",
    val textSize: UserPreferencesRepository.TextSize = UserPreferencesRepository.TextSize.MEDIUM,
    val isDyslexiaFontEnabled: Boolean = false,

    val isLegalMonitoringEnabled: Boolean = true,
    val isDossierAlertsEnabled: Boolean = true,
    val appVersion: String = "v1.0.0"
)

/**
 * ViewModel for the Settings screen.
 * Manages offline mode toggle, document download management, and application settings.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val legalRepository: LocalLegalRepository,
    private val notificationManager: NotificationManager,
    private val notificationRepository: NotificationRepository
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
            lastUpdateDate = formatTimestampToDate(lastSync),
            textSize = userPreferencesRepository.getTextSize(),
            isDyslexiaFontEnabled = userPreferencesRepository.isDyslexiaFontEnabled(),
            isLegalMonitoringEnabled = userPreferencesRepository.isLegalMonitoringEnabled(),
            isDossierAlertsEnabled = userPreferencesRepository.isDossierAlertsEnabled()
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
                        isDownloaded = code.isDownloaded,
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
     * Update the text size.
     */
    fun setTextSize(size: UserPreferencesRepository.TextSize) {
        userPreferencesRepository.setTextSize(size)
        _uiState.value = _uiState.value.copy(textSize = size)
    }

    /**
     * Toggle dyslexia font.
     */
    fun setDyslexiaFontEnabled(enabled: Boolean) {
        userPreferencesRepository.setDyslexiaFontEnabled(enabled)
        _uiState.value = _uiState.value.copy(isDyslexiaFontEnabled = enabled)
    }



    /**
     * Toggle legal monitoring.
     */
    fun setLegalMonitoringEnabled(enabled: Boolean) {
        userPreferencesRepository.setLegalMonitoringEnabled(enabled)
        _uiState.value = _uiState.value.copy(isLegalMonitoringEnabled = enabled)
    }

    /**
     * Toggle dossier alerts.
     */
    fun setDossierAlertsEnabled(enabled: Boolean) {
        userPreferencesRepository.setDossierAlertsEnabled(enabled)
        _uiState.value = _uiState.value.copy(isDossierAlertsEnabled = enabled)
    }

    /**
     * Toggle notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled) {
            notificationManager.requestPermission { granted ->
                if (granted) {
                    userPreferencesRepository.setNotificationsEnabled(true)
                    _uiState.value = _uiState.value.copy(isNotificationsEnabled = true)

                    // Register device on backend
                    viewModelScope.launch {
                        notificationManager.getPushToken { token ->
                            if (token != null) {
                                viewModelScope.launch {
                                    val platformName = getPlatform().name.lowercase()
                                    val backendPlatform = if (platformName.contains("android")) "android" else "ios"
                                    
                                    notificationRepository.registerDevice(
                                        deviceId = getDeviceId(),
                                        pushToken = token,
                                        platform = backendPlatform
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            userPreferencesRepository.setNotificationsEnabled(false)
            _uiState.value = _uiState.value.copy(isNotificationsEnabled = false)

            // Unregister device on backend
            viewModelScope.launch {
                notificationRepository.unregisterDevice(getDeviceId())
            }
        }
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
     * Clear all offline data and database.
     */
    fun clearStorage() {
        viewModelScope.launch {
            // TODO: Implement actual clear storage logic
            // legalRepository.clearAllData()
            refreshDiskUsage()
        }
    }



    fun clearSyncError() {
        _uiState.value = _uiState.value.copy(syncError = null)
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
