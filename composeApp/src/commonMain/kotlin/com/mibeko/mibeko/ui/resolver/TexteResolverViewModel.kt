package com.mibeko.mibeko.ui.resolver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.SlugResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Résout un lien public `mibeko.fr/textes/{slug}` reçu en App Link, puis émet
 * une redirection interne (article, document, ou accueil en dernier recours).
 */
class TexteResolverViewModel(
    private val repository: LocalLegalRepository,
    private val analytics: com.mibeko.mibeko.util.MibekoAnalytics
) : ViewModel() {

    sealed class Target {
        data object Resolving : Target()
        data class Reader(val articleId: String) : Target()
        data class Document(val documentId: String) : Target()
        /** Slug irrésolu (inconnu, ou hors-ligne sans cache) → repli accueil. */
        data object Home : Target()
    }

    private val _target = MutableStateFlow<Target>(Target.Resolving)
    val target: StateFlow<Target> = _target.asStateFlow()

    private var started = false

    fun resolve(docSlug: String, articleNumber: String?) {
        if (started) return
        started = true
        analytics.logEvent(
            com.mibeko.mibeko.util.AnalyticsEvents.DEEP_LINK_OPENED,
            mapOf("has_article" to (articleNumber != null))
        )
        viewModelScope.launch {
            val resolution = runCatching {
                repository.resolveTexteBySlug(docSlug, articleNumber)
            }.getOrNull()

            _target.value = when (resolution) {
                is SlugResolution.Article -> Target.Reader(resolution.articleId)
                is SlugResolution.Document -> Target.Document(resolution.documentId)
                null -> Target.Home
            }
        }
    }
}
