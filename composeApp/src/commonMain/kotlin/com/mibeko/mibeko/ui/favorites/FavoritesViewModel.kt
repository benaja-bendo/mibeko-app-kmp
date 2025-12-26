package com.mibeko.mibeko.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    val favoriteArticles: StateFlow<List<ArticleSpec>> = repository.getFavoriteArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
