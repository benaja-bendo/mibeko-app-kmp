package com.mibeko.mibeko.ui.dossier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DossierViewModel(
    private val repository: DossierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DossierListUiState())
    val uiState: StateFlow<DossierListUiState> = _uiState.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _editingDossier = MutableStateFlow<DossierEntity?>(null)
    val editingDossier: StateFlow<DossierEntity?> = _editingDossier.asStateFlow()

    init {
        loadDossiers()
        ensureFavoritesExist()
    }

    /**
     * Vérifie si un dossier "Favoris" existe, sinon le crée par défaut.
     */
    private fun ensureFavoritesExist() {
        viewModelScope.launch {
            repository.getAllDossiers().firstOrNull()?.let { dossiersWithCount ->
                val dossiers = dossiersWithCount.map { it.dossier }
                val exists = dossiers.any { 
                    it.name.equals("Favoris", ignoreCase = true) || 
                    it.name.equals("Favorites", ignoreCase = true) 
                }
                
                if (!exists) {
                    repository.createDossier(
                        name = "Favoris",
                        legalDomain = "Général",
                        tag = DossierTag.FAVORIS,
                        description = "Mes articles sauvegardés",
                        color = "#FFD700" // Gold color
                    )
                    loadDossiers()
                }
            }
        }
    }

    private fun loadDossiers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllDossiers()
                .catch { e ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = e.message ?: "Erreur inconnue") 
                    }
                }
                .collect { dossiersWithCount ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            dossiers = dossiersWithCount.map { dwc -> dwc.dossier },
                            error = null
                        ) 
                    }
                }
        }
    }

    fun filterByTag(tag: DossierTag?) {
        _uiState.update { it.copy(selectedTag = tag) }
        viewModelScope.launch {
            if (tag == null) {
                loadDossiers()
            } else {
                repository.getDossiersByTag(tag)
                    .collect { dossiers ->
                        _uiState.update { it.copy(dossiers = dossiers) }
                    }
            }
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun searchDossiers(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.getAllDossiers()
                    .collect { dossiersWithCount ->
                        _uiState.update {
                            it.copy(dossiers = dossiersWithCount.map { dwc -> dwc.dossier })
                        }
                    }
            } else {
                repository.searchDossiers(query)
                    .collect { dossiers ->
                        _uiState.update { it.copy(dossiers = dossiers) }
                    }
            }
        }
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
            _showCreateDialog.value = false
            loadDossiers()
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
            loadDossiers()
        }
    }

    fun deleteDossier(dossierId: String) {
        viewModelScope.launch {
            repository.deleteDossier(dossierId)
            loadDossiers()
        }
    }
}

data class DossierListUiState(
    val isLoading: Boolean = false,
    val dossiers: List<DossierEntity> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedTag: DossierTag? = null,
    val isGridView: Boolean = false
)
