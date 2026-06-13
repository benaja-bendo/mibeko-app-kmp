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
    /** Catalogue complet, trié du plus récent au plus ancien (ordre serveur). */
    val journals: List<RemoteOfficialJournal> = emptyList(),
    val searchQuery: String = "",
    val selectedYear: Int? = null,
    val currentJournal: RemoteOfficialJournal? = null,
    val error: String? = null
) {
    /** Filtrage local : le catalogue est petit, le ressenti doit être instantané. */
    val filteredJournals: List<RemoteOfficialJournal>
        get() = journals.filter { journal ->
            val matchesYear = selectedYear == null || journal.year == selectedYear
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                journal.title.contains(q, ignoreCase = true) ||
                (journal.number?.contains(q, ignoreCase = true) ?: false)
            matchesYear && matchesQuery
        }

    val availableYears: List<Int>
        get() = journals.mapNotNull { it.year }.distinct().sortedDescending()

    /** Position dans le kiosque pour la navigation précédent / suivant. */
    val currentIndex: Int
        get() = currentJournal?.let { current -> journals.indexOfFirst { it.id == current.id } } ?: -1

    /** JO plus récent (la liste est triée par date décroissante). */
    val previousJournalId: String?
        get() = currentIndex.takeIf { it > 0 }?.let { journals[it - 1].id }

    /** JO plus ancien. */
    val nextJournalId: String?
        get() = currentIndex.takeIf { it >= 0 && it < journals.size - 1 }
            ?.let { journals[it + 1].id }
}

/** Année de publication extraite de la date ISO (« 2026-06-12T… » → 2026). */
val RemoteOfficialJournal.year: Int?
    get() = publication_date.take(4).toIntOrNull()

class OfficialJournalViewModel(
    private val repository: LocalLegalRepository,
    private val contentSharer: ContentSharer
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfficialJournalUiState())
    val uiState: StateFlow<OfficialJournalUiState> = _uiState.asStateFlow()

    /**
     * Charge le catalogue complet des JO (quelques dizaines de numéros) :
     * le filtrage et le regroupement par mois se font ensuite localement.
     */
    fun loadJournals() {
        if (_uiState.value.journals.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val all = mutableListOf<RemoteOfficialJournal>()
                var page = 1
                var lastPage: Int
                do {
                    val response = repository.getOfficialJournals(page = page, perPage = 100)
                    if (!response.success) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = response.message)
                        return@launch
                    }
                    all += response.data
                    lastPage = response.meta?.last_page ?: 1
                    page++
                } while (page <= lastPage && page <= 5)

                _uiState.value = _uiState.value.copy(isLoading = false, journals = all)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur lors du chargement des journaux officiels."
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onYearSelected(year: Int?) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
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
        // Le catalogue alimente la navigation JO précédent / suivant, y compris
        // quand on arrive directement depuis l'accueil.
        loadJournals()
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
