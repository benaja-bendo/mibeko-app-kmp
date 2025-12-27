package com.mibeko.mibeko.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi

class SearchViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow("Tout")
    val filter: StateFlow<String> = _filter
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ArticleSpec>> = kotlinx.coroutines.flow.combine(_query, _filter) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        if (query.isEmpty()) MutableStateFlow(emptyList())
        else repository.search(query).flatMapLatest { results ->
            // In-memory filtering
            val filtered = when (filter) {
                "Codes" -> results.filter { it.title.contains("Code", ignoreCase = true) }
                "Lois" -> results.filter { it.title.contains("Loi", ignoreCase = true) }
                else -> results
            }
            MutableStateFlow(filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _query.value = query
    }
    
    fun updateFilter(filter: String) {
        _filter.value = filter
    }
}
