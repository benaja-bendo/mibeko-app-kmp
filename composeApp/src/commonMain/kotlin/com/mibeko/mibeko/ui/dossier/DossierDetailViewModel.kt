package com.mibeko.mibeko.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.dao.DossierArticleWithDetails
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DossierFilterType {
    ALL, DOCUMENTS, ARTICLES
}

data class ClientDossierDocument(
    val id: String,
    val title: String,
    val articleCount: Int
)

class DossierDetailViewModel(
    private val dossierId: String,
    private val repository: DossierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DossierDetailUiState())
    val uiState: StateFlow<DossierDetailUiState> = _uiState.asStateFlow()

    private val _showNoteDialog = MutableStateFlow<DossierArticleWithDetails?>(null)
    val showNoteDialog: StateFlow<DossierArticleWithDetails?> = _showNoteDialog.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()
    
    // For specific document filtering
    private val _selectedDocumentId = MutableStateFlow<String?>(null)

    init {
        loadDossier()
        loadArticles()
    }
    
    fun setFilter(filter: DossierFilterType) {
        _uiState.update { it.copy(filter = filter) }
        if (filter != DossierFilterType.DOCUMENTS) {
            _selectedDocumentId.value = null
        }
    }
    
    fun verifyFilter() {
        // If we are viewing articles of a specific document, ensure we go back to Documents view when clearing
        _selectedDocumentId.value = null
    }
    
    fun selectDocument(documentId: String) {
        _selectedDocumentId.value = documentId
    }

    private fun loadDossier() {
        viewModelScope.launch {
            repository.getDossierById(dossierId)
                .combine(repository.getDossierArticleCount(dossierId)) { dossier, count ->
                    dossier to count
                }
                .collect { (dossier, count) ->
                    _uiState.update { 
                        it.copy(dossier = dossier, articleCount = count) 
                    }
                }
        }
    }

    private fun loadArticles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Using combine to react to document selection changes
            combine(
                repository.getDossierArticles(dossierId),
                _selectedDocumentId
            ) { articles, selectedDocId ->
                val docs = articles
                    .groupBy { it.document_id }
                    .map { (id, list) ->
                        ClientDossierDocument(
                            id = id,
                            title = list.first().document_title,
                            articleCount = list.size
                        )
                    }
                
                val filteredArticles = if (selectedDocId != null) {
                    articles.filter { it.document_id == selectedDocId }
                } else {
                    articles
                }
                
                Triple(articles, docs, filteredArticles)
            }
            .catch { e ->
                _uiState.update { 
                    it.copy(isLoading = false, error = e.message) 
                }
            }
            .collect { (allArticles, docs, filteredArticles) ->
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        articles = allArticles, // Keep all for "Articles" tab
                        documents = docs,
                        displayedArticles = filteredArticles, // For when a document is selected
                        error = null
                    ) 
                }
            }
        }
    }

    fun showNoteDialog(article: DossierArticleWithDetails) {
        _showNoteDialog.value = article
    }

    fun dismissNoteDialog() {
        _showNoteDialog.value = null
    }

    fun updateNote(articleId: String, note: String?) {
        viewModelScope.launch {
            repository.updatePersonalNote(dossierId, articleId, note)
            _showNoteDialog.value = null
        }
    }

    fun removeArticle(articleId: String) {
        viewModelScope.launch {
            repository.removeArticleFromDossier(dossierId, articleId)
        }
    }

    fun showEditDialog() {
        _showEditDialog.value = true
    }

    fun dismissEditDialog() {
        _showEditDialog.value = false
    }

    fun updateDossier(
        name: String,
        legalDomain: String,
        tag: DossierTag,
        description: String?,
        color: String
    ) {
        viewModelScope.launch {
            repository.updateDossier(dossierId, name, legalDomain, tag, description, color)
            _showEditDialog.value = false
        }
    }

    fun exportPdf(onSuccess: (ByteArray) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val bytes = repository.exportDossierPdf(dossierId)
                onSuccess(bytes)
            } catch (e: Exception) {
                onError(e.message ?: "Erreur lors de l'export")
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }
}

data class DossierDetailUiState(
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val dossier: DossierEntity? = null,
    val articles: List<DossierArticleWithDetails> = emptyList(),
    val documents: List<ClientDossierDocument> = emptyList(),
    val displayedArticles: List<DossierArticleWithDetails> = emptyList(),
    val articleCount: Int = 0,
    val filter: DossierFilterType = DossierFilterType.ALL,
    val error: String? = null
)
