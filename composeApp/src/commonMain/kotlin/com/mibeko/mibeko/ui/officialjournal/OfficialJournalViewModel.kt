package com.mibeko.mibeko.ui.officialjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.RemoteOfficialJournal
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.util.ContentSharer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfficialJournalUiState(
    val isLoading: Boolean = false,
    val isDownloadingPdf: Boolean = false,
    val journals: List<RemoteOfficialJournal> = emptyList(),
    val currentJournal: RemoteOfficialJournal? = null,
    val error: String? = null
)

class OfficialJournalViewModel(
    private val repository: LocalLegalRepository,
    private val contentSharer: ContentSharer
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficialJournalUiState())
    val uiState: StateFlow<OfficialJournalUiState> = _uiState.asStateFlow()

    private var currentFilterNumber: String? = null
    private var currentFilterYear: Int? = null

    fun loadJournals(page: Int = 1, number: String? = currentFilterNumber, year: Int? = currentFilterYear) {
        currentFilterNumber = number
        currentFilterYear = year
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getOfficialJournals(page, number, year)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        journals = response.data
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur lors du chargement des journaux officiels."
                )
            }
        }
    }

    fun loadJournalDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val journal = repository.getOfficialJournal(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentJournal = journal
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur lors du chargement des détails."
                )
            }
        }
    }

    fun getPdfUrl(id: String): String {
        return repository.getOfficialJournalPdfUrl(id)
    }

    fun sharePdf(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloadingPdf = true, error = null)
            try {
                val url = repository.getOfficialJournalPdfUrl(id)
                val bytes = repository.downloadFile(url)
                val journal = _uiState.value.currentJournal
                val fileName = "Journal_Officiel_${journal?.publication_date ?: id}.pdf".replace(" ", "_")
                contentSharer.shareFile(bytes, fileName, "application/pdf")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = "Erreur lors du partage du PDF: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isDownloadingPdf = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}