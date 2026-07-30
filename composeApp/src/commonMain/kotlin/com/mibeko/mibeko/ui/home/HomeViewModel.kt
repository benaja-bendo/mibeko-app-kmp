package com.mibeko.mibeko.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.remote.ApiResponse
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.ResendVerificationResult
import com.mibeko.mibeko.data.repository.CorpusRefreshResult
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.getCurrentTimeMillis
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Represents a recently viewed item for the home screen.
 */
data class RecentItem(
    val id: String,
    val title: String
)

/**
 * UI State for the home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isNetworkAvailable: Boolean = true,
    val isOfflineMode: Boolean = false,
    val isLoggedIn: Boolean = false,
    val recentItems: List<RecentItem> = emptyList(),
    val popularCodes: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList(),
    val recentlyAdded: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList(),
    val officialJournals: List<com.mibeko.mibeko.data.remote.RemoteOfficialJournal> = emptyList(),
    val aiSuggestions: List<String> = emptyList(),
    val isSyncing: Boolean = false,
    /**
     * Bannière « e-mail non vérifié » (posture douce, non bloquante). N'apparaît
     * que si le profil expose explicitement `email_verified == false` : si le
     * champ manque (login/register, backend plus ancien), dégradation
     * silencieuse — pas de bannière.
     */
    val showEmailVerificationBanner: Boolean = false,
    val isResendingVerification: Boolean = false,
    /** Message transitoire de résultat du renvoi (succès / throttle / erreur). */
    val verificationResendMessage: String? = null,
    /** Cooldown local anti-spam : timestamp epoch ms de fin d'attente. */
    val verificationResendCooldownUntil: Long = 0L
)

class HomeViewModel(
    private val repository: LocalLegalRepository,
    private val networkChecker: NetworkConnectivityChecker,
    private val userPreferences: UserPreferencesRepository,
    private val authApiService: AuthApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    val lawCodes: StateFlow<List<LawCodeSpec>> = repository.getLawCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInitialState()
        initialSyncIfNeeded()
        loadHomeData()
        checkEmailVerification()
    }

    /**
     * Posture douce P1.17 : au démarrage connecté, interroge le profil et, si
     * l'e-mail n'est explicitement pas vérifié, arme la bannière non bloquante.
     * Toute absence de champ / erreur réseau → pas de bannière (silencieux).
     */
    private fun checkEmailVerification() {
        if (!userPreferences.isLoggedIn()) return
        if (!networkChecker.isNetworkAvailable()) return
        viewModelScope.launch {
            try {
                val response = authApiService.getProfile()
                // Bannière uniquement si le backend dit explicitement « non vérifié ».
                val unverified = response.success && response.data?.email_verified == false
                if (unverified) {
                    _uiState.value = _uiState.value.copy(showEmailVerificationBanner = true)
                }
            } catch (e: Exception) {
                // Dégradation silencieuse : aucune bannière, aucun crash.
            }
        }
    }

    /**
     * Renvoie l'e-mail de vérification (bouton de la bannière). Respecte un
     * cooldown local de 60 s et gère l'étranglement serveur (429).
     */
    fun resendEmailVerification() {
        val now = getCurrentTimeMillis()
        if (_uiState.value.isResendingVerification) return
        if (now < _uiState.value.verificationResendCooldownUntil) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isResendingVerification = true,
                verificationResendMessage = null
            )
            val result = authApiService.resendEmailVerification()
            val message: String
            val cooldownUntil: Long
            when (result) {
                ResendVerificationResult.SENT -> {
                    message = "E-mail de vérification envoyé. Pensez à vérifier vos spams."
                    cooldownUntil = now + COOLDOWN_MS
                }
                ResendVerificationResult.THROTTLED -> {
                    message = "Trop de tentatives. Réessayez dans quelques minutes."
                    cooldownUntil = now + COOLDOWN_MS
                }
                ResendVerificationResult.ERROR -> {
                    message = "L'envoi a échoué. Vérifiez votre connexion puis réessayez."
                    cooldownUntil = _uiState.value.verificationResendCooldownUntil
                }
            }
            _uiState.value = _uiState.value.copy(
                isResendingVerification = false,
                verificationResendMessage = message,
                verificationResendCooldownUntil = cooldownUntil
            )
        }
    }

    fun clearVerificationResendMessage() {
        _uiState.value = _uiState.value.copy(verificationResendMessage = null)
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            var validPopular: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList()
            var validRecentlyAdded: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList()
            var suggestions: List<String> = emptyList()
            var journals: List<com.mibeko.mibeko.data.remote.RemoteOfficialJournal> = emptyList()
            var isOffline = false

            if (networkChecker.isNetworkAvailable()) {
                // Fetch Home Data
                try {
                    val homeResponse = repository.getHomeData()
                    if (homeResponse.success && homeResponse.data != null) {
                        validPopular = homeResponse.data.popular_codes.filter { 
                            it.id.isNotBlank() && it.title.isNotBlank() 
                        }
                        validRecentlyAdded = homeResponse.data.recently_added.filter { 
                            it.id.isNotBlank() && it.title.isNotBlank() 
                        }
                        suggestions = homeResponse.data.ai_suggestions
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fetch Official Journals
                try {
                    val journalsResponse = repository.getOfficialJournals()
                    if (journalsResponse.success) {
                        journals = journalsResponse.data
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                isOffline = true
            }

            // Fallback for popular codes if we didn't get any from API
            if (validPopular.isEmpty()) {
                try {
                    val codes = repository.getLawCodes().first()
                    if (codes.isNotEmpty()) {
                        validPopular = codes.take(5).map { code ->
                            com.mibeko.mibeko.data.remote.RemoteDocument(
                                id = code.id,
                                title = code.title,
                                status = "published",
                                updated_at = ""
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _uiState.value = _uiState.value.copy(
                popularCodes = validPopular,
                recentlyAdded = validRecentlyAdded,
                officialJournals = journals,
                aiSuggestions = suggestions,
                isOfflineMode = isOffline,
                isLoading = false
            )
        }
    }


    private fun loadInitialState() {
        // Check initial network state
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.value = _uiState.value.copy(
            isNetworkAvailable = isOnline,
            isLoggedIn = userPreferences.isLoggedIn(),
            isLoading = true
        )
        
        loadRecentItems()
    }

    /**
     * Aligne le corpus local au démarrage.
     *
     * Trois cas, dans cet ordre :
     * 1. base vide → synchronisation initiale ;
     * 2. synchronisation initiale interrompue (curseur posé) → REPRISE — sans
     *    cela le corpus restait tronqué à vie, l'ancienne garde « base vide »
     *    ne se déclenchant plus dès la première page écrite ;
     * 3. corpus complet → rafraîchissement différentiel au plus une fois par
     *    jour, qui ne re-télécharge que les textes réellement modifiés.
     */
    private fun initialSyncIfNeeded() {
        viewModelScope.launch {
            val codes = repository.getLawCodes().first()
            val needsFullSync = codes.isEmpty() || userPreferences.isInitialSyncIncomplete()

            if (needsFullSync && networkChecker.isNetworkAvailable()) {
                syncData()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
                refreshCorpusIfStale()
            }
        }
    }

    /**
     * Rafraîchissement discret du corpus, au plus une fois par 24 h. Silencieux
     * par construction : il ne doit ni bloquer l'accueil ni afficher d'erreur,
     * l'utilisateur garde son corpus local en attendant.
     */
    private fun refreshCorpusIfStale() {
        if (!networkChecker.isNetworkAvailable()) return

        val since = getCurrentTimeMillis() - userPreferences.getLastCorpusRefreshAt()
        if (userPreferences.getLastCorpusRefreshAt() != 0L && since < CORPUS_REFRESH_INTERVAL_MS) return

        viewModelScope.launch {
            val result = runCatching { repository.refreshCorpus() }.getOrNull()
            if (result is CorpusRefreshResult.Refreshed && result.updated > 0) {
                _uiEvent.emit(
                    if (result.updated == 1) "1 texte a été mis à jour."
                    else "${result.updated} textes ont été mis à jour."
                )
            }
        }
    }
    
    /**
     * Refresh network status.
     */
    fun refreshNetworkStatus() {
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.value = _uiState.value.copy(
            isNetworkAvailable = isOnline,
            isLoggedIn = userPreferences.isLoggedIn()
        )
    }
    
    private fun loadRecentItems() {
        // For now, this is a placeholder. In a real implementation,
        // this would query recent views from local storage.
        // The UI will show demo items if this is empty.
    }

    fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, isLoading = true)
            try {
                repository.sync()
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = "Erreur de synchronisation: ${e.message ?: "Inconnue"}"
                _uiEvent.emit(errorMsg)
            } finally {
                _uiState.value = _uiState.value.copy(isSyncing = false, isLoading = false)
            }
        }
    }

    private companion object {
        /** Intervalle minimal entre deux rafraîchissements automatiques (24 h). */
        const val CORPUS_REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L

        /** Cooldown local anti-spam du renvoi de vérification (60 s). */
        const val COOLDOWN_MS = 60_000L
    }
}
