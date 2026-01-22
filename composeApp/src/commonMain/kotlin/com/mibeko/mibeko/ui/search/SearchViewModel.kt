package com.mibeko.mibeko.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.preferences.SearchHistoryManager
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.SearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * UI State for the search screen.
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<ArticleSpec> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val aiAnswer: String? = null,
    val errorMessage: String? = null,
    val isFromNetwork: Boolean = false,
    val currentFilter: String = "Tout",
    val documentTypes: List<com.mibeko.mibeko.data.remote.RemoteDocumentType> = emptyList()
)

class SearchViewModel(
    private val repository: LocalLegalRepository,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private val _suggestions = MutableStateFlow<List<com.mibeko.mibeko.data.ArticleSuggestion>>(emptyList())
    val suggestions: StateFlow<List<com.mibeko.mibeko.data.ArticleSuggestion>> = _suggestions.asStateFlow()
    
    private var allSearchResults: List<ArticleSpec> = emptyList()
    private var currentTag: String? = null
    
    init {
        fetchDocumentTypes()
    }

    private fun fetchDocumentTypes() {
        viewModelScope.launch {
            val types = repository.getDocumentTypes()
            _uiState.value = _uiState.value.copy(documentTypes = types)
        }
    }
    
    /**
     * Recent search history from local storage.
     */
    val recentSearches: StateFlow<List<String>> = searchHistoryManager.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Update the search query and trigger hybrid search.
     */
    fun updateQuery(query: String) {
        _query.value = query
        if (query.length >= 2) {
            performSearch(query)
        } else {
            _uiState.value = SearchUiState()
        }
    }
    
    /**
     * Update filter and reapply to current results.
     */
    fun updateFilter(filter: String) {
        val filteredResults = applyFilter(allSearchResults, filter)
        _uiState.value = _uiState.value.copy(
            currentFilter = filter,
            results = filteredResults
        )
    }
    
    /**
     * Update live query for autocomplete suggestions.
     * Uses hybrid autocomplete - API if online, local if offline.
     */
    fun updateLiveQuery(query: String) {
        if (query.length >= 2) {
            viewModelScope.launch {
                val suggestions = repository.getAutocompleteSuggestionsHybrid(query)
                _suggestions.value = suggestions
            }
        } else {
            _suggestions.value = emptyList()
        }
    }
    
    fun performSearch(query: String) {
        currentTag = null
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.searchHybrid(query = query)) {
                is SearchResult.Success -> {
                    allSearchResults = result.articles
                    val counts = calculateCounts(allSearchResults)
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
                        counts = counts,
                        aiAnswer = result.aiAnswer,
                        isFromNetwork = result.isFromNetwork,
                        currentFilter = _uiState.value.currentFilter
                    )
                }
                is SearchResult.Error -> {
                    allSearchResults = result.fallbackArticles
                    val counts = calculateCounts(allSearchResults)
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
                        counts = counts,
                        errorMessage = result.message,
                        isFromNetwork = false,
                        currentFilter = _uiState.value.currentFilter
                    )
                }
                is SearchResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun performSearchByTag(tag: String) {
        currentTag = tag
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.searchHybrid(tag = tag)) {
                is SearchResult.Success -> {
                    allSearchResults = result.articles
                    val counts = calculateCounts(allSearchResults)
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
                        counts = counts,
                        aiAnswer = result.aiAnswer,
                        isFromNetwork = result.isFromNetwork,
                        currentFilter = _uiState.value.currentFilter
                    )
                }
                is SearchResult.Error -> {
                    allSearchResults = result.fallbackArticles
                    val counts = calculateCounts(allSearchResults)
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
                        counts = counts,
                        errorMessage = result.message,
                        isFromNetwork = false,
                        currentFilter = _uiState.value.currentFilter
                    )
                }
                is SearchResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    /**
     * Toggle favorite status for an article.
     */
    fun toggleFavorite(articleId: String) {
        viewModelScope.launch {
            repository.getArticleById(articleId).first()?.let { article ->
                val newFavoriteStatus = !article.isFavorite
                // We need a method in repository to toggle favorite
                // Looking at LocalLegalRepository, it doesn't have a direct toggleFavorite
                // But it has mibekoDao. Let's assume we use a direct DAO call or add to repository.
                // For simplicity, let's update the local state first to be reactive
                allSearchResults = allSearchResults.map {
                    if (it.id == articleId) it.copy(isFavorite = newFavoriteStatus) else it
                }
                val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                _uiState.value = _uiState.value.copy(results = filteredResults)
                
                // Actual update in DB
                repository.updateArticleFavoriteStatus(articleId, newFavoriteStatus)
            }
        }
    }
    
    private fun calculateCounts(results: List<ArticleSpec>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        counts["Tout"] = results.size
        
        // Count by typeCode
        results.forEach { article ->
            if (article.typeCode.isNotEmpty()) {
                counts[article.typeCode] = (counts[article.typeCode] ?: 0) + 1
            }
        }
        
        return counts
    }
    
    /**
     * Apply filter to search results.
     */
    private fun applyFilter(results: List<ArticleSpec>, filter: String): List<ArticleSpec> {
        return when (filter) {
            "Tout" -> results
            "Downloaded" -> results.filter { it.isDownloaded }
            else -> results.filter { it.typeCode == filter }
        }
    }
    
    /**
     * Retry search after error.
     */
    fun retrySearch() {
        if (currentTag != null) {
            performSearchByTag(currentTag!!)
        } else {
            val currentQuery = _query.value
            if (currentQuery.isNotEmpty()) {
                performSearch(currentQuery)
            }
        }
    }
    
    /**
     * Save a search query to history.
     */
    fun saveSearch(query: String) {
        viewModelScope.launch {
            searchHistoryManager.addSearch(query)
        }
    }
    
    /**
     * Remove a query from search history.
     */
    fun removeFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryManager.removeSearch(query)
        }
    }
    
    /**
     * Clear all search history.
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryManager.clearHistory()
        }
    }
}
