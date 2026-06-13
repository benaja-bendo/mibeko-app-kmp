package com.mibeko.mibeko.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.ProfileUpdateRequest
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
    val appVersion: String = "v1.0.0",
    
    // Profile Fields
    val userName: String = "",
    val userEmail: String = "",
    val phone: String = "",
    val profession: String = "",
    val company: String = "",
    val isUpdatingProfile: Boolean = false,
    val profileUpdateMessage: String? = null,
    
    // Password Fields
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isUpdatingPassword: Boolean = false,
    val passwordUpdateMessage: String? = null
)

/**
 * ViewModel for the Settings screen.
 * Manages offline mode toggle, document download management, application settings, and profile.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val legalRepository: LocalLegalRepository,
    private val notificationManager: NotificationManager,
    private val notificationRepository: NotificationRepository,
    private val authApiService: AuthApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        refreshDiskUsage()
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val response = authApiService.getProfile()
                if (response.success && response.data != null) {
                    val user = response.data
                    _uiState.value = _uiState.value.copy(
                        userName = user.name,
                        userEmail = user.email,
                        phone = user.mobile_profile?.phone ?: "",
                        profession = user.mobile_profile?.profession ?: "",
                        company = user.mobile_profile?.company ?: ""
                    )
                }
            } catch (e: Exception) {
                // Ignore for now or handle silently
            }
        }
    }

    fun updateProfileField(field: String, value: String) {
        when (field) {
            "name" -> _uiState.value = _uiState.value.copy(userName = value)
            "phone" -> _uiState.value = _uiState.value.copy(phone = value)
            "profession" -> _uiState.value = _uiState.value.copy(profession = value)
            "company" -> _uiState.value = _uiState.value.copy(company = value)
        }
    }

    fun updatePasswordField(field: String, value: String) {
        when (field) {
            "currentPassword" -> _uiState.value = _uiState.value.copy(currentPassword = value)
            "newPassword" -> _uiState.value = _uiState.value.copy(newPassword = value)
            "confirmPassword" -> _uiState.value = _uiState.value.copy(confirmPassword = value)
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingProfile = true, profileUpdateMessage = null)
            try {
                val request = com.mibeko.mibeko.data.remote.ProfileUpdateRequest(
                    name = _uiState.value.userName,
                    phone = _uiState.value.phone,
                    profession = _uiState.value.profession,
                    company = _uiState.value.company
                )
                val response = authApiService.updateProfile(request)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingProfile = false,
                        profileUpdateMessage = "Profil mis à jour avec succès"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingProfile = false,
                        profileUpdateMessage = response.message ?: "Erreur lors de la mise à jour"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingProfile = false,
                    profileUpdateMessage = "Erreur de connexion"
                )
            }
        }
    }

    fun updatePassword() {
        if (_uiState.value.newPassword != _uiState.value.confirmPassword) {
            _uiState.value = _uiState.value.copy(passwordUpdateMessage = "Les mots de passe ne correspondent pas")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingPassword = true, passwordUpdateMessage = null)
            try {
                val request = com.mibeko.mibeko.data.remote.PasswordUpdateRequest(
                    current_password = _uiState.value.currentPassword,
                    password = _uiState.value.newPassword,
                    password_confirmation = _uiState.value.confirmPassword
                )
                val response = authApiService.updatePassword(request)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPassword = false,
                        passwordUpdateMessage = "Mot de passe mis à jour avec succès",
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPassword = false,
                        passwordUpdateMessage = response.message ?: "Erreur lors de la mise à jour"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingPassword = false,
                    passwordUpdateMessage = "Erreur de connexion"
                )
            }
        }
    }

    fun clearProfileUpdateMessage() {
        _uiState.value = _uiState.value.copy(profileUpdateMessage = null)
    }

    fun clearPasswordUpdateMessage() {
        _uiState.value = _uiState.value.copy(passwordUpdateMessage = null)
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                authApiService.logout()
            } catch (e: Exception) {
                // Proceed to logout locally even if API fails
            } finally {
                userPreferencesRepository.logout()
                authApiService.invalidateTokenCache()
                legalRepository.clearAllData()
                onLogoutComplete()
            }
        }
    }

    /**
     * Suppression du compte (droit à l'effacement, exigence Play Store /
     * App Store). L'API exige le mot de passe courant ; en cas de succès,
     * la session locale est entièrement nettoyée.
     */
    fun deleteAccount(
        currentPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = authApiService.deleteAccount(currentPassword)
                if (response.success) {
                    userPreferencesRepository.logout()
                    authApiService.invalidateTokenCache()
                    legalRepository.clearAllData()
                    onSuccess()
                } else {
                    val message = response.errors?.values?.firstOrNull()?.firstOrNull()
                        ?: response.message
                        ?: "La suppression du compte a échoué."
                    onError(message)
                }
            } catch (e: Exception) {
                onError(e.message ?: "La suppression du compte a échoué.")
            }
        }
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
            legalRepository.clearAllData()
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
