package com.mibeko.mibeko.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.NotificationTypes
import com.mibeko.mibeko.data.remote.ProfileUpdateRequest
import com.mibeko.mibeko.data.repository.CorpusRefreshResult
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.NotificationRepository
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.NotificationManager
import com.mibeko.mibeko.util.formatSize
import com.mibeko.mibeko.util.formatTimestampToDate
import com.mibeko.mibeko.util.getAppVersionName
import com.mibeko.mibeko.util.getDatabaseSize
import com.mibeko.mibeko.util.getDeviceId
import com.mibeko.mibeko.util.recordException
import com.mibeko.mibeko.getPlatform
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import com.mibeko.mibeko.getCurrentTimeMillis

/** Borne des travaux réseau best-effort exécutés avant une purge locale. */
private const val REMOTE_CLEANUP_TIMEOUT_MS = 5_000L

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
    val isTelemetryEnabled: Boolean = true,
    val appVersion: String = "v1.0.0",
    
    // Profile Fields
    val userName: String = "",
    val userEmail: String = "",
    val phone: String = "",
    val profession: String = "",
    val company: String = "",
    val isUpdatingProfile: Boolean = false,
    val profileUpdateMessage: String? = null,
    val isExportingData: Boolean = false,
    
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
    private val authApiService: AuthApiService,
    private val dossierRepository: DossierRepository,
    private val analytics: MibekoAnalytics,
    private val contentSharer: com.mibeko.mibeko.util.ContentSharer
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
                // Dégradation silencieuse assumée : userName/userEmail restent
                // les valeurs locales posées au login (voir loadInitialState),
                // donc l'écran reste correct même si ce rafraîchissement échoue.
                recordException(e, context = "SettingsViewModel.fetchProfile")
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

    /**
     * Travaux réseau best-effort exécutés avant une purge locale (déconnexion,
     * suppression de compte). Bornés dans le temps : sur un réseau qui pend,
     * les timeouts Ktor (jusqu'à 60 s) laisseraient l'utilisateur bloqué sur
     * un bouton « Se déconnecter » sans réponse.
     */
    private suspend fun runBestEffort(context: String, block: suspend () -> Unit) {
        try {
            withTimeoutOrNull(REMOTE_CLEANUP_TIMEOUT_MS) { block() }
        } catch (e: Exception) {
            recordException(e, context)
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            // Avant de purger : pousser l'état des dossiers (les suppressions
            // faites hors-ligne vivent dans pending_dossier_deletions, que
            // clearAllData efface — sans ce push, elles seraient perdues) et
            // détacher l'appareil des push (sinon un envoi « ciblé » partirait
            // vers un utilisateur déconnecté).
            runBestEffort("logout_dossier_sync") { dossierRepository.syncNow() }
            runBestEffort("logout_unregister_device") {
                notificationRepository.unregisterDevice(getDeviceId())
            }
            runBestEffort("logout_api") { authApiService.logout() }

            userPreferencesRepository.logout()
            authApiService.invalidateTokenCache()
            legalRepository.clearAllData()
            onLogoutComplete()
        }
    }

    /**
     * Rafraîchissement manuel du corpus. Ne re-télécharge que les textes dont
     * l'empreinte a changé côté serveur, et retire ceux qui ne sont plus
     * publiés.
     */
    fun refreshCorpus() {
        if (_uiState.value.isSyncing) return
        _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null)
        viewModelScope.launch {
            val message = when (val result = legalRepository.refreshCorpus(force = true)) {
                is CorpusRefreshResult.UpToDate -> "Votre corpus est déjà à jour."
                is CorpusRefreshResult.Offline -> "Aucune connexion : réessayez une fois en ligne."
                is CorpusRefreshResult.Failed -> "Mise à jour impossible : ${result.reason}"
                is CorpusRefreshResult.Refreshed -> buildString {
                    when {
                        result.updated == 0 && result.removed == 0 -> append("Aucun changement.")
                        else -> {
                            if (result.updated > 0) append("${result.updated} texte(s) mis à jour")
                            if (result.removed > 0) {
                                if (result.updated > 0) append(", ")
                                append("${result.removed} retiré(s) du corpus")
                            }
                            append(".")
                        }
                    }
                    if (result.partial) append(" Certains textes n'ont pas pu être récupérés.")
                }
            }
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastUpdateDate = formatTimestampToDate(userPreferencesRepository.getLastSyncTimestamp()),
                profileUpdateMessage = message
            )
        }
    }

    /**
     * Export des données personnelles (droit d'accès) : le JSON renvoyé par
     * l'API est remis à l'utilisateur via le partage système (enregistrement,
     * envoi par mail…). C'est le parcours annoncé par la politique de
     * confidentialité publiée sur mibeko.fr.
     */
    fun exportPersonalData() {
        if (_uiState.value.isExportingData) return
        _uiState.value = _uiState.value.copy(isExportingData = true)
        viewModelScope.launch {
            try {
                val bytes = authApiService.exportPersonalData()
                contentSharer.shareFile(
                    bytes = bytes,
                    fileName = "mibeko-mes-donnees.json",
                    mimeType = "application/json"
                )
                _uiState.value = _uiState.value.copy(
                    isExportingData = false,
                    profileUpdateMessage = "Export prêt : choisissez où l'enregistrer."
                )
            } catch (e: Exception) {
                recordException(e, "export_personal_data")
                _uiState.value = _uiState.value.copy(
                    isExportingData = false,
                    profileUpdateMessage = "L'export a échoué. Vérifiez votre connexion et réessayez."
                )
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
                    // Même détachement qu'à la déconnexion : sans lui, le
                    // backend garderait l'appareil enregistré pour un compte
                    // supprimé et continuerait de lui pousser des alertes.
                    runBestEffort("delete_account_unregister_device") {
                        notificationRepository.unregisterDevice(getDeviceId())
                    }
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
            isDossierAlertsEnabled = userPreferencesRepository.isDossierAlertsEnabled(),
            isTelemetryEnabled = analytics.isTelemetryEnabled(),
            appVersion = "v${getAppVersionName()}",
            // isAuthenticated (Screen) dépend de userName/userEmail : les lire
            // localement d'abord évite qu'un utilisateur connecté hors-ligne
            // retombe sur l'en-tête « Invité » (setUserInfo les pose déjà au
            // login/inscription). fetchProfile() les rafraîchit ensuite si le
            // réseau est là.
            userName = userPreferencesRepository.getUserName() ?: "",
            userEmail = userPreferencesRepository.getUserEmail() ?: ""
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
        syncNotificationPreferences()
    }

    /**
     * Toggle dossier alerts.
     */
    fun setDossierAlertsEnabled(enabled: Boolean) {
        userPreferencesRepository.setDossierAlertsEnabled(enabled)
        _uiState.value = _uiState.value.copy(isDossierAlertsEnabled = enabled)
        syncNotificationPreferences()
    }

    /**
     * Reporte les réglages de l'app dans la matrice serveur, seule consultée
     * par les expéditeurs de veille.
     *
     * Sans cela le toggle mentait : le canal push est désactivé par défaut côté
     * serveur, un utilisateur connecté n'aurait donc jamais rien reçu malgré un
     * réglage affiché « activé ». On ne modifie QUE le canal push : les choix
     * e-mail et in-app faits depuis le web restent intacts.
     */
    private fun syncNotificationPreferences() {
        if (!userPreferencesRepository.isLoggedIn()) return

        viewModelScope.launch {
            runBestEffort("sync_notification_preferences") {
                val current = authApiService.getNotificationPreferences() ?: return@runBestEffort
                val notificationsOn = userPreferencesRepository.isNotificationsEnabled()
                val watchOn = notificationsOn && userPreferencesRepository.isLegalMonitoringEnabled()
                val dossierOn = notificationsOn && userPreferencesRepository.isDossierAlertsEnabled()

                val updated = buildJsonObject {
                    for ((key, value) in current) {
                        // Tout ce qui n'est pas un objet de canaux (`_frequency`,
                        // clés futures) est recopié tel quel : on ne réécrit que
                        // le canal push, jamais les choix e-mail / in-app faits
                        // depuis le web.
                        val channels = value as? JsonObject
                        if (channels == null) {
                            put(key, value)
                            continue
                        }

                        val push = when (key) {
                            NotificationTypes.NEW_DOCUMENT, NotificationTypes.LEGAL_ALERT -> watchOn
                            NotificationTypes.SHARE -> dossierOn
                            NotificationTypes.EXTRACTION_UPDATE, NotificationTypes.SYSTEM -> notificationsOn
                            else -> null
                        }
                        if (push == null) {
                            put(key, channels)
                        } else {
                            put(
                                key,
                                buildJsonObject {
                                    for ((channel, enabled) in channels) put(channel, enabled)
                                    put("push", JsonPrimitive(push))
                                }
                            )
                        }
                    }
                }

                authApiService.updateNotificationPreferences(updated)
            }
        }
    }

    /**
     * Toggle « Partage de statistiques anonymes » : pilote la préférence ET la
     * collecte au niveau du SDK Analytics des deux plateformes.
     */
    fun setTelemetryEnabled(enabled: Boolean) {
        analytics.setTelemetryEnabled(enabled)
        _uiState.value = _uiState.value.copy(isTelemetryEnabled = enabled)
    }

    /**
     * Toggle notifications.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled) {
            notificationManager.requestPermission { granted ->
                analytics.logEvent(
                    AnalyticsEvents.NOTIFICATION_OPT_IN,
                    mapOf("granted" to granted)
                )
                if (granted) {
                    userPreferencesRepository.setNotificationsEnabled(true)
                    _uiState.value = _uiState.value.copy(isNotificationsEnabled = true)
                    // L'accord donné dans l'app doit atteindre la matrice
                    // serveur, sinon aucun push ne partira jamais.
                    syncNotificationPreferences()

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
                            } else {
                                // Pas de jeton push sur cette plateforme (iOS :
                                // APNs pas encore branché). Le réglage est bien
                                // mémorisé, mais laisser croire que des alertes
                                // vont arriver serait mensonger.
                                _uiState.value = _uiState.value.copy(
                                    profileUpdateMessage = "Préférence enregistrée. Les alertes push arriveront sur iPhone dans une prochaine version."
                                )
                            }
                        }
                    }
                }
            }
        } else {
            userPreferencesRepository.setNotificationsEnabled(false)
            _uiState.value = _uiState.value.copy(isNotificationsEnabled = false)
            syncNotificationPreferences()

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
                // Téléchargement réel : structure + articles stockés dans Room.
                legalRepository.downloadDocument(documentId)

                updateDocumentState(documentId) {
                    it.copy(isDownloading = false, isDownloaded = true, downloadProgress = 1f)
                }
                refreshDiskUsage()
            } catch (e: Exception) {
                updateDocumentState(documentId) {
                    it.copy(isDownloading = false, isDownloaded = false, downloadProgress = 0f)
                }
                _uiState.value = _uiState.value.copy(
                    syncError = "Le téléchargement du document a échoué. Vérifiez votre connexion."
                )
            }
        }
    }

    /**
     * Delete a downloaded document to free up space.
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                // Suppression réelle des articles non favoris du document dans Room.
                legalRepository.removeDownload(documentId)

                updateDocumentState(documentId) {
                    it.copy(isDownloaded = false, downloadProgress = 0f)
                }
                refreshDiskUsage()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    syncError = "La suppression du document a échoué."
                )
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
