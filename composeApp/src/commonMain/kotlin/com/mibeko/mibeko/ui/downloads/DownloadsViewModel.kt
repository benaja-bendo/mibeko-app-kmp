package com.mibeko.mibeko.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val isLoading: Boolean = false,
    val documents: List<LawCodeSpec> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val error: String? = null
)

class DownloadsViewModel(
    private val repository: LocalLegalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    /**
     * Load all documents and their download status.
     */
    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getLawCodes().collect { codes ->
                _uiState.value = _uiState.value.copy(
                    documents = codes,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Start downloading a document for offline use.
     */
    fun downloadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadingIds = _uiState.value.downloadingIds + documentId
            )
            try {
                repository.downloadDocument(documentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erreur lors du téléchargement : ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    downloadingIds = _uiState.value.downloadingIds - documentId
                )
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
                _uiState.value = _uiState.value.copy(
                    error = "Erreur lors de la suppression : ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
