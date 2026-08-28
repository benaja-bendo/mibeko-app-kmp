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
import com.mibeko.mibeko.util.UiResult
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    /**
     * Non-null quand le chargement de l'accueil (codes populaires, récemment
     * ajoutés, suggestions) a échoué alors que le réseau était disponible —
     * jamais posé en pur hors-ligne, déjà signalé par [NetworkStatusBanner].
     */
    val homeDataError: UiResult.Error? = null,
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
        observeConnectivity()
        initialSyncIfNeeded()
        loadHomeData()
        checkEmailVerification()
    }

    /**
     * Suit l'état réseau en continu et **rattrape** ce qu'un démarrage hors-ligne
     * a manqué.
     *
     * Sans cela, la connectivité n'était échantillonnée qu'une fois, ici même
     * dans le `init` : une app lancée pendant que le réseau n'était pas encore
     * joignable n'appelait plus jamais l'API et sautait la synchronisation
     * initiale du corpus — pour toute la session, et sur une première
     * installation le corpus n'était donc jamais téléchargé.
     */
    private fun observeConnectivity() {
        viewModelScope.launch {
            var wasOnline = networkChecker.isNetworkAvailable()
            networkChecker.isOnline.collect { online ->
                _uiState.update { it.copy(isNetworkAvailable = online) }
                if (online && !wasOnline) {
                    loadHomeData()
                    initialSyncIfNeeded()
                }
                wasOnline = online
            }
        }
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
                    _uiState.update { it.copy(showEmailVerificationBanner = true) }
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
            _uiState.update { it.copy(
                isResendingVerification = true,
                verificationResendMessage = null
            ) }
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
            _uiState.update { it.copy(
                isResendingVerification = false,
                verificationResendMessage = message,
                verificationResendCooldownUntil = cooldownUntil
            ) }
        }
    }

    fun clearVerificationResendMessage() {
        _uiState.update { it.copy(verificationResendMessage = null) }
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, homeDataError = null) }

            var validPopular: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList()
            var validRecentlyAdded: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList()
            var suggestions: List<String> = emptyList()
            var journals: List<com.mibeko.mibeko.data.remote.RemoteOfficialJournal> = emptyList()
            var isOffline = false
            var homeDataFailed = false

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
                    recordException(e, context = "HomeViewModel.loadHomeData")
                    homeDataFailed = true
                }

                // Fetch Official Journals
                try {
                    val journalsResponse = repository.getOfficialJournals()
                    if (journalsResponse.success) {
                        journals = journalsResponse.data
                    }
                } catch (e: Exception) {
                    recordException(e, context = "HomeViewModel.loadOfficialJournals")
                    // Panne isolée : la section Journal Officiel reste simplement absente,
                    // pas de bannière dédiée (aucune affirmation « aucune actualité » n'est faite).
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
                    recordException(e, context = "HomeViewModel.loadHomeData.fallback")
                }
            }

            _uiState.update { it.copy(
                popularCodes = validPopular,
                recentlyAdded = validRecentlyAdded,
                officialJournals = journals,
                aiSuggestions = suggestions,
                isOfflineMode = isOffline,
                isLoading = false,
                // Jamais posé en pur hors-ligne : NetworkStatusBanner le signale déjà.
                homeDataError = if (homeDataFailed && !isOffline) {
                    UiResult.Error(offline = !networkChecker.isNetworkAvailable(), retry = ::loadHomeData)
                } else {
                    null
                }
            ) }
        }
    }

    /** Recharge l'accueil (tiré vers le bas). */
    fun refresh() {
        loadHomeData()
    }


    private fun loadInitialState() {
        // Check initial network state
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.update { it.copy(
            isNetworkAvailable = isOnline,
            isLoggedIn = userPreferences.isLoggedIn(),
            isLoading = true
        ) }
        
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
        // Une synchronisation déjà en vol ne doit pas être relancée par un
        // rebond de connectivité.
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            val codes = repository.getLawCodes().first()
            val needsFullSync = codes.isEmpty() || userPreferences.isInitialSyncIncomplete()

            if (needsFullSync && networkChecker.isNetworkAvailable()) {
                syncData()
            } else {
                _uiState.update { it.copy(isLoading = false) }
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
    
    private fun loadRecentItems() {
        // For now, this is a placeholder. In a real implementation,
        // this would query recent views from local storage.
        // The UI will show demo items if this is empty.
    }

    fun syncData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, isLoading = true) }
            try {
                repository.sync()
            } catch (e: Exception) {
                recordException(e, context = "HomeViewModel.syncData")
                _uiEvent.emit("La synchronisation a échoué. Réessayez plus tard.")
            } finally {
                _uiState.update { it.copy(isSyncing = false, isLoading = false) }
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
