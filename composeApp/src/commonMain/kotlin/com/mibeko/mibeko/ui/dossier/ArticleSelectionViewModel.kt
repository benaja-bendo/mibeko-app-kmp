package com.mibeko.mibeko.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.LibraryApiService
import com.mibeko.mibeko.data.remote.LibrarySearchItem
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.UiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleSelectionUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<LibrarySearchItem> = emptyList(),
    val addedArticleIds: Set<String> = emptySet(),
    val error: UiResult.Error? = null
)

/** Recherche + ajout d'un article à un dossier, depuis l'écran du dossier lui-même. */
class ArticleSelectionViewModel(
    private val dossierRepository: DossierRepository,
    private val libraryApi: LibraryApiService,
    private val networkChecker: NetworkConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleSelectionUiState())
    val uiState: StateFlow<ArticleSelectionUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var dossierId: String? = null

    fun start(dossierId: String) {
        this.dossierId = dossierId
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        runSearch(query)
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false, error = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce de frappe
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val response = libraryApi.search(query = trimmed, perPage = 15)
                _uiState.update { it.copy(results = response.data, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        error = UiResult.Error(
                            offline = !networkChecker.isNetworkAvailable(),
                            retry = { runSearch(trimmed) }
                        )
                    )
                }
            }
        }
    }

    fun addArticle(articleId: String) {
        val id = dossierId ?: return
        viewModelScope.launch {
            dossierRepository.addArticleToDossier(id, articleId)
            dossierRepository.syncNow()
            _uiState.update { it.copy(addedArticleIds = it.addedArticleIds + articleId) }
        }
    }
}
