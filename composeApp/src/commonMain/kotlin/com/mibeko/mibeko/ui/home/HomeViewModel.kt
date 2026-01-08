package com.mibeko.mibeko.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents a recently viewed item for the home screen.
 */
data class RecentItem(
    val id: String,
    val title: String
)

class HomeViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    val lawCodes: StateFlow<List<LawCodeSpec>> = repository.getLawCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    // Recently viewed items for the home screen
    private val _recentItems = MutableStateFlow<List<RecentItem>>(emptyList())
    val recentItems: StateFlow<List<RecentItem>> = _recentItems
    
    // User requested error handling
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        // Load recent items on init (placeholder - would come from local DB in real impl)
        loadRecentItems()
    }
    
    private fun loadRecentItems() {
        // For now, this is a placeholder. In a real implementation,
        // this would query recent views from local storage.
        // The UI will show demo items if this is empty.
    }

    fun clearError() {
        _error.value = null
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null
            try {
                repository.sync()
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Erreur de synchronisation: ${e.message ?: "Inconnue"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

