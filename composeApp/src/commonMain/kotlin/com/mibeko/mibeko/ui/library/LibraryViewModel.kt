package com.mibeko.mibeko.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.preferences.RecentlyViewedItem
import com.mibeko.mibeko.data.preferences.RecentlyViewedManager
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.remote.LibraryApiService
import com.mibeko.mibeko.data.remote.LibraryHomeData
import com.mibeko.mibeko.data.remote.LibraryPagination
import com.mibeko.mibeko.data.remote.LibrarySearchItem
import com.mibeko.mibeko.data.remote.LibrarySuggestions
import com.mibeko.mibeko.data.remote.RemoteDocumentType
import com.mibeko.mibeko.data.remote.RemoteInstitution
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.SearchResult
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.UiResult
import com.mibeko.mibeko.util.recordException
import com.mibeko.mibeko.util.parseRemoteDateToEpochMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Périmètre juridique — aligné sur `legal_documents.legal_scope` (web). */
enum class LibraryScope(val apiValue: String?, val label: String) {
    ALL(null, "Tous"),
    NATIONAL("national", "National"),
    OHADA("ohada", "OHADA"),
    COMMUNAUTAIRE("communautaire", "Communautaire")
}

/** Tri des résultats, mêmes valeurs que le web. */
enum class LibrarySort(val apiValue: String, val label: String) {
    RELEVANCE("relevance", "Pertinence"),
    DATE_DESC("date_desc", "Plus récents"),
    DATE_ASC("date_asc", "Plus anciens")
}

data class LibraryUiState(
    // ── Accueil vivant ────────────────────────────────────────────────────
    val isLoading: Boolean = false,
    val home: LibraryHomeData? = null,
    /** Documents présents en base locale (repli hors-ligne + état téléchargé). */
    val localCodes: List<LawCodeSpec> = emptyList(),
    /** Connectivité réellement absente — **pas** « le dernier appel a échoué ». */
    val isOffline: Boolean = false,
    /**
     * Non-null quand `/library/home` a échoué alors que le réseau était
     * disponible. Jamais posé en pur hors-ligne, déjà dit par [isOffline].
     */
    val homeError: UiResult.Error? = null,
    // ── Recherche serveur (granularité article, comme le web) ────────────
    val searchQuery: String = "",
    val submittedQuery: String = "",
    val isSearching: Boolean = false,
    val results: List<LibrarySearchItem> = emptyList(),
    val pagination: LibraryPagination? = null,
    val isLoadingMore: Boolean = false,
    /** false quand les résultats viennent de la base locale (mode hors-ligne). */
    val resultsFromNetwork: Boolean = true,
    /**
     * Non-null quand la dernière tentative de recherche a échoué — que des
     * résultats de repli soient affichés ou non (voir [UiResult.Error]).
     * Jamais null simplement parce que [results] est vide.
     */
    val searchError: UiResult.Error? = null,
    // ── Autocomplétion ────────────────────────────────────────────────────
    val suggestions: LibrarySuggestions? = null,
    // ── Filtres (appliqués côté serveur) ─────────────────────────────────
    val scope: LibraryScope = LibraryScope.ALL,
    val selectedTypeCode: String? = null,
    val selectedInstitutionId: String? = null,
    val sort: LibrarySort = LibrarySort.RELEVANCE,
    val documentTypes: List<RemoteDocumentType> = emptyList(),
    val institutions: List<RemoteInstitution> = emptyList(),
    // ── Divers ────────────────────────────────────────────────────────────
    val downloadingIds: Set<String> = emptySet(),
    val error: String? = null
) {
    val hasSearched: Boolean get() = submittedQuery.length >= 2
    val activeFilterCount: Int
        get() = (if (scope != LibraryScope.ALL) 1 else 0) +
            (if (selectedTypeCode != null) 1 else 0) +
            (if (selectedInstitutionId != null) 1 else 0) +
            (if (sort != LibrarySort.RELEVANCE) 1 else 0)
}

/**
 * Bibliothèque alignée sur le poste de travail web : accueil vivant servi par
 * `/library/home`, recherche plein-texte serveur `/library/search` et
 * autocomplétion `/library/suggest`. Hors-ligne, l'écran retombe sur la base
 * Room (documents téléchargés + recherche FTS locale).
 */
class LibraryViewModel(
    private val repository: LocalLegalRepository,
    private val recentlyViewedManager: RecentlyViewedManager,
    private val libraryApi: LibraryApiService,
    private val legalApi: LegalApiService,
    private val networkChecker: NetworkConnectivityChecker,
    private val analytics: MibekoAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val recentItems: StateFlow<List<RecentlyViewedItem>> = recentlyViewedManager.recentItems

    private var suggestJob: Job? = null
    private var searchJob: Job? = null

    init {
        observeLocalDocuments()
        observeConnectivity()
        loadHome()
        loadFilterOptions()
    }

    /**
     * Suit l'état réseau et recharge à la reconnexion. Sans cela, une
     * Bibliothèque ouverte pendant une coupure restait vide jusqu'à ce que
     * l'utilisateur pense à tirer vers le bas.
     */
    private fun observeConnectivity() {
        viewModelScope.launch {
            var wasOnline = networkChecker.isNetworkAvailable()
            networkChecker.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
                if (online && !wasOnline) {
                    loadHome()
                    loadFilterOptions()
                }
                wasOnline = online
            }
        }
    }

    fun refresh() {
        loadHome()
        loadFilterOptions()
    }

    // ── Accueil vivant ────────────────────────────────────────────────────────

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, homeError = null) }

            if (!networkChecker.isNetworkAvailable()) {
                _uiState.update { it.copy(isOffline = true, isLoading = false) }
                return@launch
            }

            try {
                val home = libraryApi.fetchHome()
                _uiState.update {
                    it.copy(home = home, isOffline = false, isLoading = false, homeError = null)
                }
            } catch (e: Exception) {
                // Règle produit n° 1 : ne jamais affirmer une absence non vérifiée.
                // Un 500, une expiration de délai ou une réponse illisible ne sont
                // PAS « hors-ligne » — l'annoncer ainsi était un mensonge d'interface,
                // et l'exception disparaissait sans jamais remonter à Crashlytics.
                recordException(e, context = "LibraryViewModel.loadHome")
                val offline = !networkChecker.isNetworkAvailable()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOffline = offline,
                        homeError = if (offline) null else UiResult.Error(offline = false, retry = ::loadHome)
                    )
                }
            }
        }
    }

    private fun observeLocalDocuments() {
        viewModelScope.launch {
            repository.getLawCodes().collect { codes ->
                val sorted = codes.sortedByDescending {
                    parseRemoteDateToEpochMillis(it.dateSignature) ?: it.lastUpdated
                }
                _uiState.update { it.copy(localCodes = sorted) }
            }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                val types = repository.getDocumentTypes()
                _uiState.update { it.copy(documentTypes = types) }
            } catch (e: Exception) {
                // silencieux : les filtres types restent vides
            }
        }
        viewModelScope.launch {
            try {
                val institutions = legalApi.fetchInstitutions().data ?: emptyList()
                _uiState.update { it.copy(institutions = institutions) }
            } catch (e: Exception) {
                // silencieux : le filtre institution reste vide
            }
        }
    }

    // ── Saisie & autocomplétion ───────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        suggestJob?.cancel()
        if (query.trim().length < 2 || !networkChecker.isNetworkAvailable()) {
            _uiState.update { it.copy(suggestions = null) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(250) // debounce de frappe
            try {
                val suggestions = libraryApi.suggest(query.trim())
                _uiState.update { it.copy(
                    suggestions = if (suggestions.isEmpty) null else suggestions
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(suggestions = null) }
            }
        }
    }

    fun dismissSuggestions() {
        suggestJob?.cancel()
        _uiState.update { it.copy(suggestions = null) }
    }

    // ── Recherche ─────────────────────────────────────────────────────────────

    fun submitSearch(query: String = _uiState.value.searchQuery) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        dismissSuggestions()
        _uiState.update { it.copy(searchQuery = trimmed, submittedQuery = trimmed) }
        runSearch(page = 1)
    }

    fun clearSearch() {
        searchJob?.cancel()
        dismissSuggestions()
        _uiState.update { it.copy(
            searchQuery = "",
            submittedQuery = "",
            results = emptyList(),
            pagination = null,
            searchError = null
        ) }
    }

    fun loadNextPage() {
        val state = _uiState.value
        val pagination = state.pagination ?: return
        if (state.isLoadingMore || pagination.current_page >= pagination.last_page) return
        runSearch(page = pagination.current_page + 1, append = true)
    }

    private fun runSearch(page: Int, append: Boolean = false) {
        val state = _uiState.value
        val query = state.submittedQuery
        if (query.length < 2) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(
                isSearching = !append,
                isLoadingMore = append,
                searchError = null
            ) }

            if (networkChecker.isNetworkAvailable()) {
                try {
                    val response = libraryApi.search(
                        query = query,
                        typeCode = state.selectedTypeCode,
                        legalScope = state.scope.apiValue,
                        institutionId = state.selectedInstitutionId,
                        sort = state.sort.apiValue,
                        page = page
                    )
                    _uiState.update { it.copy(
                        results = if (append) it.results + response.data else response.data,
                        pagination = response.pagination,
                        resultsFromNetwork = true,
                        isSearching = false,
                        isLoadingMore = false
                    ) }
                    if (!append) logSearchPerformed(query, response.data.size, offline = false)
                    return@launch
                } catch (e: Exception) {
                    // L'API a échoué : on tente le repli local avant d'afficher une erreur.
                }
            }

            searchLocallyAsFallback(query, append)
        }
    }

    /** Recherche FTS locale (documents téléchargés) quand le réseau manque ou a échoué. */
    private suspend fun searchLocallyAsFallback(query: String, append: Boolean) {
        try {
            when (val local = repository.searchHybrid(query = query)) {
                is SearchResult.Success -> {
                    val items = local.articles.map { it.toLibrarySearchItem() }
                    _uiState.update { it.copy(
                        results = if (append) it.results + items else items,
                        pagination = null,
                        resultsFromNetwork = false,
                        isSearching = false,
                        isLoadingMore = false,
                        searchError = null
                    ) }
                    if (!append) logSearchPerformed(query, items.size, offline = true)
                }

                is SearchResult.Error -> {
                    // Panne API : ne jamais jeter les résultats locaux de repli
                    // déjà calculés — un « aucun résultat » serait un faux
                    // négatif juridique (règle produit non négociable).
                    val items = local.fallbackArticles.map { it.toLibrarySearchItem() }
                    _uiState.update { it.copy(
                        results = if (append) it.results + items else items,
                        pagination = null,
                        resultsFromNetwork = false,
                        isSearching = false,
                        isLoadingMore = false,
                        searchError = UiResult.Error(
                            offline = !networkChecker.isNetworkAvailable(),
                            retry = { runSearch(page = 1) }
                        )
                    ) }
                    if (!append) logSearchFailed(query)
                }

                SearchResult.Loading -> Unit // Variant jamais produit par searchHybrid.
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(
                isSearching = false,
                isLoadingMore = false,
                searchError = UiResult.Error(
                    offline = !networkChecker.isNetworkAvailable(),
                    retry = { runSearch(page = 1) }
                )
            ) }
            if (!append) logSearchFailed(query)
        }
    }

    /** Jamais le texte de la requête : longueur et compteurs only. */
    private fun logSearchPerformed(query: String, resultCount: Int, offline: Boolean) {
        analytics.logEvent(
            AnalyticsEvents.SEARCH_PERFORMED,
            mapOf(
                "query_length" to query.length,
                "result_count" to resultCount,
                "source" to if (offline) "offline" else "online"
            )
        )
    }

    private fun logSearchFailed(query: String) {
        analytics.logEvent(
            AnalyticsEvents.SEARCH_FAILED,
            mapOf(
                "query_length" to query.length,
                "offline" to !networkChecker.isNetworkAvailable()
            )
        )
    }

    private fun ArticleSpec.toLibrarySearchItem(): LibrarySearchItem = LibrarySearchItem(
        id = id,
        number = number,
        content = content,
        document_id = codeId,
        document_title = breadcrumb.substringBefore(">").trim().ifBlank { title },
        document_type = typeCode,
        breadcrumb = breadcrumb
    )

    // ── Filtres (chaque changement relance la recherche en cours) ────────────

    fun updateScope(scope: LibraryScope) {
        _uiState.update { it.copy(scope = scope) }
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun updateTypeFilter(typeCode: String?) {
        _uiState.update { it.copy(selectedTypeCode = typeCode) }
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun updateInstitutionFilter(institutionId: String?) {
        _uiState.update { it.copy(selectedInstitutionId = institutionId) }
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun updateSort(sort: LibrarySort) {
        _uiState.update { it.copy(sort = sort) }
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun resetFilters() {
        _uiState.update { it.copy(
            scope = LibraryScope.ALL,
            selectedTypeCode = null,
            selectedInstitutionId = null,
            sort = LibrarySort.RELEVANCE
        ) }
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    // ── Téléchargements hors-ligne ────────────────────────────────────────────

    fun downloadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                downloadingIds = it.downloadingIds + documentId
            ) }
            try {
                repository.downloadDocument(documentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Erreur lors du téléchargement : ${e.message}"
                ) }
            } finally {
                _uiState.update { it.copy(
                    downloadingIds = it.downloadingIds - documentId
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
