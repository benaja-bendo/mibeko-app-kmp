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
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.mibeko.mibeko.getCurrentTimeMillis
import kotlin.time.Instant as DateTimeInstant

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
        var currentPage = 1
        var totalPages = 1

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
            val favoriteIds = mibekoDao.getFavoriteArticles().firstOrNull()?.map { it.article.id }?.toSet() ?: emptySet()
            val offlineIds = mibekoDao.getOfflineArticles().firstOrNull()?.map { it.article.id }?.toSet() ?: emptySet()

            for (remoteDoc in response.data) {
                // Map document
                documents.add(DocumentEntity(
                    id = remoteDoc.id,
                    title = remoteDoc.title,
                    type_code = remoteDoc.type?.code ?: "unknown",
                    last_updated = parseIsoDate(remoteDoc.updated_at),
                    is_downloaded = downloadedIds.contains(remoteDoc.id),
                    institution_name = remoteDoc.institution?.name,
                    date_signature = remoteDoc.dates?.signature
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

    private fun flattenTree(
        documentId: String,
        nodes: List<RemoteNode>,
        outNodes: MutableList<NodeEntity>,
        outArticles: MutableList<ArticleEntity>,
        favoriteIds: Set<String> = emptySet(),
        offlineIds: Set<String> = emptySet()
    ) {
        // Actes courts (arrêtés/décrets issus d'un JO) : l'API renvoie leurs
        // articles comme pseudo-nœuds `type == "ARTICLE"` à la racine. On les
        // regroupe sous un nœud racine synthétique, sinon la structure locale
        // reste vide et l'écran retombe sur le PDF.
        val orphanArticles = nodes.filter { it.type == "ARTICLE" }
        if (orphanArticles.isNotEmpty()) {
            val rootId = syntheticRootNodeId(documentId)
            outNodes.add(NodeEntity(
                id = rootId,
                document_id = documentId,
                parent_id = null,
                title = "",
                sort_order = 0
            ))
            orphanArticles.forEach { node ->
                outArticles.add(ArticleEntity(
                    id = node.id,
                    node_id = rootId,
                    number = node.number ?: "",
                    content = node.content ?: "",
                    is_favorite = favoriteIds.contains(node.id),
                    is_offline = offlineIds.contains(node.id)
                ))
            }
        }

        nodes.filter { it.type != "ARTICLE" }.forEach { node ->
            outNodes.add(NodeEntity(
                id = node.id,
                document_id = documentId,
                parent_id = node.parent_id,
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

    /** Nœud local porteur des articles sans structure (un par document). */
    private fun syntheticRootNodeId(documentId: String) = "root_$documentId"

    private fun parseIsoDate(isoString: String?): Long {
        if (isoString == null) return 0L
        return try {
            DateTimeInstant.parse(isoString).toEpochMilliseconds()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Download a full document (structure + articles) for offline use.
     * Crucially preserves the 'is_favorite' and 'is_offline' status of existing articles 
     * by querying their current IDs before performing the bulk upsert.
     */
    suspend fun downloadDocument(documentId: String) {
        val response = apiService.downloadDocument(documentId)
        val data = response.data
        
        val favoriteIds = mibekoDao.getFavoriteArticleIds().toSet()
        val offlineIds = mibekoDao.getOfflineArticleIds().toSet()

        val nodes = data.nodes.map { node ->
            NodeEntity(
                id = node.id,
                document_id = documentId,
                parent_id = node.parent_id,
                title = node.title ?: "",
                sort_order = node.order
            )
        }.toMutableList()

        // Les articles sans structure (actes courts) sont rattachés à un nœud
        // racine synthétique : `node_id = ""` violerait la clé étrangère vers
        // `nodes` et ferait échouer l'insertion de tout le lot.
        val rootId = syntheticRootNodeId(documentId)
        if (data.articles.any { it.parent_node_id == null }) {
            nodes.add(NodeEntity(
                id = rootId,
                document_id = documentId,
                parent_id = null,
                title = "",
                sort_order = 0
            ))
        }

        val articles = data.articles.map { article ->
            ArticleEntity(
                id = article.id,
                node_id = article.parent_node_id ?: rootId,
                number = article.number,
                content = article.content ?: "",
                is_favorite = favoriteIds.contains(article.id),
                is_offline = offlineIds.contains(article.id)
            )
        }

        mibekoDao.syncAll(
            documents = emptyList(),
            nodes = nodes,
            articles = articles
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
                is_downloaded = mibekoDao.getDownloadedDocumentIds().contains(remoteDoc.id),
                institution_name = remoteDoc.institution?.name,
                date_signature = remoteDoc.dates?.signature
            )
            
            mibekoDao.upsertDocuments(listOf(document))
            document.toLawCodeSpec()
        } catch (e: Exception) {
            recordException(e, context = "LocalLegalRepository.fetchAndStoreDocument")
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
            val favoriteIds = mibekoDao.getFavoriteArticleIds().toSet()
            val offlineIds = mibekoDao.getOfflineArticleIds().toSet()
            
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

    fun getLawCodeById(id: String): Flow<com.mibeko.mibeko.data.LawCodeSpec?> {
        return mibekoDao.getDocumentById(id).map { it?.toLawCodeSpec() }
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
                type_code = article.typeCode.ifEmpty { "unknown" },
                last_updated = getCurrentTimeMillis(),
                is_downloaded = false,
                institution_name = null,
                date_signature = null
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
        val existing = mibekoDao.getArticleById(articleId).firstOrNull()
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
                // On remonte l'erreur (elle était avalée) avant le fallback :
                // distingue une vraie panne d'un simple hors-ligne.
                recordException(e, context = "LocalLegalRepository.getDocumentStats")
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
        
        if (shouldUseNetwork) {
            try {
                val response = apiService.fetchDocumentTypes()
                if (response.success && response.data != null) {
                    return response.data
                }
            } catch (e: Exception) {
                // Fallback to local — mais on remonte l'erreur avalée.
                recordException(e, context = "LocalLegalRepository.getDocumentTypes")
            }
        }

        // Fallback or offline: Get unique types from local DB
        return mibekoDao.getUniqueDocumentTypeCodes().map { code ->
            com.mibeko.mibeko.data.remote.RemoteDocumentType(
                code = code,
                name = formatTypeCode(code)
            )
        }
    }

    private fun formatTypeCode(code: String): String {
        return when (code) {
            "CODE" -> "Codes"
            "LOI" -> "Lois"
            "DEC" -> "Décrets"
            "ARR" -> "Arrêtés"
            "CONST" -> "Constitution"
            "LOI_ORG" -> "Lois Organiques"
            "ORD" -> "Ordonnances"
            "CIRC" -> "Circulaires"
            "JUR" -> "Jurisprudence"
            else -> code.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    suspend fun getHomeData(): ApiResponse<com.mibeko.mibeko.data.remote.RemoteHomeData> {
        return apiService.fetchHomeData()
    }

    /**
     * Fetch paginated list of Official Journals directly from API
     */
    suspend fun getOfficialJournals(page: Int = 1, number: String? = null, year: Int? = null, perPage: Int = 15): com.mibeko.mibeko.data.remote.RemoteOfficialJournalResponse {
        return apiService.fetchOfficialJournals(page, number, year, perPage)
    }

    /**
     * Fetch a specific Official Journal with its documents directly from API
     */
    suspend fun getOfficialJournal(id: String): com.mibeko.mibeko.data.remote.RemoteOfficialJournal {
        return apiService.fetchOfficialJournal(id)
    }

    fun getArticleById(id: String): Flow<ArticleSpec?> {
        return mibekoDao.getArticleById(id).map { result ->
            result?.toArticleSpec()
        }
    }

    /**
     * Garantit qu'un article est lisible localement. Un résultat de recherche
     * peut pointer vers un document jamais téléchargé : on résout alors le
     * document parent via l'API puis on stocke sa structure complète (ce qui
     * alimente aussi la navigation précédent/suivant et le sommaire du lecteur).
     *
     * @return true si l'article est disponible dans le cache local à l'issue.
     */
    suspend fun ensureArticleAvailable(articleId: String): Boolean {
        if (mibekoDao.getArticleById(articleId).firstOrNull() != null) {
            return true
        }

        val canUseNetwork = networkChecker.isNetworkAvailable() &&
            !userPreferencesRepository.isOfflineModeEnabled()
        if (!canUseNetwork) {
            return false
        }

        return try {
            val context = apiService.fetchArticleContext(articleId) ?: return false
            fetchAndStoreDocument(context.document_id)
            fetchAndStoreDocumentStructure(context.document_id)
            mibekoDao.getArticleById(articleId).firstOrNull() != null
        } catch (e: Exception) {
            recordException(e, context = "LocalLegalRepository.ensureArticleAvailable")
            false
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
                // Network request failed, fallback to local search.
                recordException(e, context = "LocalLegalRepository.search")
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
                recordException(e, context = "LocalLegalRepository.getAutocompleteSuggestions")
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
    fun getArticleExportUrl(articleId: String): String {
        return apiService.getArticleExportUrl(articleId)
    }

    fun getDocumentExportUrl(documentId: String): String {
        return apiService.getDocumentExportUrl(documentId)
    }

    fun getOfficialJournalPdfUrl(id: String): String {
        return apiService.getOfficialJournalPdfUrl(id)
    }

    suspend fun downloadFile(url: String): ByteArray {
        return apiService.downloadFile(url)
    }

    /**
     * Envoie un signalement d'erreur pour un document ou un article.
     */
    suspend fun reportError(
        documentId: String?,
        articleId: String?,
        type: String,
        description: String
    ) {
        apiService.reportError(documentId, articleId, type, description)
    }

    /**
     * Clear all data from the local database.
     */
    suspend fun clearAllData() {
        mibekoDao.clearAllData()
    }
}
