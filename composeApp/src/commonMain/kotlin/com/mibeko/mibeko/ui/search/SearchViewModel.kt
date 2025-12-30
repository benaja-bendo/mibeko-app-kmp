package com.mibeko.mibeko.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.preferences.SearchHistoryManager
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: LocalLegalRepository,
    private val searchHistoryManager: SearchHistoryManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    
    private val _filter = MutableStateFlow("Tout")
    val filter: StateFlow<String> = _filter
    
    private val _liveQuery = MutableStateFlow("")
    
    /**
     * Recent search history from local storage.
     */
    val recentSearches: StateFlow<List<String>> = searchHistoryManager.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Live suggestions based on current input.
     * Matches article numbers like "Art 45" → "Article 45 Code du Travail".
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<String>> = _liveQuery
        .flatMapLatest { query ->
            if (query.length >= 2) {
                repository.getAutocompleteSuggestions(query)
            } else {
                MutableStateFlow(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Search results filtered by current filter.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ArticleSpec>> = combine(_query, _filter) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        if (query.isEmpty()) {
            MutableStateFlow(emptyList())
        } else {
            repository.search(query).map { results ->
                when (filter) {
                    "Codes" -> results.filter { it.title.contains("Code", ignoreCase = true) }
                    "Lois" -> results.filter { it.title.contains("Loi", ignoreCase = true) }
                    else -> results
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _query.value = query
    }
    
    fun updateFilter(filter: String) {
        _filter.value = filter
    }
    
    fun updateLiveQuery(query: String) {
        _liveQuery.value = query
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
