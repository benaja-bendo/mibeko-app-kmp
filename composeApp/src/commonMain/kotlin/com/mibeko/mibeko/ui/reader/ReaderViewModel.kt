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
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.articleLeafLabel
import com.mibeko.mibeko.util.articlePlainText
import com.mibeko.mibeko.util.formatTimestampToDate
import com.mibeko.mibeko.util.getAppVersionName
import com.mibeko.mibeko.util.recordException
import com.mibeko.mibeko.util.requestInAppReview
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
    /** « À jour au » du texte consolidé — `null` pour un acte unitaire. */
    val consolidationAsOf: String? = null,
    /** Date de la dernière synchronisation locale de ce texte. */
    val localVersionDate: String? = null,
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
    private val recentlyViewedManager: RecentlyViewedManager,
    private val analytics: MibekoAnalytics
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
                analytics.logEvent(AnalyticsEvents.REPORT_SUBMITTED, mapOf("type" to type))
                // Snackbar, pas uiState.error : un signalement réussi
                // s'affichait comme un écran d'erreur plein écran.
                _snackbarMessage.value = "Signalement envoyé avec succès. Merci !"
            } catch (e: Exception) {
                recordException(e, context = "ReaderViewModel.reportError")
                _snackbarMessage.value = "Erreur lors de l'envoi du signalement : ${e.message}"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    /**
     * In-app review passive : au 3e article lu, au plus une fois par version
     * installée. Jamais conditionnée à un avis positif ni précédée d'un
     * pré-filtre (guideline Apple 5.6.1) ; l'OS reste seul juge de
     * l'affichage réel.
     */
    private fun maybeRequestReview() {
        val readCount = userPreferencesRepository.incrementArticlesReadCount()
        val version = getAppVersionName()
        if (readCount >= 3 && !userPreferencesRepository.wasReviewRequestedFor(version)) {
            userPreferencesRepository.markReviewRequested(version)
            analytics.logEvent(AnalyticsEvents.REVIEW_REQUESTED)
            requestInAppReview()
        }
    }

    fun toggleOffline() {
        val currentArticle = _uiState.value.article ?: return
        val downloading = !currentArticle.isDownloaded
        viewModelScope.launch {
            try {
                repository.toggleArticleOffline(currentArticle, downloading)
                if (downloading) {
                    analytics.logEvent(AnalyticsEvents.OFFLINE_DOWNLOAD)
                }
                // Reload article to update UI state
                loadArticle(currentArticle.id)
            } catch (e: Exception) {
                recordException(e, context = "ReaderViewModel.toggleOffline")
                // Snackbar : l'article affiché reste lisible, inutile de le
                // remplacer par un écran d'erreur.
                _snackbarMessage.value = "Erreur : ${e.message}"
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
                
                if (newStatus) {
                    analytics.logEvent(AnalyticsEvents.FAVORITE_ADDED)
                }

                // Reload article to update UI state
                loadArticle(currentArticle.id)
            } catch (e: Exception) {
                // Chemin favoris = crash opaque historique (audit 07/2026) : on
                // remonte l'exception au collecteur avant de l'afficher.
                recordException(e, context = "ReaderViewModel.toggleFavorite")
                _snackbarMessage.value = "Erreur : ${e.message}"
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
                    analytics.logEvent(AnalyticsEvents.ARTICLE_READ)
                    maybeRequestReview()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        article = articleResult,
                        documentTitle = docResult?.title,
                        documentType = docResult?.type,
                        documentSlug = docResult?.slug,
                        consolidationAsOf = docResult?.consolidationAsOf,
                        localVersionDate = docResult?.lastUpdated
                            ?.takeIf { it > 0 }
                            ?.let { formatTimestampToDate(it) },
                        error = null
                    )

                    loadArticleSequence(articleResult.codeId)

                    // Log to recently viewed
                    recentlyViewedManager.addRecentlyViewed(
                        id = articleResult.id,
                        title = articleLeafLabel(articleResult.number),
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

    fun shareAsText() {
        val currentArticle = _uiState.value.article ?: return
        val documentTitle = _uiState.value.documentTitle ?: "Document"
        val label = articleLeafLabel(currentArticle.number)
        val text = buildString {
            appendLine(label)
            appendLine(documentTitle)
            appendLine()
            // Jamais `content` brut : un article hérité porte encore du HTML de
            // tableau, que le destinataire recevrait tel quel.
            append(articlePlainText(currentArticle.content, currentArticle.tables))
            appendLine()
            appendLine("---")
            appendLine("Partagé via Mibeko - Le Droit numérique")
        }
        contentSharer.shareText(text, label)
        analytics.logEvent(AnalyticsEvents.READER_SHARE, mapOf("format" to "text"))
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
        val label = articleLeafLabel(currentArticle.number)
        val message = buildString {
            appendLine("📚 $label")
            appendLine(documentTitle)
            appendLine()
            appendLine("Découvrez cet article sur Mibeko :")
            appendLine(link)
        }

        contentSharer.shareText(message, "$label - Mibeko")
        analytics.logEvent(AnalyticsEvents.READER_SHARE, mapOf("format" to "link"))
    }

    fun copyArticleText() {
        val currentArticle = _uiState.value.article ?: return
        val text = buildString {
            append(articleLeafLabel(currentArticle.number))
            append("\n\n")
            append(articlePlainText(currentArticle.content, currentArticle.tables))
        }
        contentSharer.copyToClipboard(text)
        _snackbarMessage.value = "✓ Texte de l'article copié"
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
                analytics.logEvent(AnalyticsEvents.READER_SHARE, mapOf("format" to "pdf"))
            } catch (e: Exception) {
                recordException(e, context = "ReaderViewModel.shareAsPdf")
                _snackbarMessage.value = "Erreur lors du partage PDF : ${e.message}"
            } finally {
                _isSharing.value = false
            }
        }
    }
}
