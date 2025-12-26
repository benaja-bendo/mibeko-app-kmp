package com.mibeko.mibeko.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    private val _article = MutableStateFlow<ArticleSpec?>(null)
    val article: StateFlow<ArticleSpec?> = _article

    fun loadArticle(id: String) {
        viewModelScope.launch {
            repository.getArticleById(id).collect { 
                _article.value = it
            }
        }
    }
}
