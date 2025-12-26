package com.mibeko.mibeko.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

class SearchViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ArticleSpec>> = _query
        .flatMapLatest { query ->
            if (query.isEmpty()) MutableStateFlow(emptyList())
            else repository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _query.value = query
    }
}
