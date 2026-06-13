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
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.SearchResult
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.parseRemoteDateToEpochMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isOffline: Boolean = false,
    // ── Recherche serveur (granularité article, comme le web) ────────────
    val searchQuery: String = "",
    val submittedQuery: String = "",
    val isSearching: Boolean = false,
    val results: List<LibrarySearchItem> = emptyList(),
    val pagination: LibraryPagination? = null,
    val isLoadingMore: Boolean = false,
    /** false quand les résultats viennent de la base locale (mode hors-ligne). */
    val resultsFromNetwork: Boolean = true,
    val searchError: String? = null,
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
    private val networkChecker: NetworkConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val recentItems: StateFlow<List<RecentlyViewedItem>> = recentlyViewedManager.recentItems

    private var suggestJob: Job? = null
    private var searchJob: Job? = null

    init {
        observeLocalDocuments()
        loadHome()
        loadFilterOptions()
    }

    fun refresh() {
        loadHome()
        loadFilterOptions()
    }

    // ── Accueil vivant ────────────────────────────────────────────────────────

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val home = libraryApi.fetchHome()
                _uiState.value = _uiState.value.copy(home = home, isOffline = false, isLoading = false)
            } catch (e: Exception) {
                // Pas de réseau (ou API indisponible) : l'accueil retombe sur la base locale.
                _uiState.value = _uiState.value.copy(isOffline = true, isLoading = false)
            }
        }
    }

    private fun observeLocalDocuments() {
        viewModelScope.launch {
            repository.getLawCodes().collect { codes ->
                val sorted = codes.sortedByDescending {
                    parseRemoteDateToEpochMillis(it.dateSignature) ?: it.lastUpdated
                }
                _uiState.value = _uiState.value.copy(localCodes = sorted)
            }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                val types = repository.getDocumentTypes()
                _uiState.value = _uiState.value.copy(documentTypes = types)
            } catch (e: Exception) {
                // silencieux : les filtres types restent vides
            }
        }
        viewModelScope.launch {
            try {
                val institutions = legalApi.fetchInstitutions().data ?: emptyList()
                _uiState.value = _uiState.value.copy(institutions = institutions)
            } catch (e: Exception) {
                // silencieux : le filtre institution reste vide
            }
        }
    }

    // ── Saisie & autocomplétion ───────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        suggestJob?.cancel()
        if (query.trim().length < 2 || !networkChecker.isNetworkAvailable()) {
            _uiState.value = _uiState.value.copy(suggestions = null)
            return
        }
        suggestJob = viewModelScope.launch {
            delay(250) // debounce de frappe
            try {
                val suggestions = libraryApi.suggest(query.trim())
                _uiState.value = _uiState.value.copy(
                    suggestions = if (suggestions.isEmpty) null else suggestions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(suggestions = null)
            }
        }
    }

    fun dismissSuggestions() {
        suggestJob?.cancel()
        _uiState.value = _uiState.value.copy(suggestions = null)
    }

    // ── Recherche ─────────────────────────────────────────────────────────────

    fun submitSearch(query: String = _uiState.value.searchQuery) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        dismissSuggestions()
        _uiState.value = _uiState.value.copy(searchQuery = trimmed, submittedQuery = trimmed)
        runSearch(page = 1)
    }

    fun clearSearch() {
        searchJob?.cancel()
        dismissSuggestions()
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            submittedQuery = "",
            results = emptyList(),
            pagination = null,
            searchError = null
        )
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
            _uiState.value = _uiState.value.copy(
                isSearching = !append,
                isLoadingMore = append,
                searchError = null
            )

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
                    _uiState.value = _uiState.value.copy(
                        results = if (append) _uiState.value.results + response.data else response.data,
                        pagination = response.pagination,
                        resultsFromNetwork = true,
                        isSearching = false,
                        isLoadingMore = false
                    )
                    return@launch
                } catch (e: Exception) {
                    // L'API a échoué : on tente le repli local avant d'afficher une erreur.
                }
            }

            searchLocallyAsFallback(query, append)
        }
    }

    /** Recherche FTS locale (documents téléchargés) quand le réseau manque. */
    private suspend fun searchLocallyAsFallback(query: String, append: Boolean) {
        try {
            val local = repository.searchHybrid(query = query)
            val items = when (local) {
                is SearchResult.Success -> local.articles.map { article ->
                    LibrarySearchItem(
                        id = article.id,
                        number = article.number,
                        content = article.content,
                        document_id = article.codeId,
                        document_title = article.breadcrumb.substringBefore(">").trim()
                            .ifBlank { article.title },
                        document_type = article.typeCode,
                        breadcrumb = article.breadcrumb
                    )
                }

                else -> emptyList()
            }
            _uiState.value = _uiState.value.copy(
                results = if (append) _uiState.value.results + items else items,
                pagination = null,
                resultsFromNetwork = false,
                isSearching = false,
                isLoadingMore = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                isLoadingMore = false,
                searchError = "La recherche a échoué. Vérifiez votre connexion."
            )
        }
    }

    // ── Filtres (chaque changement relance la recherche en cours) ────────────

    fun updateScope(scope: LibraryScope) {
        _uiState.value = _uiState.value.copy(scope = scope)
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun updateTypeFilter(typeCode: String?) {
        _uiState.value = _uiState.value.copy(selectedTypeCode = typeCode)
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun updateInstitutionFilter(institutionId: String?) {
        _uiState.value = _uiState.value.copy(selectedInstitutionId = institutionId)
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun updateSort(sort: LibrarySort) {
        _uiState.value = _uiState.value.copy(sort = sort)
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            scope = LibraryScope.ALL,
            selectedTypeCode = null,
            selectedInstitutionId = null,
            sort = LibrarySort.RELEVANCE
        )
        if (_uiState.value.hasSearched) runSearch(page = 1)
    }

    // ── Téléchargements hors-ligne ────────────────────────────────────────────

    fun downloadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadingIds = _uiState.value.downloadingIds + documentId
            )
            try {
                repository.downloadDocument(documentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erreur lors du téléchargement : ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    downloadingIds = _uiState.value.downloadingIds - documentId
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
