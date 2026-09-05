package com.mibeko.mibeko.ui.details

import com.mibeko.mibeko.util.recordException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.util.isExportEntitlementDenied
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.LawCodeSpec
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * État UI pour l'écran de détail d'un document.
 */
data class DocumentDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Confirmations et infos (snackbar) : séparées de `error`, qui déclenche
    // l'écran d'échec quand la structure est vide.
    val message: String? = null,
    val document: LawCodeSpec? = null,
    val structure: Map<NodeEntity, List<ArticleEntity>> = emptyMap(),
    val filteredStructure: Map<NodeEntity, List<ArticleEntity>> = emptyMap(),
    val searchQuery: String = "",
    val isDownloading: Boolean = false,
    // Partage PDF distinct du téléchargement hors-ligne : chaque bouton de la
    // bottom bar n'affiche que son propre état.
    val isSharingPdf: Boolean = false
)

class DocumentDetailViewModel(
    private val repository: LocalLegalRepository,
    private val contentSharer: com.mibeko.mibeko.util.ContentSharer,
    private val analytics: com.mibeko.mibeko.util.MibekoAnalytics
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
        _uiState.update { it.copy(isLoading = true, error = null) }
        analytics.logEvent(com.mibeko.mibeko.util.AnalyticsEvents.DOCUMENT_OPENED)

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
                    recordException(e, context = "DocumentDetailViewModel.loadStructure")
                    // Only show error if we have no data at all
                    if (_uiState.value.structure.isEmpty()) {
                        _uiState.update { it.copy(
                            error = "Impossible de charger le document. Vérifiez votre connexion.",
                            isLoading = false
                        ) }
                    }
                } finally {
                    // Final check to stop loading if it hasn't stopped yet
                    if (_uiState.value.isLoading) {
                        _uiState.update { it.copy(isLoading = false) }
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

                _uiState.update { it.copy(
                    structure = localStructure,
                    filteredStructure = filtered,
                    document = doc,
                    isLoading = if (localStructure.isNotEmpty()) false else it.isLoading
                ) }
            }
        }
    }

    /**
     * Filtre la structure du document selon une requête.
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        val filtered = if (query.isEmpty()) {
            _uiState.value.structure
        } else {
            filterStructure(_uiState.value.structure, query)
        }
        _uiState.update { it.copy(filteredStructure = filtered) }
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

    fun shareDocumentAsLink() {
        val doc = _uiState.value.document ?: return

        // Lien public vers le portail citoyen mibeko.fr (slug connu → texte
        // précis ; slug inconnu → accueil, cf. PublicLinks).
        val link = com.mibeko.mibeko.util.PublicLinks.document(doc.slug)

        val message = buildString {
            appendLine("📚 ${doc.title}")
            appendLine(doc.type)
            appendLine()
            appendLine("Consultez ce document sur Mibeko :")
            appendLine(link)
        }

        contentSharer.shareText(message, "${doc.title} - Mibeko")
    }

    fun copyDocumentLink() {
        val doc = _uiState.value.document ?: return
        val url = com.mibeko.mibeko.util.PublicLinks.document(doc.slug)
        contentSharer.copyToClipboard(url)
        _uiState.update { it.copy(message = "✓ Lien copié dans le presse-papiers") }
    }

    fun shareDocumentAsPdf() {
        val doc = _uiState.value.document ?: return
        val url = repository.getDocumentExportUrl(doc.id)

        viewModelScope.launch {
            _uiState.update { it.copy(isSharingPdf = true) }
            try {
                val bytes = repository.downloadFile(url)
                val fileName = "${doc.title.replace(" ", "_")}.pdf"
                contentSharer.shareFile(bytes, fileName, "application/pdf")
            } catch (e: ClientRequestException) {
                if (isExportEntitlementDenied(e.response.status.value)) {
                    // mibeko-dashboard#86 : refus attendu (compte non-Pro),
                    // pas une panne — pas de recordException pour ce cas.
                    _uiState.update { it.copy(error = "Le PDF Mibeko est réservé aux comptes Mibeko Pro.") }
                } else {
                    recordException(e, context = "DocumentDetailViewModel.shareDocumentAsPdf")
                    _uiState.update { it.copy(error = "Erreur lors du partage PDF: ${e.message}") }
                }
            } catch (e: Exception) {
                recordException(e, context = "DocumentDetailViewModel.shareDocumentAsPdf")
                _uiState.update { it.copy(error = "Erreur lors du partage PDF: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSharingPdf = false) }
            }
        }
    }

    /**
     * Active/Désactive le mode hors-ligne pour ce document.
     */
    fun toggleOffline() {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDownloading = true) }
                _isDownloading.value = true
                if (doc.isDownloaded) {
                    repository.removeDownload(doc.id)
                } else {
                    val cached = repository.downloadDocument(doc.id)
                    if (!cached) {
                        // PDF-only : rien n'est mis en cache (voir PdfViewer),
                        // le badge « Hors-ligne » ne doit pas s'allumer pour
                        // autant — dire pourquoi plutôt que de laisser le
                        // bouton ne rien faire visiblement.
                        _uiState.update { it.copy(
                            error = "Ce document (PDF non structuré) ne peut pas être mis à disposition hors-ligne."
                        ) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erreur lors du téléchargement : ${e.message}") }
            } finally {
                _uiState.update { it.copy(isDownloading = false) }
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
                _uiState.update { it.copy(message = "Signalement envoyé avec succès. Merci pour votre contribution.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erreur lors de l'envoi du signalement : ${e.message}") }
            }
        }
    }

    /**
     * Efface l'erreur actuelle.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Efface le message d'information actuel.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
