package com.mibeko.mibeko.ui.resolver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.SlugResolution
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.UiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Résout un lien public `mibeko.fr/textes/{slug}` reçu en App Link, puis émet
 * une redirection interne (article, document) ou un état honnête si la
 * résolution échoue — c'est la colle du funnel site → app, jamais un
 * silence.
 */
class TexteResolverViewModel(
    private val repository: LocalLegalRepository,
    private val analytics: MibekoAnalytics,
    private val networkChecker: NetworkConnectivityChecker
) : ViewModel() {

    sealed class Target {
        data object Resolving : Target()
        data class Reader(val articleId: String) : Target()
        data class Document(val documentId: String) : Target()
        /** Slug réellement inconnu (404) — sûr de le dire. */
        data object NotFound : Target()
        /** Résolution non vérifiable (réseau/serveur) — jamais confondu avec NotFound. */
        data class Failed(val error: UiResult.Error) : Target()
    }

    private val _target = MutableStateFlow<Target>(Target.Resolving)
    val target: StateFlow<Target> = _target.asStateFlow()

    private var started = false
    private var lastSlug: String? = null
    private var lastArticleNumber: String? = null

    fun resolve(docSlug: String, articleNumber: String?) {
        if (started) return
        started = true
        lastSlug = docSlug
        lastArticleNumber = articleNumber
        analytics.logEvent(
            AnalyticsEvents.DEEP_LINK_OPENED,
            mapOf("has_article" to (articleNumber != null))
        )
        performResolve(docSlug, articleNumber)
    }

    private fun retry() {
        val slug = lastSlug ?: return
        _target.value = Target.Resolving
        performResolve(slug, lastArticleNumber)
    }

    private fun performResolve(docSlug: String, articleNumber: String?) {
        viewModelScope.launch {
            val resolution = runCatching {
                repository.resolveTexteBySlug(docSlug, articleNumber)
            }.getOrElse { SlugResolution.Failed }

            _target.value = when (resolution) {
                is SlugResolution.Article -> Target.Reader(resolution.articleId)
                is SlugResolution.Document -> Target.Document(resolution.documentId)
                SlugResolution.NotFound -> {
                    analytics.logEvent(
                        AnalyticsEvents.DEEP_LINK_RESOLUTION_FAILED,
                        mapOf("reason" to "not_found")
                    )
                    Target.NotFound
                }
                SlugResolution.Failed -> {
                    analytics.logEvent(
                        AnalyticsEvents.DEEP_LINK_RESOLUTION_FAILED,
                        mapOf("reason" to "error")
                    )
                    Target.Failed(
                        UiResult.Error(
                            offline = !networkChecker.isNetworkAvailable(),
                            retry = ::retry
                        )
                    )
                }
            }
        }
    }
}
