package com.mibeko.mibeko.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.remote.DocumentStats
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryFilter {
    ALL, DOWNLOADED
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val documents: List<LawCodeSpec> = emptyList(),
    val stats: List<DocumentStats> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val currentFilter: LibraryFilter = LibraryFilter.ALL,
    val error: String? = null
)

class LibraryViewModel(
    private val repository: LocalLegalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
        loadStats()
    }

    /**
     * Load statistics about document counts.
     */
    private fun loadStats() {
        viewModelScope.launch {
            val stats = repository.getDocumentStats()
            _uiState.value = _uiState.value.copy(stats = stats)
        }
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

    fun updateFilter(filter: LibraryFilter) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
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
