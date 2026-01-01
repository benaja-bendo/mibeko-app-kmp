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

class HomeViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    val lawCodes: StateFlow<List<LawCodeSpec>> = repository.getLawCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    // User requested error handling
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
