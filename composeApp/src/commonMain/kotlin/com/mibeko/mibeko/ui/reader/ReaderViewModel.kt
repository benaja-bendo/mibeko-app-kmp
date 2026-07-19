package com.mibeko.mibeko.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.preferences.RecentlyViewedManager
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.flow.asStateFlow

/** Entrée du sommaire : un article dans l'ordre du document. */
data class ReaderTocEntry(
    val id: String,
    val number: String,
    val nodeTitle: String
)

data class ReaderUiState(
    val isLoading: Boolean = true,
    val article: ArticleSpec? = null,
    val documentTitle: String? = null,
    val documentType: String? = null,
    /** Slug d'URL publique du document (partage vers mibeko.fr) — `null` si inconnu. */
    val documentSlug: String? = null,
    /** Articles du document dans l'ordre de lecture (navigation + sommaire). */
    val articleSequence: List<ReaderTocEntry> = emptyList(),
    val error: String? = null
) {
    val currentIndex: Int
        get() = article?.let { current -> articleSequence.indexOfFirst { it.id == current.id } } ?: -1
    val previousArticleId: String?
        get() = currentIndex.takeIf { it > 0 }?.let { articleSequence[it - 1].id }
    val nextArticleId: String?
        get() = currentIndex.takeIf { it >= 0 && it < articleSequence.size - 1 }
            ?.let { articleSequence[it + 1].id }
}

class ReaderViewModel(
    private val repository: LocalLegalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dossierRepository: DossierRepository,
    private val contentSharer: com.mibeko.mibeko.util.ContentSharer,
    private val recentlyViewedManager: RecentlyViewedManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val textSize: StateFlow<UserPreferencesRepository.TextSize> = userPreferencesRepository.textSizeFlow
    val isDyslexiaFontEnabled: StateFlow<Boolean> = userPreferencesRepository.isDyslexiaFontEnabledFlow
    val readerTheme: StateFlow<UserPreferencesRepository.ReaderTheme> = userPreferencesRepository.readerThemeFlow

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    /**
     * Envoie un signalement d'erreur pour cet article
     */
    fun reportError(type: String, description: String) {
        val article = _uiState.value.article ?: return
        viewModelScope.launch {
            try {
                repository.reportError(
                    documentId = null,
                    articleId = article.id,
                    type = type,
                    description = description
                )
                _uiState.value = _uiState.value.copy(error = "Signalement envoyé avec succès. Merci !")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur lors de l'envoi du signalement : ${e.message}")
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun toggleOffline() {
        val currentArticle = _uiState.value.article ?: return
        viewModelScope.launch {
            try {
                repository.toggleArticleOffline(currentArticle, !currentArticle.isDownloaded)
                // Reload article to update UI state
                loadArticle(currentArticle.id)
            } catch (e: Exception) {
                recordException(e, context = "ReaderViewModel.toggleOffline")
                _uiState.value = _uiState.value.copy(error = "Erreur: ${e.message}")
            }
        }
    }

    fun toggleFavorite() {
        val currentArticle = _uiState.value.article ?: return
        val newStatus = !currentArticle.isFavorite
        
        viewModelScope.launch {
            try {
                // Update local status
                repository.updateArticleFavoriteStatus(currentArticle.id, newStatus)
                
                // Sync with Favorites Dossier
                val favoritesDossier = dossierRepository.getOrCreateFavoritesDossier()
                if (newStatus) {
                    dossierRepository.addArticleToDossier(favoritesDossier.id, currentArticle.id)
                } else {
                    dossierRepository.removeArticleFromDossier(favoritesDossier.id, currentArticle.id)
                }
                
                // Reload article to update UI state
                loadArticle(currentArticle.id)
            } catch (e: Exception) {
                // Chemin favoris = crash opaque historique (audit 07/2026) : on
                // remonte l'exception au collecteur avant de l'afficher.
                recordException(e, context = "ReaderViewModel.toggleFavorite")
                _uiState.value = _uiState.value.copy(error = "Erreur: ${e.message}")
            }
        }
    }

    fun loadArticle(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                var articleResult = repository.getArticleById(id).firstOrNull()

                // Résultat de recherche d'un document jamais téléchargé : on
                // récupère le document parent depuis l'API puis on relit.
                if (articleResult == null && repository.ensureArticleAvailable(id)) {
                    articleResult = repository.getArticleById(id).firstOrNull()
                }

                if (articleResult != null) {
                    val docResult = repository.getLawCodeById(articleResult.codeId).firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        article = articleResult,
                        documentTitle = docResult?.title,
                        documentType = docResult?.type,
                        documentSlug = docResult?.slug,
                        error = null
                    )

                    loadArticleSequence(articleResult.codeId)

                    // Log to recently viewed
                    recentlyViewedManager.addRecentlyViewed(
                        id = articleResult.id,
                        title = "Article ${articleResult.number}",
                        typeCode = docResult?.type ?: "Inconnu"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Article indisponible. Vérifiez votre connexion internet puis réessayez."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement: ${e.message}"
                )
            }
        }
    }

    /**
     * Construit la séquence ordonnée des articles du document (même ordre que
     * la table des matières : nœuds par sort_order, articles par numéro).
     * Alimente la navigation précédent / suivant et le sommaire.
     */
    private var sequenceDocumentId: String? = null

    private suspend fun loadArticleSequence(documentId: String) {
        if (sequenceDocumentId == documentId && _uiState.value.articleSequence.isNotEmpty()) return
        try {
            val structure = repository.getStructure(documentId).firstOrNull() ?: return
            val sequence = structure.keys
                .sortedBy { it.sort_order }
                .flatMap { node ->
                    (structure[node] ?: emptyList())
                        .sortedBy { article -> article.number.filter { it.isDigit() }.toIntOrNull() ?: 0 }
                        .map { article ->
                            ReaderTocEntry(
                                id = article.id,
                                number = article.number,
                                nodeTitle = node.title
                            )
                        }
                }
            sequenceDocumentId = documentId
            _uiState.value = _uiState.value.copy(articleSequence = sequence)
        } catch (e: Exception) {
            // Pas bloquant : la lecture fonctionne sans navigation séquentielle.
        }
    }

    fun refreshPreferences() {
        // Obsolete now that we use reactive Flow
    }

    fun setTextSize(size: UserPreferencesRepository.TextSize) {
        userPreferencesRepository.setTextSize(size)
    }

    fun setReaderTheme(theme: UserPreferencesRepository.ReaderTheme) {
        userPreferencesRepository.setReaderTheme(theme)
    }

    fun setDyslexiaFontEnabled(enabled: Boolean) {
        userPreferencesRepository.setDyslexiaFontEnabled(enabled)
    }

    fun exportPdf(): String {
        val currentArticle = _uiState.value.article ?: return ""
        return repository.getArticleExportUrl(currentArticle.id)
    }

    fun shareAsText() {
        val currentArticle = _uiState.value.article ?: return
        val documentTitle = _uiState.value.documentTitle ?: "Document"
        val text = buildString {
            appendLine("Article ${currentArticle.number}")
            appendLine(documentTitle)
            appendLine()
            append(currentArticle.content ?: "")
            appendLine()
            appendLine("---")
            appendLine("Partagé via Mibeko - Le Droit numérique")
        }
        contentSharer.shareText(text, "Article ${currentArticle.number}")
        _snackbarMessage.value = "Préparation du partage..."
    }

    fun shareAsLink() {
        val currentArticle = _uiState.value.article ?: return
        val documentTitle = _uiState.value.documentTitle ?: "Document"

        // Lien public vers le portail citoyen mibeko.fr (slug connu → article
        // précis ; slug inconnu → accueil, cf. PublicLinks).
        val link = com.mibeko.mibeko.util.PublicLinks.article(
            documentSlug = _uiState.value.documentSlug,
            articleNumber = currentArticle.number
        )

        // Create a rich message with context and the link
        val message = buildString {
            appendLine("📚 Article ${currentArticle.number}")
            appendLine(documentTitle)
            appendLine()
            appendLine("Découvrez cet article sur Mibeko :")
            appendLine(link)
        }

        contentSharer.shareText(message, "Article ${currentArticle.number} - Mibeko")
        _snackbarMessage.value = "Ouverture du lien de partage..."
    }

    fun copyArticleText() {
        val currentArticle = _uiState.value.article ?: return
        val text = "Article ${currentArticle.number}\n\n${currentArticle.content ?: ""}"
        contentSharer.copyToClipboard(text)
        _snackbarMessage.value = "✓ Texte de l'article copié"
    }

    fun copyArticleLink() {
        val currentArticle = _uiState.value.article ?: return
        val url = com.mibeko.mibeko.util.PublicLinks.article(
            documentSlug = _uiState.value.documentSlug,
            articleNumber = currentArticle.number
        )
        contentSharer.copyToClipboard(url)
        _snackbarMessage.value = "✓ Lien de l'article copié"
    }

    fun shareAsPdf() {
        val currentArticle = _uiState.value.article ?: return
        val url = repository.getArticleExportUrl(currentArticle.id)
        
        viewModelScope.launch {
            _isSharing.value = true
            try {
                val bytes = repository.downloadFile(url)
                val fileName = "Article_${currentArticle.number}.pdf"
                contentSharer.shareFile(bytes, fileName, "application/pdf")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erreur lors du partage PDF: ${e.message}")
            } finally {
                _isSharing.value = false
            }
        }
    }

    // Legacy method for backward compatibility
    fun shareArticle() {
        shareAsPdf()
    }
}
