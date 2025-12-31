package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.toArticleSpec
import com.mibeko.mibeko.data.toLawCodeSpec
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.ArticleTagEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.data.local.entities.TagEntity
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.remote.RemoteNode
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Sealed class representing the result of a search operation.
 */
sealed class SearchResult {
    data class Success(val articles: List<ArticleSpec>, val isFromNetwork: Boolean) : SearchResult()
    data class Error(val message: String, val fallbackArticles: List<ArticleSpec>) : SearchResult()
    object Loading : SearchResult()
}

class LocalLegalRepository(
    private val mibekoDao: MibekoDao,
    private val apiService: LegalApiService,
    private val networkChecker: NetworkConnectivityChecker,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend fun sync() {
        var currentPage = 1
        var totalPages = 1

        while (currentPage <= totalPages) {
            val response = apiService.fetchDocuments(currentPage)
            totalPages = response.meta.last_page

            val documents = mutableListOf<DocumentEntity>()
            val allNodes = mutableListOf<NodeEntity>()
            val allArticles = mutableListOf<ArticleEntity>()

            for (remoteDoc in response.data) {
                // Map document
                documents.add(DocumentEntity(
                    id = remoteDoc.id,
                    title = remoteDoc.title,
                    type_code = remoteDoc.type.code,
                    last_updated = parseIsoDate(remoteDoc.updated_at)
                ))

                // Fetch full tree for this document to get structure and articles
                val treeNodes = apiService.fetchDocumentTree(remoteDoc.id)
                flattenTree(remoteDoc.id, treeNodes, allNodes, allArticles)
            }

            mibekoDao.syncAll(documents, allNodes, allArticles)
            currentPage++
        }
    }

    suspend fun syncUpdates(since: String) {
        val response = apiService.fetchUpdates(since)
        
        val updatedArticles = mutableListOf<ArticleEntity>()
        val updatedTags = mutableListOf<TagEntity>()
        val updatedArticleTags = mutableListOf<ArticleTagEntity>()

        response.data.updated.forEach { remoteArticle ->
            updatedArticles.add(ArticleEntity(
                id = remoteArticle.id,
                node_id = remoteArticle.parent_node_id,
                number = remoteArticle.number,
                content = remoteArticle.content ?: "",
                is_favorite = false
            ))

            remoteArticle.tags.forEach { tagName ->
                val tagId = tagName.hashCode().toString() 
                val tagSlug = tagName.lowercase().replace(" ", "-")
                
                updatedTags.add(TagEntity(
                    id = tagId,
                    name = tagName,
                    slug = tagSlug,
                    type = "generated"
                ))
                
                updatedArticleTags.add(ArticleTagEntity(
                    article_id = remoteArticle.id,
                    tag_id = tagId
                ))
            }
        }

        mibekoDao.syncAll(
            documents = emptyList(),
            nodes = emptyList(),
            articles = updatedArticles,
            tags = updatedTags,
            articleTags = updatedArticleTags,
            deletedArticleIds = response.data.deleted_ids
        )
    }

    private fun flattenTree(
        documentId: String,
        nodes: List<RemoteNode>,
        outNodes: MutableList<NodeEntity>,
        outArticles: MutableList<ArticleEntity>
    ) {
        nodes.forEach { node ->
            outNodes.add(NodeEntity(
                id = node.id,
                document_id = documentId,
                parent_id = null,
                title = node.title ?: "",
                sort_order = node.order
            ))

            node.articles.forEach { articleBrief ->
                outArticles.add(ArticleEntity(
                    id = articleBrief.id,
                    node_id = node.id,
                    number = articleBrief.number,
                    content = articleBrief.content ?: "",
                    is_favorite = false
                ))
            }
        }
    }

    private fun parseIsoDate(isoString: String): Long {
        return 0L 
    }

    fun getLawCodes(): Flow<List<com.mibeko.mibeko.data.LawCodeSpec>> {
        return mibekoDao.getAllDocuments().map { docs ->
            docs.map { it.toLawCodeSpec() }
        }
    }

    fun getFavoriteArticles(): Flow<List<ArticleSpec>> {
        return mibekoDao.getFavoriteArticles().map { results ->
            results.map { result ->
                result.article.toArticleSpec(
                    codeId = result.document_id,
                    title = result.node_title,
                    breadcrumb = result.node_title
                )
            }
        }
    }

    fun getArticleById(id: String): Flow<ArticleSpec?> {
        return mibekoDao.getArticleById(id).map { result ->
            result?.article?.toArticleSpec(
                codeId = result.document_id,
                title = result.node_title,
                breadcrumb = result.node_title
            )
        }
    }

    /**
     * Hybrid search: queries API if online, falls back to local Room database.
     * 
     * This is the core hybrid search logic:
     * 1. Check if network is available AND offline mode is not forced
     * 2. If yes -> Call API for live search (leverages backend full-text/pgvector search)
     * 3. If no (or on API error) -> Query local Room database
     * 
     * @param query The search query string
     * @return SearchResult containing articles and source information
     */
    suspend fun searchHybrid(query: String): SearchResult {
        val shouldUseNetwork = networkChecker.isNetworkAvailable() && 
                               !userPreferencesRepository.isOfflineModeEnabled()
        
        return if (shouldUseNetwork) {
            try {
                val response = apiService.searchArticles(query)
                val articles = response.data.map { it.toArticleSpec() }
                SearchResult.Success(articles, isFromNetwork = true)
            } catch (e: Exception) {
                // Network request failed, fallback to local search
                val localResults = searchLocally(query)
                SearchResult.Error(
                    message = "Erreur réseau: ${e.message ?: "Connexion impossible"}",
                    fallbackArticles = localResults
                )
            }
        } else {
            val localResults = searchLocally(query)
            SearchResult.Success(localResults, isFromNetwork = false)
        }
    }

    /**
     * Local-only search using Room database.
     * Used as primary when offline or as fallback when API fails.
     */
    private suspend fun searchLocally(query: String): List<ArticleSpec> {
        return mibekoDao.searchArticles(query).first().map { result ->
            result.article.toArticleSpec(
                codeId = result.document_id,
                title = result.node_title,
                breadcrumb = result.node_title
            )
        }
    }

    /**
     * Legacy Flow-based search for backward compatibility.
     * Always uses local Room database.
     * 
     * @deprecated Use searchHybrid() for network-enabled search
     */
    fun search(query: String): Flow<List<ArticleSpec>> {
        return mibekoDao.searchArticles(query).map { results ->
            results.map { result ->
                result.article.toArticleSpec(
                    codeId = result.document_id,
                    title = result.node_title,
                    breadcrumb = result.node_title
                )
            }
        }
    }

    fun getStructure(documentId: String): Flow<Map<NodeEntity, List<ArticleEntity>>> {
        return mibekoDao.getStructure(documentId)
    }

    /**
     * Get autocomplete suggestions for a query.
     * Uses hybrid approach: API if online, local if offline.
     * Returns formatted strings like "Article 45 - Code du Travail".
     */
    suspend fun getAutocompleteSuggestionsHybrid(query: String): List<String> {
        val shouldUseNetwork = networkChecker.isNetworkAvailable() && 
                               !userPreferencesRepository.isOfflineModeEnabled()
        
        return if (shouldUseNetwork) {
            try {
                val response = apiService.searchArticles(query, page = 1)
                response.data.take(10).map { "${it.number} - ${it.breadcrumb}" }
            } catch (e: Exception) {
                getAutocompleteSuggestionsLocally(query)
            }
        } else {
            getAutocompleteSuggestionsLocally(query)
        }
    }

    private suspend fun getAutocompleteSuggestionsLocally(query: String): List<String> {
        return mibekoDao.searchArticles(query).first().take(10).map { result ->
            "${result.article.number} - ${result.node_title}"
        }
    }

    /**
     * Legacy Flow-based autocomplete for backward compatibility.
     * Always uses local Room database.
     */
    fun getAutocompleteSuggestions(query: String): Flow<List<String>> {
        return mibekoDao.searchArticles(query).map { results ->
            results.take(10).map { result ->
                "${result.article.number} - ${result.node_title}"
            }
        }
    }
}
