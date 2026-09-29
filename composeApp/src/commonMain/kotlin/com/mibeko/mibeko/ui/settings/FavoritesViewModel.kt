package com.mibeko.mibeko.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.dao.DossierArticleWithDetails
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * « Mes favoris » (D-052) : les articles marqués d'un signet, du plus récent
 * au plus ancien. Sans dossiers ni notes : le stockage reste le dossier
 * étiqueté FAVORIS, mais l'écran n'en montre que la liste.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModel(
    private val dossierRepository: DossierRepository,
    private val legalRepository: LocalLegalRepository
) : ViewModel() {

    /**
     * `null` tant que la base locale n'a pas répondu : l'écran n'affiche pas
     * « aucun favori » avant de savoir.
     */
    val favorites: StateFlow<List<DossierArticleWithDetails>?> = dossierRepository
        .getDossiersByTag(DossierTag.FAVORIS)
        .flatMapLatest { dossiers ->
            if (dossiers.isEmpty()) {
                flowOf(emptyList())
            } else {
                // Plusieurs dossiers Favoris peuvent coexister sur un appareil
                // resté en 1.4 (kmp#11) : on les lit tous, sans doublon.
                combine(dossiers.map { dossierRepository.getDossierArticles(it.id) }) { lists ->
                    lists.flatMap { it }
                        .distinctBy { it.article.id }
                        .sortedByDescending { it.added_at }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val count: StateFlow<Int?> = favorites
        .map { it?.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // L'onglet Dossiers, qui déclenchait la synchronisation, a disparu :
        // « Moi » et « Mes favoris » prennent le relais pour ramener les
        // favoris posés sur un autre appareil. Sans compte ou sans réseau,
        // syncNow ne fait rien.
        viewModelScope.launch { dossierRepository.syncNow() }
    }

    private val _message = MutableStateFlow<String?>(null)
    /** Message ponctuel (échec du retrait) à afficher dans une snackbar. */
    val message: StateFlow<String?> = _message.asStateFlow()

    fun messageShown() {
        _message.value = null
    }

    fun remove(favorite: DossierArticleWithDetails) {
        viewModelScope.launch {
            try {
                dossierRepository.getDossiersByTag(DossierTag.FAVORIS).first()
                    .forEach { dossierRepository.removeArticleFromDossier(it.id, favorite.article.id) }
                legalRepository.updateArticleFavoriteStatus(favorite.article.id, false)
            } catch (e: Exception) {
                recordException(e, context = "FavoritesViewModel.remove")
                _message.value = "Ce favori n'a pas pu être retiré. Réessayez."
                return@launch
            }
            // Hors ligne, le retrait reste local et partira à la prochaine
            // synchronisation : ce n'est pas un échec pour l'utilisateur.
            dossierRepository.syncNow()
        }
    }
}
