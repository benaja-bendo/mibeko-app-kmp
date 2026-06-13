package com.mibeko.mibeko.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class DossierWithSelection(
    val dossier: DossierEntity,
    val isSelected: Boolean
)

data class DossierSelectionUiState(
    val isLoading: Boolean = true,
    val dossiers: List<DossierWithSelection> = emptyList(),
    val error: String? = null
)

class DossierSelectionViewModel(
    private val dossierRepository: DossierRepository,
    private val localLegalRepository: LocalLegalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DossierSelectionUiState())
    val uiState: StateFlow<DossierSelectionUiState> = _uiState.asStateFlow()

    private var currentArticleId: String? = null

    fun loadDossiers(articleId: String) {
        currentArticleId = articleId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Ensure Favorites dossier exists
                dossierRepository.getOrCreateFavoritesDossier()
                
                val allDossiersFlow = dossierRepository.getAllDossiers()
                val selectedDossiersFlow = dossierRepository.getDossiersContainingArticle(articleId)

                allDossiersFlow.combine(selectedDossiersFlow) { allDossiers, selectedIds ->
                    allDossiers.map {
                        DossierWithSelection(
                            dossier = it,
                            isSelected = selectedIds.contains(it.id)
                        )
                    }
                }.collect { dossiers ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dossiers = dossiers,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur lors du chargement des dossiers: ${e.message}"
                )
            }
        }
    }

    fun toggleDossierSelection(dossierId: String, isSelected: Boolean) {
        val articleId = currentArticleId ?: return
        
        viewModelScope.launch {
            try {
                if (isSelected) {
                    dossierRepository.addArticleToDossier(dossierId, articleId)
                } else {
                    dossierRepository.removeArticleFromDossier(dossierId, articleId)
                }
                
                // If it's the Favorites dossier, update the ArticleEntity's is_favorite field
                val dossier = dossierRepository.getDossierById(dossierId).firstOrNull()
                if (dossier?.tag == DossierTag.FAVORIS) {
                    localLegalRepository.updateArticleFavoriteStatus(articleId, isSelected)
                }
                dossierRepository.syncNow()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erreur lors de la modification: ${e.message}"
                )
            }
        }
    }
    
    fun createAndAddToDossier(name: String, legalDomain: String, tag: DossierTag, description: String?, color: String) {
        val articleId = currentArticleId ?: return
        viewModelScope.launch {
            try {
                val dossierId = dossierRepository.createDossier(
                    name = name,
                    legalDomain = legalDomain,
                    tag = tag,
                    description = description,
                    color = color
                )
                dossierRepository.addArticleToDossier(dossierId, articleId)
                dossierRepository.syncNow()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erreur lors de la création du dossier: ${e.message}"
                )
            }
        }
    }
}
