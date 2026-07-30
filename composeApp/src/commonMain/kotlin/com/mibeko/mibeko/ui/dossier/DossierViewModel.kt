package com.mibeko.mibeko.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.dao.DossierWithCount
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.data.repository.DossierSyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DossierViewModel(
    private val repository: DossierRepository,
    private val analytics: com.mibeko.mibeko.util.MibekoAnalytics
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedTag = MutableStateFlow<DossierTag?>(null)
    private val isGridView = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    /** État de synchronisation exposé à l'écran (bandeau / icône). */
    val syncState: StateFlow<DossierSyncState> = repository.syncState

    val uiState: StateFlow<DossierListUiState> = combine(
        repository.getDossiersWithCount().catch { e ->
            error.value = e.message ?: "Erreur inconnue"
            emit(emptyList())
        },
        searchQuery,
        selectedTag,
        isGridView,
        error
    ) { dossiers, query, tag, grid, err ->
        val filtered = dossiers
            .filter { tag == null || it.dossier.tag == tag }
            .filter { query.isBlank() || it.dossier.name.contains(query, ignoreCase = true) }
        DossierListUiState(
            isLoading = false,
            dossiers = filtered,
            error = err,
            searchQuery = query,
            selectedTag = tag,
            isGridView = grid
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DossierListUiState(isLoading = true)
    )

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _editingDossier = MutableStateFlow<DossierEntity?>(null)
    val editingDossier: StateFlow<DossierEntity?> = _editingDossier.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getOrCreateFavoritesDossier()
            repository.syncNow()
        }
    }

    /** Synchronisation manuelle (bouton rafraîchir). */
    fun refresh() {
        viewModelScope.launch { repository.syncNow() }
    }

    fun filterByTag(tag: DossierTag?) {
        selectedTag.value = tag
    }

    fun toggleViewMode() {
        isGridView.value = !isGridView.value
    }

    fun searchDossiers(query: String) {
        searchQuery.value = query
    }

    fun showCreateDialog() {
        _editingDossier.value = null
        _showCreateDialog.value = true
    }

    fun showEditDialog(dossier: DossierEntity) {
        _editingDossier.value = dossier
        _showCreateDialog.value = true
    }

    fun dismissDialog() {
        _showCreateDialog.value = false
        _editingDossier.value = null
    }

    fun createDossier(
        name: String,
        legalDomain: String,
        tag: DossierTag,
        description: String?,
        color: String
    ) {
        viewModelScope.launch {
            repository.createDossier(name, legalDomain, tag, description, color)
            analytics.logEvent(com.mibeko.mibeko.util.AnalyticsEvents.DOSSIER_CREATED)
            _showCreateDialog.value = false
            repository.syncNow()
        }
    }

    fun updateDossier(
        dossierId: String,
        name: String,
        legalDomain: String,
        tag: DossierTag,
        description: String?,
        color: String
    ) {
        viewModelScope.launch {
            repository.updateDossier(dossierId, name, legalDomain, tag, description, color)
            _showCreateDialog.value = false
            _editingDossier.value = null
            repository.syncNow()
        }
    }

    fun deleteDossier(dossierId: String) {
        viewModelScope.launch {
            repository.deleteDossier(dossierId)
            repository.syncNow()
        }
    }
}

data class DossierListUiState(
    val isLoading: Boolean = false,
    val dossiers: List<DossierWithCount> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedTag: DossierTag? = null,
    val isGridView: Boolean = false
)
