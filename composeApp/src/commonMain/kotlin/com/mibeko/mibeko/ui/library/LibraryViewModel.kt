package com.mibeko.mibeko.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.remote.DocumentStats
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.remote.RemoteDocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.preferences.RecentlyViewedItem
import com.mibeko.mibeko.data.preferences.RecentlyViewedManager

enum class LibraryFilter {
    ALL, DOWNLOADED
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val documents: List<LawCodeSpec> = emptyList(),
    val filteredDocuments: List<LawCodeSpec> = emptyList(),
    val latestDocuments: List<LawCodeSpec> = emptyList(),
    val documentTypes: List<RemoteDocumentType> = emptyList(),
    val institutions: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val stats: List<DocumentStats> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val currentFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedType: String? = null,
    val selectedInstitution: String? = null,
    val selectedYear: String? = null,
    val error: String? = null
)

class LibraryViewModel(
    private val repository: LocalLegalRepository,
    private val recentlyViewedManager: RecentlyViewedManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val recentItems: StateFlow<List<RecentlyViewedItem>> = recentlyViewedManager.recentItems

    init {
        observeDocuments()
        loadStats()
        loadDocumentTypes()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getLawCodes().collect { codes ->
                val sortedCodes = codes.sortedByDescending { it.lastUpdated }
                
                val institutions = codes.mapNotNull { it.institutionName }.distinct().sorted()
                val years = codes.mapNotNull { it.dateSignature?.take(4) }.distinct().sortedDescending()
                
                _uiState.value = _uiState.value.copy(
                    documents = codes,
                    institutions = institutions,
                    years = years,
                    latestDocuments = sortedCodes.take(5), // Take top 5 for Nouveautés
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.documents

        if (state.selectedType != null) {
            filtered = filtered.filter { it.type == state.selectedType }
        }
        if (state.selectedInstitution != null) {
            filtered = filtered.filter { it.institutionName == state.selectedInstitution }
        }
        if (state.selectedYear != null) {
            filtered = filtered.filter { it.dateSignature?.take(4) == state.selectedYear }
        }
        
        // Default sort for the vertical list (e.g., most recent first)
        filtered = filtered.sortedByDescending { it.dateSignature ?: it.lastUpdated }

        _uiState.value = state.copy(filteredDocuments = filtered)
    }

    fun updateTypeFilter(type: String?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        applyFilters()
    }

    fun updateInstitutionFilter(institution: String?) {
        _uiState.value = _uiState.value.copy(selectedInstitution = institution)
        applyFilters()
    }

    fun updateYearFilter(year: String?) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        applyFilters()
    }

    private fun loadDocumentTypes() {
        viewModelScope.launch {
            try {
                val types = repository.getDocumentTypes()
                _uiState.value = _uiState.value.copy(documentTypes = types)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
