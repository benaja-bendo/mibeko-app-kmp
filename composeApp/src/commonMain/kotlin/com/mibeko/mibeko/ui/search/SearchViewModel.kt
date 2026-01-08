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
import kotlinx.coroutines.launch

/**
 * UI State for the search screen.
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<ArticleSpec> = emptyList(),
    val errorMessage: String? = null,
    val isFromNetwork: Boolean = false,
    val currentFilter: String = "Tout"
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
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
                        isFromNetwork = result.isFromNetwork,
                        currentFilter = _uiState.value.currentFilter
                    )
                }
                is SearchResult.Error -> {
                    allSearchResults = result.fallbackArticles
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
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
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
                        isFromNetwork = result.isFromNetwork,
                        currentFilter = _uiState.value.currentFilter
                    )
                }
                is SearchResult.Error -> {
                    allSearchResults = result.fallbackArticles
                    val filteredResults = applyFilter(allSearchResults, _uiState.value.currentFilter)
                    _uiState.value = SearchUiState(
                        isLoading = false,
                        results = filteredResults,
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
     * Apply filter to search results.
     */
    private fun applyFilter(results: List<ArticleSpec>, filter: String): List<ArticleSpec> {
        return when (filter) {
            "Codes" -> results.filter { it.title.contains("Code", ignoreCase = true) }
            "Lois" -> results.filter { it.title.contains("Loi", ignoreCase = true) }
            "Downloaded" -> results.filter { it.isDownloaded }
            else -> results
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
