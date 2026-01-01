package com.mibeko.mibeko.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.dao.DossierArticleWithDetails
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    init {
        loadDossier()
        loadArticles()
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
            repository.getDossierArticles(dossierId)
                .catch { e ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = e.message) 
                    }
                }
                .collect { articles ->
                    _uiState.update { 
                        it.copy(isLoading = false, articles = articles, error = null) 
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

    fun generateTextExport(): String {
        var result = ""
        viewModelScope.launch {
            result = repository.generateTextExport(dossierId)
        }
        return result
    }
}

data class DossierDetailUiState(
    val isLoading: Boolean = false,
    val dossier: DossierEntity? = null,
    val articles: List<DossierArticleWithDetails> = emptyList(),
    val articleCount: Int = 0,
    val error: String? = null
)
