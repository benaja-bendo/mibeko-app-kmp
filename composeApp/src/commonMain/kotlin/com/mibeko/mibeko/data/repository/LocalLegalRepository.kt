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
import com.mibeko.mibeko.data.remote.SlugFetchResult
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

/**
 * Résultat de la résolution d'un lien public `mibeko.fr/textes/{slug}` :
 * la cible de navigation interne à ouvrir.
 */
sealed class SlugResolution {
    /** Ouvrir le lecteur sur un article précis. */
    data class Article(val articleId: String, val documentId: String) : SlugResolution()

    /** Ouvrir le détail du document (article inconnu ou lien document seul). */
    data class Document(val documentId: String) : SlugResolution()

    /** Slug syntaxiquement valide mais réellement inconnu du corpus publié (404). */
    data object NotFound : SlugResolution()

    /**
     * Résolution impossible à vérifier (réseau indisponible sans cache local,
     * panne serveur) — jamais confondu avec [NotFound] : l'app n'affirme
     * jamais qu'un texte n'existe pas quand elle n'a simplement pas pu
     * vérifier.
     */
    data object Failed : SlugResolution()
}

class LocalLegalRepository(
    private val mibekoDao: MibekoDao,
    private val apiService: LegalApiService,
    private val networkChecker: NetworkConnectivityChecker,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    /**
     * Synchronisation initiale du corpus, REPRENABLE.
     *
     * Chaque page est commitée puis le curseur avancé : si le réseau tombe au
     * milieu (fréquent au Congo), l'appel suivant reprend à la page qui a
     * échoué au lieu de tout reprendre — ou, pire, de ne jamais repartir comme
     * avant, ce qui figeait un corpus tronqué définitivement.
     */
    suspend fun sync() {
        // Le nombre total de pages n'est connu qu'APRÈS le premier appel : la
        // décision de continuer est donc prise page par page (cf. SyncPagination,
        // testé — une boucle à pré-test s'y était révélée inerte à la reprise).
        var currentPage = SyncPagination.startPage(userPreferencesRepository.getSyncCursorPage())

        while (true) {
            val response = apiService.fetchDocuments(currentPage)
            // Une réponse sans bloc de pagination trahit une erreur (429, 5xx)
            // désérialisée en enveloppe vide : la prendre pour « une seule
            // page » clôturerait la synchronisation sur un corpus tronqué en
            // la déclarant complète. On laisse remonter pour que le curseur
            // reste posé et que la reprise fasse son travail.
            val totalPages = response.pagination?.last_page
                ?: throw IllegalStateException("Réponse de catalogue sans pagination (page $currentPage)")

            when (val decision = SyncPagination.decide(currentPage, totalPages)) {
                is SyncPageDecision.Finished -> break
                is SyncPageDecision.RestartFromFirstPage -> {
                    currentPage = 1
                    userPreferencesRepository.setSyncCursorPage(1)
                    continue
                }
                is SyncPageDecision.Process -> Unit
            }

            val documents = mutableListOf<DocumentEntity>()
            val allNodes = mutableListOf<NodeEntity>()
            val allArticles = mutableListOf<ArticleEntity>()

            // Get currently downloaded IDs to preserve state
            val downloadedIds = mibekoDao.getDownloadedDocumentIds().toSet()
            // Métadonnées de catalogue déjà connues : l'upsert remplaçant la
            // ligne entière, il faut les reporter sous peine de les effacer.
            val knownMetadata = mibekoDao.getDocumentVersions().associateBy { it.id }
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
                    date_signature = remoteDoc.dates?.signature,
                    slug = remoteDoc.slug,
                    consolidation_as_of = knownMetadata[remoteDoc.id]?.consolidation_as_of,
                    version_hash = knownMetadata[remoteDoc.id]?.version_hash
                ))

                // Fetch full tree for this document to get structure and articles
                val treeNodes = apiService.fetchDocumentTree(remoteDoc.id)
                flattenTree(remoteDoc.id, treeNodes, allNodes, allArticles, favoriteIds, offlineIds)
            }

            mibekoDao.syncAll(documents, allNodes, allArticles)

            val processedPage = currentPage
            currentPage++
            // Curseur avancé APRÈS le commit : au pire on refait une page déjà
            // écrite (les écritures sont des upserts, donc sans effet de bord).
            userPreferencesRepository.setSyncCursorPage(currentPage)

            if (SyncPagination.isComplete(processedPage, totalPages)) break
        }

        // Parcours complet : plus rien à reprendre.
        userPreferencesRepository.setSyncCursorPage(0)
        userPreferencesRepository.setLastSyncTimestamp(getCurrentTimeMillis())

        // Le corpus vient d'être aligné : on mémorise l'état du catalogue pour
        // que le premier rafraîchissement ne re-télécharge pas tout.
        runCatching { alignCatalogVersions() }
    }

    /**
     * Rafraîchissement différentiel du corpus.
     *
     * Compare le catalogue serveur à l'état local et ne re-télécharge que les
     * textes réellement modifiés. Sans lui, un texte corrigé restait faux
     * indéfiniment sur les téléphones déjà installés.
     *
     * @param force ignore l'empreinte globale et recompare document par document.
     * @return le résultat, pour informer l'utilisateur.
     */
    suspend fun refreshCorpus(force: Boolean = false): CorpusRefreshResult {
        if (!networkChecker.isNetworkAvailable()) return CorpusRefreshResult.Offline

        // Mode hors-ligne demandé par l'utilisateur : ne pas consommer ses
        // données mobiles pour un rafraîchissement automatique. Un
        // déclenchement explicite (bouton des Réglages) reste prioritaire.
        if (!force && userPreferencesRepository.isOfflineModeEnabled()) {
            return CorpusRefreshResult.Offline
        }

        val catalog = try {
            apiService.fetchCatalog().data ?: return CorpusRefreshResult.Failed("Catalogue indisponible")
        } catch (e: Exception) {
            recordException(e, "refreshCorpus.fetchCatalog")
            return CorpusRefreshResult.Failed(e.message ?: "Connexion impossible")
        }

        // Empreinte globale inchangée : rien n'a bougé côté serveur.
        val remoteVersion = catalog.catalog_version
        if (!force && remoteVersion != null && remoteVersion == userPreferencesRepository.getCatalogVersion()) {
            userPreferencesRepository.setLastCorpusRefreshAt(getCurrentTimeMillis())
            return CorpusRefreshResult.UpToDate
        }

        val localById = mibekoDao.getDocumentVersions().associateBy { it.id }
        val localVersions = localById.mapValues { it.value.version_hash }
        val remoteById = catalog.resources.associateBy { it.id }

        // Documents dont l'empreinte est inconnue mais qui existent déjà en
        // base : c'est le cas de TOUT le corpus juste après la mise à jour de
        // l'app (la colonne `version_hash` vient d'être créée, donc vide). Les
        // déclarer périmés re-téléchargerait le corpus entier sur les données
        // mobiles de l'utilisateur. On retombe donc sur l'horodatage serveur,
        // déjà mémorisé lors de la synchronisation précédente : identique, le
        // texte n'a pas bougé et on se contente d'adopter son empreinte.
        val bootstrapIds = remoteById.values
            .filter { remote ->
                val local = localById[remote.id]
                local != null &&
                    local.version_hash == null &&
                    local.last_updated != 0L &&
                    local.last_updated == parseIsoDate(remote.last_updated)
            }
            .map { it.id }

        for (id in bootstrapIds) {
            val remote = remoteById[id] ?: continue
            runCatching {
                mibekoDao.applyCatalogMetadata(id, remote.version_hash, remote.consolidation_as_of, remote.slug)
            }
        }

        val staleIds = remoteById.values
            .filter { remote -> localVersions[remote.id] != remote.version_hash }
            .map { it.id }
            .filterNot { bootstrapIds.contains(it) }

        // Un document absent du catalogue n'est plus publié : on le retire du
        // corpus local. Garde-fou : jamais sur un catalogue vide, qui
        // trahirait une panne serveur plutôt qu'une dépublication de masse.
        val removedIds = if (catalog.resources.isEmpty()) {
            emptyList()
        } else {
            localVersions.keys.filterNot { remoteById.containsKey(it) }
        }

        var updated = 0
        for (id in staleIds) {
            val remote = remoteById[id] ?: continue
            // Un document jamais vu localement n'a pas encore sa ligne
            // `documents` : on la crée avant d'y accrocher nœuds et articles.
            if (!localVersions.containsKey(id)) {
                runCatching { fetchAndStoreDocument(id) }
            }
            val refreshed = runCatching { fetchAndStoreDocumentStructure(id) }.isSuccess
            if (refreshed) {
                runCatching { mibekoDao.applyCatalogMetadata(id, remote.version_hash, remote.consolidation_as_of, remote.slug) }
                updated++
            }
        }

        // Le retrait épargne les documents dont l'utilisateur a conservé des
        // articles : on ne compte donc que ce qui a réellement disparu.
        val actuallyRemoved = if (removedIds.isEmpty()) {
            emptyList()
        } else {
            runCatching { mibekoDao.removeDocuments(removedIds) }.getOrDefault(emptyList())
        }

        val completed = updated == staleIds.size
        if (completed) {
            // L'empreinte n'est mémorisée que si TOUT est passé : sinon le
            // prochain rafraîchissement croirait le corpus à jour.
            userPreferencesRepository.setCatalogVersion(remoteVersion)
        }
        userPreferencesRepository.setLastCorpusRefreshAt(getCurrentTimeMillis())

        return CorpusRefreshResult.Refreshed(
            updated = updated,
            removed = actuallyRemoved.size,
            partial = !completed
        )
    }

    /**
     * Aligne les empreintes locales sur le catalogue sans rien re-télécharger :
     * appelé juste après une synchronisation complète, où le corpus local est
     * par construction identique au serveur.
     */
    private suspend fun alignCatalogVersions() {
        if (!networkChecker.isNetworkAvailable()) return
        val catalog = apiService.fetchCatalog().data ?: return
        for (remote in catalog.resources) {
            mibekoDao.applyCatalogMetadata(remote.id, remote.version_hash, remote.consolidation_as_of, remote.slug)
        }

        // `applyCatalogMetadata` est un UPDATE ciblé : il ne touche aucune ligne
        // pour un document jamais téléchargé, silencieusement. Mémoriser
        // l'empreinte globale sans vérifier la complétude ferait conclure
        // « rien à faire » au prochain rafraîchissement et gèlerait un corpus
        // incomplet. Tant qu'il manque des textes, on laisse l'empreinte
        // absente : le diff par document les rattrapera.
        val localIds = mibekoDao.getDocumentVersions().map { it.id }.toSet()
        val isComplete = catalog.resources.all { localIds.contains(it.id) }
        if (isComplete) {
            userPreferencesRepository.setCatalogVersion(catalog.catalog_version)
        }
        userPreferencesRepository.setLastCorpusRefreshAt(getCurrentTimeMillis())
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
    /** @return false si rien n'a pu être mis en cache (document PDF-only, sans article structuré). */
    suspend fun downloadDocument(documentId: String): Boolean {
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

        // Un document PDF-only (aucun article structuré, ex. certains JO) n'a
        // rien de mis en cache ici — le PDF lui-même n'est jamais téléchargé
        // (voir PdfViewer). Le marquer « disponible hors-ligne » serait un
        // badge menteur : rien de consultable une fois hors connexion.
        if (articles.isNotEmpty()) {
            mibekoDao.updateDocumentDownloadStatus(documentId, true)
        }
        return articles.isNotEmpty()
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
            
            // `@Upsert` remplace la ligne entière : sans report explicite, on
            // effacerait l'empreinte de version (le document redeviendrait
            // « périmé » et serait re-téléchargé) et la date de consolidation
            // (le bandeau de provenance disparaîtrait du lecteur).
            val known = mibekoDao.getDocumentVersions().firstOrNull { it.id == remoteDoc.id }

            val document = DocumentEntity(
                id = remoteDoc.id,
                title = remoteDoc.title,
                type_code = remoteDoc.type?.code ?: "unknown",
                last_updated = parseIsoDate(remoteDoc.updated_at),
                is_downloaded = mibekoDao.getDownloadedDocumentIds().contains(remoteDoc.id),
                institution_name = remoteDoc.institution?.name,
                date_signature = remoteDoc.dates?.signature,
                slug = remoteDoc.slug,
                consolidation_as_of = known?.consolidation_as_of,
                version_hash = known?.version_hash
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

            // L'arbre reçu fait foi : ce qui n'y figure plus a été abrogé ou
            // supprimé côté serveur. Sans cet élagage, de simples upserts
            // laissaient l'ancien contenu en base — lisible et indexé — sur un
            // document pourtant marqué à jour.
            mibekoDao.pruneDocumentContent(
                documentId = documentId,
                keptArticleIds = allArticles.map { it.id },
                keptNodeIds = allNodes.map { it.id }
            )
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
            // Document parent : l'ancienne coquille title="Document" ÉCRASAIT
            // (upsert) les métadonnées d'un document déjà présent et polluait
            // l'écran Téléchargements. On préserve l'existant, on tente les
            // vraies métadonnées via l'API, et la coquille ne reste qu'en
            // dernier recours hors-ligne.
            val existingDocument = mibekoDao.getDocumentById(article.codeId).firstOrNull()
            if (existingDocument == null) {
                val fetched = runCatching { fetchAndStoreDocument(article.codeId) }.getOrNull()
                if (fetched == null) {
                    mibekoDao.upsertDocuments(
                        listOf(
                            DocumentEntity(
                                id = article.codeId,
                                title = article.title.ifEmpty { "Document" },
                                type_code = article.typeCode.ifEmpty { "unknown" },
                                last_updated = getCurrentTimeMillis(),
                                is_downloaded = false,
                                institution_name = null,
                                date_signature = null
                            )
                        )
                    )
                }
            }
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
     * Résout un lien public `mibeko.fr/textes/{slug}` (et éventuellement
     * `.../article-{numero}`) en cibles de navigation internes.
     *
     * Le slug est d'abord cherché parmi les documents déjà en cache (chemin
     * hors-ligne), puis via l'endpoint public par slug si nécessaire. En cas de
     * succès, le document est stocké localement (le lecteur lit depuis Room) et
     * un [SlugResolution] indique quoi ouvrir : l'article demandé si son numéro
     * a pu être résolu, sinon le document parent. [SlugResolution.NotFound] si
     * le slug est réellement inconnu (404) ; [SlugResolution.Failed] si la
     * résolution n'a simplement pas pu être vérifiée (hors-ligne sans cache,
     * panne réseau/serveur) — jamais confondus, l'app n'affirme jamais qu'un
     * texte n'existe pas quand elle n'a pas pu vérifier.
     */
    suspend fun resolveTexteBySlug(slug: String, articleNumber: String?): SlugResolution {
        // 1) Cache local : le document est-il déjà connu par son slug ?
        val cached = mibekoDao.getAllDocuments().firstOrNull()
            ?.firstOrNull { it.slug == slug }
        var documentId = cached?.id

        // 2) Sinon, résolution réseau via l'endpoint public par slug.
        var remoteArticles: List<com.mibeko.mibeko.data.remote.RemotePublicArticleRef> = emptyList()
        val canUseNetwork = networkChecker.isNetworkAvailable() &&
            !userPreferencesRepository.isOfflineModeEnabled()

        if (documentId == null) {
            if (!canUseNetwork) {
                // Hors-ligne (ou mode forcé) et rien en cache : impossible de
                // vérifier — surtout pas « introuvable ».
                return SlugResolution.Failed
            }
            when (val result = apiService.fetchDocumentBySlug(slug)) {
                is SlugFetchResult.Found -> {
                    documentId = result.document.id
                    remoteArticles = result.articles
                    // Stocke le document + sa structure pour une lecture depuis Room.
                    fetchAndStoreDocument(result.document.id)
                    runCatching { fetchAndStoreDocumentStructure(result.document.id) }
                }
                SlugFetchResult.NotFound -> return SlugResolution.NotFound
                SlugFetchResult.Failed -> return SlugResolution.Failed
            }
        }

        // documentId est forcément non-null ici : soit trouvé en cache, soit
        // posé par la branche Found ci-dessus (les deux autres retournent).
        val resolvedDocId = documentId

        // Pas de numéro d'article demandé → on ouvre le document.
        if (articleNumber.isNullOrBlank()) {
            return SlugResolution.Document(resolvedDocId)
        }

        // Résolution de l'article : d'abord via l'index renvoyé par l'API, puis
        // via le cache local (document déjà téléchargé).
        val articleId = remoteArticles.firstOrNull { it.number == articleNumber }?.id
            ?: mibekoDao.getArticleIdByDocumentAndNumber(resolvedDocId, articleNumber)

        return if (articleId != null) {
            SlugResolution.Article(articleId, resolvedDocId)
        } else {
            // Numéro introuvable : on retombe sur le document parent (règle P1.11).
            SlugResolution.Document(resolvedDocId)
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
        return try {
            when {
                tag != null -> mibekoDao.searchArticlesByTag(tag).first().map { it.toArticleSpec() }
                query != null -> {
                    val sanitized = sanitizeFtsQuery(query)
                    if (sanitized.isEmpty()) {
                        emptyList()
                    } else {
                        mibekoDao.searchArticles(sanitized).first().map { it.toArticleSpec() }
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            // Une requête FTS invalide ne doit jamais faire tomber la
            // recherche hors-ligne : zéro résultat plutôt qu'un crash.
            recordException(e, context = "LocalLegalRepository.searchLocally")
            emptyList()
        }
    }

    companion object {
        /**
         * La saisie utilisateur est passée telle quelle à `MATCH`, dont la
         * syntaxe (guillemets, `-`, `*`, parenthèses) peut lever une
         * SQLiteException. On neutralise en encadrant chaque terme de
         * guillemets (AND implicite entre les termes).
         */
        internal fun sanitizeFtsQuery(raw: String): String {
            return raw.split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .joinToString(" ") { token -> "\"" + token.replace("\"", "\"\"") + "\"" }
        }
    }

    fun getStructure(documentId: String): Flow<Map<NodeEntity, List<ArticleEntity>>> {
        return mibekoDao.getStructure(documentId)
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
        // La base repart de zéro : laisser un curseur de reprise ou une
        // empreinte de catalogue ferait croire à une synchronisation en cours
        // (ou terminée) sur une bibliothèque vide.
        userPreferencesRepository.setSyncCursorPage(0)
        userPreferencesRepository.setCatalogVersion(null)
        userPreferencesRepository.setLastCorpusRefreshAt(0L)
        userPreferencesRepository.setLastSyncTimestamp(0L)
    }
}

/** Issue d'un rafraîchissement différentiel du corpus. */
sealed class CorpusRefreshResult {
    /** Le catalogue n'a pas bougé depuis la dernière fois. */
    data object UpToDate : CorpusRefreshResult()

    /** Pas de réseau : l'utilisateur garde son corpus local. */
    data object Offline : CorpusRefreshResult()

    data class Refreshed(
        val updated: Int,
        val removed: Int,
        /** Vrai si certains textes n'ont pas pu être récupérés. */
        val partial: Boolean
    ) : CorpusRefreshResult()

    data class Failed(val reason: String) : CorpusRefreshResult()
}
