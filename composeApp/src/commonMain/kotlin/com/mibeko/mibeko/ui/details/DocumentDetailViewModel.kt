package com.mibeko.mibeko.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.LawCodeSpec
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * État UI pour l'écran de détail d'un document.
 */
data class DocumentDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val document: LawCodeSpec? = null,
    val structure: Map<NodeEntity, List<ArticleEntity>> = emptyMap(),
    val filteredStructure: Map<NodeEntity, List<ArticleEntity>> = emptyMap(),
    val searchQuery: String = "",
    val isDownloading: Boolean = false
)

class DocumentDetailViewModel(
    private val repository: LocalLegalRepository,
    private val contentSharer: com.mibeko.mibeko.util.ContentSharer
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    // Keep legacy flows for backward compatibility if needed, but prefer uiState
    private val _structure = MutableStateFlow<Map<NodeEntity, List<ArticleEntity>>>(emptyMap())
    val structure: StateFlow<Map<NodeEntity, List<ArticleEntity>>> = _structure

    private val _document = MutableStateFlow<LawCodeSpec?>(null)
    val document: StateFlow<LawCodeSpec?> = _document.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    /**
     * Charge la structure du document.
     * Si le document n'est pas présent localement, tente de le récupérer via l'API.
     */
    fun loadStructure(documentId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            // Launch remote fetch in background if needed
            launch {
                try {
                    val localData = repository.getStructure(documentId).first()
                    val localDoc = repository.getLawCodes().first().find { it.id == documentId }
                    
                    if (localData.isEmpty() || localDoc == null) {
                        if (localDoc == null) {
                            repository.fetchAndStoreDocument(documentId)
                        }
                        repository.fetchAndStoreDocumentStructure(documentId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Only show error if we have no data at all
                    if (_uiState.value.structure.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            error = "Impossible de charger le document. Vérifiez votre connexion.",
                            isLoading = false
                        )
                    }
                } finally {
                    // Final check to stop loading if it hasn't stopped yet
                    if (_uiState.value.isLoading) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }

            // Observe local changes (this will pick up data from the remote fetch too)
            combine(
                repository.getStructure(documentId),
                repository.getLawCodes()
            ) { localStructure, codes ->
                val doc = codes.find { it.id == documentId }
                Pair(localStructure, doc)
            }.collect { (localStructure, doc) ->
                _structure.value = localStructure
                _document.value = doc
                
                val filtered = if (_uiState.value.searchQuery.isEmpty()) {
                    localStructure
                } else {
                    filterStructure(localStructure, _uiState.value.searchQuery)
                }

                _uiState.value = _uiState.value.copy(
                    structure = localStructure,
                    filteredStructure = filtered,
                    document = doc,
                    isLoading = if (localStructure.isNotEmpty()) false else _uiState.value.isLoading
                )
            }
        }
    }

    /**
     * Filtre la structure du document selon une requête.
     */
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        val filtered = if (query.isEmpty()) {
            _uiState.value.structure
        } else {
            filterStructure(_uiState.value.structure, query)
        }
        _uiState.value = _uiState.value.copy(filteredStructure = filtered)
    }

    private fun filterStructure(
        structure: Map<NodeEntity, List<ArticleEntity>>,
        query: String
    ): Map<NodeEntity, List<ArticleEntity>> {
        val result = mutableMapOf<NodeEntity, List<ArticleEntity>>()
        structure.forEach { (node, articles) ->
            val filteredArticles = articles.filter { 
                it.number.contains(query, ignoreCase = true) || 
                (it.content?.contains(query, ignoreCase = true) ?: false) 
            }
            if (filteredArticles.isNotEmpty()) {
                result[node] = filteredArticles
            }
        }
        return result
    }

    fun shareDocumentAsText() {
        val doc = _uiState.value.document ?: return
        val text = buildString {
            appendLine(doc.title)
            appendLine(doc.type ?: "Document Juridique")
            appendLine()
            appendLine("Retrouvez ce document complet sur Mibeko.")
            appendLine()
            appendLine("---")
            appendLine("Partagé via Mibeko - Le Droit numérique")
        }
        contentSharer.shareText(text, doc.title)
        _uiState.value = _uiState.value.copy(error = "Préparation du partage...")
    }

    fun shareDocumentAsLink() {
        val doc = _uiState.value.document ?: return
        
        val message = buildString {
            appendLine("📚 ${doc.title}")
            appendLine(doc.type ?: "Document Juridique")
            appendLine()
            appendLine("Consultez ce document sur Mibeko :")
            appendLine("https://mibeko.cg/document/${doc.id}")
        }
        
        contentSharer.shareText(message, "${doc.title} - Mibeko")
        _uiState.value = _uiState.value.copy(error = "Ouverture du lien de partage...")
    }

    fun copyDocumentLink() {
        val doc = _uiState.value.document ?: return
        val url = "https://mibeko.cg/document/${doc.id}"
        contentSharer.copyToClipboard(url)
        _uiState.value = _uiState.value.copy(error = "✓ Lien copié dans le presse-papiers")
    }

    fun shareDocumentAsPdf() {
        val doc = _uiState.value.document ?: return
        val url = repository.getDocumentExportUrl(doc.id)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            try {
                val bytes = repository.downloadFile(url)
                val fileName = "${doc.title.replace(" ", "_")}.pdf"
                contentSharer.shareFile(bytes, fileName, "application/pdf")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur lors du partage PDF: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isDownloading = false)
            }
        }
    }

    // Keep legacy method for compatibility
    fun shareDocument() {
        shareDocumentAsPdf()
    }

    /**
     * Retourne l'URL d'export PDF.
     */
    fun exportPdf(): String {
        val docId = _uiState.value.document?.id ?: return ""
        return repository.getDocumentExportUrl(docId)
    }

    /**
     * Active/Désactive le mode hors-ligne pour ce document.
     */
    fun toggleOffline() {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isDownloading = true)
                _isDownloading.value = true
                if (doc.isDownloaded) {
                    repository.removeDownload(doc.id)
                } else {
                    repository.downloadDocument(doc.id)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur lors du téléchargement : ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isDownloading = false)
                _isDownloading.value = false
            }
        }
    }

    /**
     * Envoie un signalement d'erreur
     */
    fun reportError(type: String, description: String) {
        val docId = _uiState.value.document?.id ?: return
        viewModelScope.launch {
            try {
                repository.reportError(
                    documentId = docId,
                    articleId = null,
                    type = type,
                    description = description
                )
                _uiState.value = _uiState.value.copy(error = "Signalement envoyé avec succès. Merci pour votre contribution.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur lors de l'envoi du signalement : ${e.message}")
            }
        }
    }

    /**
     * Efface l'erreur actuelle.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
