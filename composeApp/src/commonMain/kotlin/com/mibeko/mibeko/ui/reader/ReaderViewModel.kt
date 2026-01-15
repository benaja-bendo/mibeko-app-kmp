package com.mibeko.mibeko.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.asStateFlow

data class ReaderUiState(
    val isLoading: Boolean = true,
    val article: ArticleSpec? = null,
    val error: String? = null
)

class ReaderViewModel(
    private val repository: LocalLegalRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // Keep for backward compatibility with ReaderScreen
    private val _article = MutableStateFlow<ArticleSpec?>(null)
    val article: StateFlow<ArticleSpec?> = _article

    private val _textSize = MutableStateFlow(userPreferencesRepository.getTextSize())
    val textSize: StateFlow<UserPreferencesRepository.TextSize> = _textSize.asStateFlow()

    private val _isDyslexiaFontEnabled = MutableStateFlow(userPreferencesRepository.isDyslexiaFontEnabled())
    val isDyslexiaFontEnabled: StateFlow<Boolean> = _isDyslexiaFontEnabled.asStateFlow()

    fun toggleOffline() {
        val currentArticle = _uiState.value.article ?: return
        viewModelScope.launch {
            try {
                repository.toggleArticleOffline(currentArticle, !currentArticle.isDownloaded)
                // Reload article to update UI state
                loadArticle(currentArticle.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur: ${e.message}")
            }
        }
    }

    fun loadArticle(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val articleResult = repository.getArticleById(id).firstOrNull()
                _article.value = articleResult
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    article = articleResult,
                    error = if (articleResult == null) "Article non trouvé. Veuillez télécharger ce document." else null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement: ${e.message}"
                )
            }
        }
    }

    fun refreshPreferences() {
        _textSize.value = userPreferencesRepository.getTextSize()
        _isDyslexiaFontEnabled.value = userPreferencesRepository.isDyslexiaFontEnabled()
    }

    fun setTextSize(size: UserPreferencesRepository.TextSize) {
        userPreferencesRepository.setTextSize(size)
        _textSize.value = size
    }

    fun setDyslexiaFontEnabled(enabled: Boolean) {
        userPreferencesRepository.setDyslexiaFontEnabled(enabled)
        _isDyslexiaFontEnabled.value = enabled
    }
}
