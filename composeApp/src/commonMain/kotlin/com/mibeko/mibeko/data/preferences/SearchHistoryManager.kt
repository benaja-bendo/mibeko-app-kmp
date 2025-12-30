package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages search history for recent queries and suggestions.
 * Persists up to 10 recent searches locally using multiplatform-settings.
 */
class SearchHistoryManager(private val settings: Settings = Settings()) {
    
    companion object {
        private const val SEARCH_HISTORY_KEY = "search_history"
        private const val MAX_HISTORY_SIZE = 10
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _recentSearches = MutableStateFlow(loadHistory())
    
    /**
     * Get recent search queries as a flow.
     */
    val recentSearches: Flow<List<String>> = _recentSearches.asStateFlow()
    
    private fun loadHistory(): List<String> {
        val historyJson = settings.getString(SEARCH_HISTORY_KEY, "[]")
        return try {
            json.decodeFromString<List<String>>(historyJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveHistory(history: List<String>) {
        settings.putString(SEARCH_HISTORY_KEY, json.encodeToString(history))
        _recentSearches.value = history
    }
    
    /**
     * Add a search query to history.
     * Moves existing query to front. Trims to MAX_HISTORY_SIZE.
     */
    fun addSearch(query: String) {
        if (query.isBlank()) return
        
        val currentHistory = _recentSearches.value.toMutableList()
        
        // Remove if exists, add to front
        currentHistory.remove(query)
        currentHistory.add(0, query.trim())
        
        // Trim to max size
        val trimmedHistory = currentHistory.take(MAX_HISTORY_SIZE)
        saveHistory(trimmedHistory)
    }
    
    /**
     * Remove a specific query from history.
     */
    fun removeSearch(query: String) {
        val currentHistory = _recentSearches.value.toMutableList()
        currentHistory.remove(query)
        saveHistory(currentHistory)
    }
    
    /**
     * Clear all search history.
     */
    fun clearHistory() {
        saveHistory(emptyList())
    }
}
