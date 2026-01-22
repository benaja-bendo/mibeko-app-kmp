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
import com.mibeko.mibeko.data.remote.ApiResponse
import com.mibeko.mibeko.data.remote.RemoteNode
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import com.mibeko.mibeko.getCurrentTimeMillis

/**
 * Sealed class representing the result of a search operation.
 */
sealed class SearchResult {
    data class Success(
        val articles: List<ArticleSpec>,
        val aiAnswer: String? = null,
        val isFromNetwork: Boolean
    ) : SearchResult()
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
        val lastSync = userPreferencesRepository.getLastSyncTimestamp()
        if (lastSync > 0) {
            // Convert timestamp to ISO string for backend
            val since = Instant.fromEpochMilliseconds(lastSync).toString()
            try {
                val serverTime = syncUpdates(since)
                if (serverTime != null) {
                    val newTimestamp = parseIsoDate(serverTime)
                    if (newTimestamp > 0) {
                        userPreferencesRepository.setLastSyncTimestamp(newTimestamp)
                    }
                }
                return
            } catch (e: Exception) {
                // If differential sync fails, fallback to full sync
                e.printStackTrace()
            }
        }

        var currentPage = 1
        var totalPages = 1
        var finalServerTime: String? = null

        while (currentPage <= totalPages) {
            val response = apiService.fetchDocuments(currentPage)
            totalPages = response.pagination?.last_page ?: 1
            
            // Capture server time from last page or metadata if available
            // Note: fetchDocuments might need to return server_time too in its meta
            // For now, if fetchDocuments meta is RemotePagination, it might not have server_time.
            // Let's assume we'll use current time if not provided.

            val documents = mutableListOf<DocumentEntity>()
            val allNodes = mutableListOf<NodeEntity>()
            val allArticles = mutableListOf<ArticleEntity>()

            // Get currently downloaded IDs to preserve state
            val downloadedIds = mibekoDao.getDownloadedDocumentIds().toSet()
            // Get favorite and offline IDs to preserve them
            val favoriteIds = mibekoDao.getFavoriteArticles().first().map { it.article.id }.toSet()
            val offlineIds = mibekoDao.getOfflineArticles().first().map { it.article.id }.toSet()

            for (remoteDoc in response.data) {
                // Map document
                documents.add(DocumentEntity(
                    id = remoteDoc.id,
                    title = remoteDoc.title,
                    type_code = remoteDoc.type?.code ?: "unknown",
                    last_updated = parseIsoDate(remoteDoc.updated_at),
                    is_downloaded = downloadedIds.contains(remoteDoc.id)
                ))

                // Fetch full tree for this document to get structure and articles
                val treeNodes = apiService.fetchDocumentTree(remoteDoc.id)
                flattenTree(remoteDoc.id, treeNodes, allNodes, allArticles, favoriteIds, offlineIds)
            }

            mibekoDao.syncAll(documents, allNodes, allArticles)
            currentPage++
        }
        
        // If it was a full sync, set timestamp to now
        userPreferencesRepository.setLastSyncTimestamp(getCurrentTimeMillis())
    }

    suspend fun syncUpdates(since: String): String? {
        val response = apiService.fetchUpdates(since)
        
        val updatedArticles = mutableListOf<ArticleEntity>()
        val updatedTags = mutableListOf<TagEntity>()
        val updatedArticleTags = mutableListOf<ArticleTagEntity>()

        // Get favorite and offline IDs to preserve them
        val favoriteIds = mibekoDao.getFavoriteArticles().first().map { it.article.id }.toSet()
        val offlineIds = mibekoDao.getOfflineArticles().first().map { it.article.id }.toSet()

        response.data.updated.forEach { remoteArticle ->
            updatedArticles.add(ArticleEntity(
                id = remoteArticle.id,
                node_id = remoteArticle.parent_node_id ?: "",
                number = remoteArticle.number,
                content = remoteArticle.content ?: "",
                is_favorite = favoriteIds.contains(remoteArticle.id),
                is_offline = offlineIds.contains(remoteArticle.id)
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
        
        return response.meta.server_time
    }

    private fun flattenTree(
        documentId: String,
        nodes: List<RemoteNode>,
        outNodes: MutableList<NodeEntity>,
        outArticles: MutableList<ArticleEntity>,
        favoriteIds: Set<String> = emptySet(),
        offlineIds: Set<String> = emptySet()
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
                    is_favorite = favoriteIds.contains(articleBrief.id),
                    is_offline = offlineIds.contains(articleBrief.id)
                ))
            }
        }
    }

    private fun parseIsoDate(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun downloadDocument(documentId: String) {
        val response = apiService.downloadDocument(documentId)
        val data = response.data
        
        val nodes = mutableListOf<NodeEntity>()
        val articles = mutableListOf<ArticleEntity>()

        // Map Nodes
        data.nodes.forEach { node ->
            nodes.add(NodeEntity(
                id = node.id,
                document_id = documentId,
                parent_id = null, // Backend should ideally provide parent info if hierarchical, or flat list needs checks
                title = node.title ?: "",
                sort_order = node.order
            ))
        }

        // Map Articles
        data.articles.forEach { article ->
            articles.add(ArticleEntity(
                id = article.id,
                node_id = article.parent_node_id ?: "",
                number = article.number,
                content = article.content ?: "",
                is_favorite = false // Will be ignored by Upsert if row exists? No, Upsert replaces.
                // Critical: We might lose favorite status if we upsert blind.
                // MibekoDao.upsertArticles uses REPLACE/UPSERT.
                // SQLite Upsert preserves if logic added, but Room @Upsert usually replaces.
                // We typically need to read favorite status or use partial update?
                // For MVP, simplistic approach: Assume sync brings fresh data. 
                // But Favorites MUST be preserved.
                // 'is_favorite' is in ArticleEntity.
                // We should probably NOT change is_favorite during download if it exists.
                // But room @Upsert overrides.
                // We need to implement careful Upsert in DAO or logic here.
                // Logic: is_favorite = false.
                // If it was true, it becomes false. BAD.
            ))
        }
        
        // FIXME: Handling Favorite preservation is tricky with full Upsert.
        // Option: In DAO, use: INSERT INTO ... ON CONFLICT(id) DO UPDATE SET content=excluded.content ... (not changing is_favorite)
        // For now, let's implement the basic flow and note the risk.
        // Actually, we can fetch existing favorites IDs first.
        
        val favoriteIds = mibekoDao.getFavoriteArticles().first().map { it.article.id }.toSet()
        val offlineIds = mibekoDao.getOfflineArticles().first().map { it.article.id }.toSet()
        
        val articlesWithStates = articles.map { 
            it.copy(
                is_favorite = favoriteIds.contains(it.id),
                is_offline = offlineIds.contains(it.id)
            )
        }

        mibekoDao.syncAll(
            documents = emptyList(), // Don't touch doc here or do we need to set is_downloaded?
            nodes = nodes,
            articles = articlesWithStates
        )
        
        mibekoDao.updateDocumentDownloadStatus(documentId, true)
    }

    suspend fun removeDownload(documentId: String) {
        mibekoDao.deleteNonFavoriteArticlesFromDocument(documentId)
        mibekoDao.updateDocumentDownloadStatus(documentId, false)
    }

    /**
     * Fetch a single document from the API and store it in the local database.
     */
    suspend fun fetchAndStoreDocument(documentId: String): com.mibeko.mibeko.data.LawCodeSpec? {
        return try {
            val response = apiService.fetchDocument(documentId)
            val remoteDoc = response.data ?: return null
            
            val document = DocumentEntity(
                id = remoteDoc.id,
                title = remoteDoc.title,
                type_code = remoteDoc.type?.code ?: "unknown",
                last_updated = parseIsoDate(remoteDoc.updated_at),
                is_downloaded = mibekoDao.getDownloadedDocumentIds().contains(remoteDoc.id)
            )
            
            mibekoDao.upsertDocuments(listOf(document))
            document.toLawCodeSpec()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch the full structure tree for a document from the API and store it locally.
     * This allows viewing the document even if not fully downloaded for offline use.
     */
    suspend fun fetchAndStoreDocumentStructure(documentId: String) {
        try {
            val treeNodes = apiService.fetchDocumentTree(documentId)
            
            val allNodes = mutableListOf<NodeEntity>()
            val allArticles = mutableListOf<ArticleEntity>()
            
            // Get favorite and offline IDs to preserve them
            val favoriteIds = mibekoDao.getFavoriteArticles().first().map { it.article.id }.toSet()
            val offlineIds = mibekoDao.getOfflineArticles().first().map { it.article.id }.toSet()
            
            flattenTree(documentId, treeNodes, allNodes, allArticles, favoriteIds, offlineIds)
            
            // Only sync nodes and articles, don't touch other documents
            mibekoDao.upsertNodes(allNodes)
            mibekoDao.upsertArticles(allArticles)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun getLawCodes(): Flow<List<com.mibeko.mibeko.data.LawCodeSpec>> {
        return mibekoDao.getAllDocuments().map { docs ->
            docs.map { it.toLawCodeSpec() }
        }
    }

    fun getDownloadedDocuments(): Flow<List<com.mibeko.mibeko.data.LawCodeSpec>> {
        return mibekoDao.getDownloadedDocuments().map { docs ->
            docs.map { it.toLawCodeSpec() }
        }
    }

    fun getFavoriteArticles(): Flow<List<ArticleSpec>> {
        return mibekoDao.getFavoriteArticles().map { results ->
            results.map { result ->
                result.toArticleSpec()
            }
        }
    }

    fun getOfflineArticles(): Flow<List<ArticleSpec>> {
        return mibekoDao.getOfflineArticles().map { results ->
            results.map { result ->
                result.toArticleSpec()
            }
        }
    }

    suspend fun toggleArticleOffline(article: ArticleSpec, isOffline: Boolean) {
        if (isOffline) {
            // Ensure the article and its node/document shell exist in DB
            val document = DocumentEntity(
                id = article.codeId,
                title = "Document", // We might not have the full title if from search
                type_code = "unknown",
                last_updated = getCurrentTimeMillis(),
                is_downloaded = false
            )
            val node = NodeEntity(
                id = article.id, // In this app, often article ID and node ID are related or same in some contexts, 
                                 // but let's check ArticleEntity.node_id
                document_id = article.codeId,
                parent_id = null,
                title = article.title,
                sort_order = 0
            )
            val entity = ArticleEntity(
                id = article.id,
                node_id = article.id, // Using article.id as node_id for simplicity if missing
                number = article.number,
                content = article.content,
                is_favorite = article.isFavorite,
                is_offline = true
            )
            
            mibekoDao.upsertDocuments(listOf(document))
            mibekoDao.upsertNodes(listOf(node))
            mibekoDao.upsertArticles(listOf(entity))
        } else {
            mibekoDao.updateArticleOfflineStatus(article.id, false)
        }
    }

    suspend fun updateArticleFavoriteStatus(articleId: String, isFavorite: Boolean) {
        // If the article doesn't exist locally (from network search), we need to create a shell
        val existing = mibekoDao.getArticleById(articleId).first()
        if (existing == null) {
            // This is a rare case where we favorite a network result not yet in DB
            // We'll need more info from ArticleSpec, but ViewModel handles it for now
            // For a robust implementation, we'd need to fetch or have full data
        } else {
            mibekoDao.updateArticleFavoriteStatus(articleId, isFavorite)
        }
    }

    suspend fun removeArticleDownload(articleId: String) {
        mibekoDao.updateArticleOfflineStatus(articleId, false)
        // Note: We don't necessarily delete the article from DB here 
        // because it might be part of a downloaded document or a favorite.
        // The deleteNonFavoriteArticlesFromDocument handles the cleanup of documents.
    }

    suspend fun getDocumentStats(): List<com.mibeko.mibeko.data.remote.DocumentStats> {
        val shouldUseNetwork = networkChecker.isNetworkAvailable() && 
                               !userPreferencesRepository.isOfflineModeEnabled()
        
        return if (shouldUseNetwork) {
            try {
                val response = apiService.fetchStats()
                response.data ?: emptyList()
            } catch (e: Exception) {
                // Fallback to local stats if offline
                // For MVP: Return empty or implement local count logic
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Fetch home page data from API.
     */
    /**
     * Fetch list of document types.
     */
    suspend fun getDocumentTypes(): List<com.mibeko.mibeko.data.remote.RemoteDocumentType> {
        val shouldUseNetwork = networkChecker.isNetworkAvailable() && 
                               !userPreferencesRepository.isOfflineModeEnabled()
        
        return if (shouldUseNetwork) {
            try {
                val response = apiService.fetchDocumentTypes()
                response.data ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun getHomeData(): ApiResponse<com.mibeko.mibeko.data.remote.RemoteHomeData> {
        return apiService.fetchHomeData()
    }

    fun getArticleById(id: String): Flow<ArticleSpec?> {
        return mibekoDao.getArticleById(id).map { result ->
            result?.toArticleSpec()
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
    suspend fun searchHybrid(query: String? = null, tag: String? = null): SearchResult {
        if (query == null && tag == null) {
            return SearchResult.Success(emptyList(), isFromNetwork = false)
        }

        val shouldUseNetwork = networkChecker.isNetworkAvailable() && 
                               !userPreferencesRepository.isOfflineModeEnabled()
        
        return if (shouldUseNetwork) {
            try {
                val response = apiService.searchArticles(query = query, tag = tag)
                // Map remote results. They are not downloaded, so isDownloaded = false default in Mapper.
                val articles = response.data.sources.map { it.toArticleSpec() }
                SearchResult.Success(
                    articles = articles,
                    aiAnswer = response.data.answer,
                    isFromNetwork = true
                )
            } catch (e: Exception) {
                // Network request failed, fallback to local search
                val localResults = searchLocally(query = query, tag = tag)
                SearchResult.Error(
                    message = "Erreur réseau: ${e.message ?: "Connexion impossible"}",
                    fallbackArticles = localResults
                )
            }
        } else {
            val localResults = searchLocally(query = query, tag = tag)
            SearchResult.Success(localResults, isFromNetwork = false)
        }
    }

    /**
     * Local-only search using Room database.
     * Used as primary when offline or as fallback when API fails.
     */
    private suspend fun searchLocally(query: String? = null, tag: String? = null): List<ArticleSpec> {
        return when {
            tag != null -> mibekoDao.searchArticlesByTag(tag).first().map { it.toArticleSpec() }
            query != null -> mibekoDao.searchArticles(query).first().map { it.toArticleSpec() }
            else -> emptyList()
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
                result.toArticleSpec()
            }
        }
    }

    fun getStructure(documentId: String): Flow<Map<NodeEntity, List<ArticleEntity>>> {
        return mibekoDao.getStructure(documentId)
    }

    /**
     * Get autocomplete suggestions for a query.
     * Uses hybrid approach: API if online, local if offline.
     * Returns ArticleSuggestion objects with IDs for direct navigation.
     */
    suspend fun getAutocompleteSuggestionsHybrid(query: String): List<com.mibeko.mibeko.data.ArticleSuggestion> {
        val shouldUseNetwork = networkChecker.isNetworkAvailable() && 
                               !userPreferencesRepository.isOfflineModeEnabled()
        
        return if (shouldUseNetwork) {
            try {
                val response = apiService.searchArticles(query)
                // Get local downloaded IDs to mark which are available offline
                val downloadedIds = mibekoDao.getDownloadedDocumentIds().toSet()
                
                response.data.sources.take(10).map { result ->
                    com.mibeko.mibeko.data.ArticleSuggestion(
                        id = result.id,
                        number = result.number,
                        breadcrumb = result.breadcrumb,
                        isDownloaded = downloadedIds.contains(result.document_id),
                        contentSnippet = result.content?.take(100)
                    )
                }
            } catch (e: Exception) {
                getAutocompleteSuggestionsLocally(query)
            }
        } else {
            getAutocompleteSuggestionsLocally(query)
        }
    }

    private suspend fun getAutocompleteSuggestionsLocally(query: String): List<com.mibeko.mibeko.data.ArticleSuggestion> {
        return mibekoDao.searchArticles(query).first().take(10).map { result ->
            com.mibeko.mibeko.data.ArticleSuggestion(
                id = result.article.id,
                number = result.article.number,
                breadcrumb = result.node_title,
                isDownloaded = result.doc_is_downloaded,
                contentSnippet = result.article.content?.take(100)
            )
        }
    }

    /**
     * Legacy Flow-based autocomplete for backward compatibility.
     * Always uses local Room database.
     */
    fun getAutocompleteSuggestions(query: String): Flow<List<com.mibeko.mibeko.data.ArticleSuggestion>> {
        return mibekoDao.searchArticles(query).map { results ->
            results.take(10).map { result ->
                com.mibeko.mibeko.data.ArticleSuggestion(
                    id = result.article.id,
                    number = result.article.number,
                    breadcrumb = result.node_title,
                    isDownloaded = result.doc_is_downloaded,
                    contentSnippet = result.article.content?.take(100)
                )
            }
        }
    }
}
