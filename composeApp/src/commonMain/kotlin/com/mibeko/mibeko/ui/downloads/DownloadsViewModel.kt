package com.mibeko.mibeko.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val isLoading: Boolean = false,
    val documents: List<LawCodeSpec> = emptyList(),
    val offlineArticles: List<ArticleSpec> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val error: String? = null
)

class DownloadsViewModel(
    private val repository: LocalLegalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Load all documents and articles and their download status.
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                repository.getDownloadedDocuments(),
                repository.getOfflineArticles()
            ) { codes, articles ->
                Pair(codes, articles)
            }.collect { (codes, articles) ->
                _uiState.update { it.copy(
                    documents = codes,
                    offlineArticles = articles,
                    isLoading = false
                ) }
            }
        }
    }

    /**
     * Start downloading a document for offline use.
     */
    fun downloadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                downloadingIds = it.downloadingIds + documentId
            ) }
            try {
                repository.downloadDocument(documentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Erreur lors du téléchargement : ${e.message}"
                ) }
            } finally {
                _uiState.update { it.copy(
                    downloadingIds = it.downloadingIds - documentId
                ) }
            }
        }
    }

    /**
     * Remove locally downloaded data for a document.
     */
    fun removeDownload(documentId: String) {
        viewModelScope.launch {
            try {
                repository.removeDownload(documentId)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Erreur lors de la suppression : ${e.message}"
                ) }
            }
        }
    }

    /**
     * Remove an individual article from offline storage.
     */
    fun removeArticleDownload(articleId: String) {
        viewModelScope.launch {
            try {
                repository.removeArticleDownload(articleId)
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Erreur lors de la suppression : ${e.message}"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
