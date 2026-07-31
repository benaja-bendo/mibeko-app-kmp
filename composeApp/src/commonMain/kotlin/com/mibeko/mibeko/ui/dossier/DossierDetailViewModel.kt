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
    private val repository: DossierRepository,
    private val contentSharer: com.mibeko.mibeko.util.ContentSharer
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
                .collect { allArticles ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            articles = allArticles,
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
            repository.syncNow()
        }
    }

    fun removeArticle(articleId: String) {
        viewModelScope.launch {
            repository.removeArticleFromDossier(dossierId, articleId)
            repository.syncNow()
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
            repository.syncNow()
        }
    }

    fun shareDossier() {
        viewModelScope.launch {
            // État dédié : passer isLoading à true remplacerait toute la liste
            // des articles par un spinner plein écran pendant l'export.
            _uiState.update { it.copy(isExporting = true) }
            try {
                val bytes = repository.exportDossierPdf(dossierId)
                
                // Sanitize filename: remove special chars and spaces
                val rawName = _uiState.value.dossier?.name ?: "Dossier"
                val sanitizedName = rawName.replace(Regex("[^a-zA-Z0-9]"), "_")
                    .take(30) // Limit length
                val fileName = "$sanitizedName.pdf"
                
                contentSharer.shareFile(bytes, fileName, "application/pdf")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(error = "Erreur lors de l'export: ${e.message}") }
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
    val articleCount: Int = 0,
    val error: String? = null
)
