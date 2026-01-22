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
    val isDownloading: Boolean = false
)

class DocumentDetailViewModel(private val repository: LocalLegalRepository) : ViewModel() {

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
                _uiState.value = _uiState.value.copy(
                    structure = localStructure,
                    document = doc,
                    // If we have data, we can stop the loading indicator even if the background fetch isn't finished
                    isLoading = if (localStructure.isNotEmpty()) false else _uiState.value.isLoading
                )
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
     * Efface l'erreur actuelle.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
