package com.mibeko.mibeko.data.remote

import com.mibeko.mibeko.util.ArticleTable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Generic API Response Wrappers (New API Structure)
// =============================================================================

/**
 * Standard API response wrapper for single objects.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    /**
     * Nullable ET optionnel : plusieurs endpoints répondent sans message
     * (`success($data)` sans second argument renvoie `"message": null`). Un
     * type non-nullable faisait échouer toute la désérialisation, et l'erreur,
     * avalée par les appelants, vidait silencieusement la réponse.
     */
    val message: String? = null,
    val data: T? = null
)

/**
 * Paginated API response wrapper.
 */
@Serializable
data class PaginatedApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: List<T>,
    val pagination: RemotePagination
)

/**
 * Pagination metadata from API.
 */
@Serializable
data class RemotePagination(
    val total: Int,
    val per_page: Int,
    val current_page: Int,
    val last_page: Int
)

// =============================================================================
// Catalog Models (for sync status)
// =============================================================================

/**
 * Catalog response containing sync status and available resources.
 */
@Serializable
data class RemoteCatalogData(
    val global_update_required: Boolean,
    val last_essential_sync: String,
    val resources: List<RemoteCatalogResource>,
    /**
     * Empreinte de tout le catalogue : inchangée depuis la dernière
     * synchronisation, il n'y a rien à faire. Valeurs par défaut pour rester
     * compatible d'un serveur plus ancien.
     */
    val catalog_version: String? = null,
    /** Horloge du serveur, préférée à celle de l'appareil pour horodater. */
    val server_time: String? = null
)

/**
 * Individual resource in the catalog.
 */
@Serializable
data class RemoteCatalogResource(
    val id: String,
    val title: String,
    /**
     * Objet de l'acte dérivé de son corps, pour les « actes en abrégé » du JO
     * dont le titre se réduit au type, au numéro et à la date. Champ ADDITIF :
     * absent des réponses d'une API antérieure au 16/08/2026, d'où le défaut.
     * S'affiche à côté du titre, jamais à sa place.
     */
    val descriptive_label: String? = null,
    val type: String,
    /**
     * Empreinte de version du document : mêle son `updated_at`, celui de son
     * article le plus récent et son nombre d'articles. C'est le signal de
     * « ce texte a changé » — une correction d'article la fait bouger même
     * quand la ligne du document n'a pas été touchée.
     */
    val version_hash: String,
    val last_updated: String,
    val download_size_kb: Int,
    val slug: String? = null,
    /** « À jour au » d'un texte consolidé ; null pour un acte unitaire. */
    val consolidation_as_of: String? = null,
    val articles_count: Int = 0
)

// =============================================================================
// Document Models
// =============================================================================

/**
 * Legacy response format for backwards compatibility.
 * Some endpoints may still use this format.
 */
@Serializable
data class RemoteDocumentResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: List<RemoteDocument> = emptyList(),
    val pagination: RemotePagination? = null,
    // Legacy fields for backwards compatibility
    val links: Map<String, String?> = emptyMap(),
    val meta: RemoteMeta? = null
)

@Serializable
data class RemoteMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int,
    val server_time: String? = null
)

@Serializable
data class DocumentStats(
    val type_name: String,
    val type_code: String,
    val count: Int
)

@Serializable
data class RemoteDocument(
    val id: String,
    val title: String,
    /**
     * Objet de l'acte DÉRIVÉ de son corps (`legal_documents.libelle_descriptif`
     * côté Laravel), pour les textes que le Journal officiel publie en « actes
     * en abrégé » : le titre s'y réduit au type, au numéro et à la date.
     * S'affiche à côté du titre, jamais à sa place. Champ ADDITIF, `null` sur
     * une API antérieure au 16/08/2026 comme sur la plupart des documents.
     */
    val libelle_descriptif: String? = null,
    /**
     * Slug d'URL publique stable (site vitrine `/textes/{slug}`). Exposé par
     * `LegalDocumentResource` côté Laravel ; sert à générer les liens de partage
     * vers mibeko.fr. `null` pour les documents jamais publiés/slugués.
     */
    val slug: String? = null,
    val reference: String? = null,
    val status: String,
    val type: RemoteDocumentType? = null,
    val type_code: String? = null,
    val institution: RemoteInstitution? = null,
    val dates: RemoteDocumentDates? = null,
    val structure: List<RemoteNode> = emptyList(),
    val articles: List<RemoteArticle> = emptyList(),
    val relations: List<RemoteDocumentRelation> = emptyList(),
    /** Renseigné quand le contrôleur fait un withCount (détail d'un JO). */
    val articles_count: Int? = null,
    val updated_at: String
)

@Serializable
data class RemoteDocumentType(
    val code: String,
    val name: String
)

@Serializable
data class RemoteInstitution(
    val id: String,
    val name: String,
    val acronym: String? = null
)

@Serializable
data class RemoteDocumentDates(
    val signature: String? = null,
    val publication: String? = null
)

@Serializable
data class RemoteDocumentRelation(
    val id: String,
    val source_document_id: String? = null,
    val target_document_id: String? = null,
    val relation_type: String,
    val comment: String? = null
)

// =============================================================================
// Structure Node Models
// =============================================================================

@Serializable
data class RemoteNode(
    val id: String,
    val parent_id: String? = null,
    val type: String,
    val number: String? = null,
    val title: String? = null,
    val order: Int,
    val articles: List<RemoteArticleBrief> = emptyList(),
    /**
     * Présent uniquement pour les pseudo-nœuds `type == "ARTICLE"` : les actes
     * courts (arrêtés/décrets issus d'un JO) n'ont pas de structure, l'API
     * renvoie alors leurs articles directement à la racine de l'arbre.
     */
    val content: String? = null,
    /** Idem : tableaux du pseudo-nœud ARTICLE, quand il en porte. */
    val tables: List<ArticleTable> = emptyList()
)

@Serializable
data class RemoteArticleContextResponse(
    val success: Boolean = false,
    val message: String = "",
    val data: RemoteArticleContext? = null
)

// =============================================================================
// Public-by-slug Models (deep links mibeko.fr/textes/{slug})
// =============================================================================

/**
 * Réponse de `GET /v1/legal-documents/slug/{slug}` (vue publique par slug).
 * Utilisée uniquement pour résoudre un lien profond mibeko.fr en identifiants
 * internes (document + article) : on ne conserve que l'essentiel.
 */
@Serializable
data class RemotePublicDocumentResponse(
    val success: Boolean = false,
    val message: String = "",
    val data: RemotePublicDocumentData? = null
)

@Serializable
data class RemotePublicDocumentData(
    val document: RemoteDocument? = null,
    /** Index léger des articles du document (id + numéro), pour la résolution. */
    val articles: List<RemotePublicArticleRef> = emptyList()
)

@Serializable
data class RemotePublicArticleRef(
    val id: String,
    val number: String,
    val order: Int = 0
)

/**
 * Réponse de `GET /v1/articles/{id}/context` : résout le document parent d'un
 * article isolé (résultat de recherche d'un document jamais téléchargé).
 */
@Serializable
data class RemoteArticleContext(
    val id: String,
    val document_id: String,
    val number: String = "",
    val content: String? = null
)

@Serializable
data class RemoteArticleBrief(
    val id: String,
    val number: String,
    val order: Int,
    val content: String? = null,
    /** Tableaux structurés de l'article (cf. `util/LegalTables.kt`). */
    val tables: List<ArticleTable> = emptyList(),
    val validation_status: String = "validated"
)

// =============================================================================
// Article Models
// =============================================================================

@Serializable
data class RemoteArticle(
    val id: String,
    val document_id: String,
    val parent_node_id: String? = null,
    val number: String,
    val order: Int,
    val content: String? = null,
    /** Feuille spéciale posée à l'ingestion : `preamble`, `signature`, `table`. */
    val content_format: String? = null,
    /** Tableaux structurés de l'article (cf. `util/LegalTables.kt`). */
    val tables: List<ArticleTable> = emptyList(),
    val tags: List<String> = emptyList(),
    val updated_at: String
)



// =============================================================================
// Search Models
// =============================================================================

/**
 * Response from the article search endpoint.
 * Supports RAG (AI Answer + Sources).
 */
@Serializable
data class RemoteSearchResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: RemoteSearchData
)

@Serializable
data class RemoteSearchData(
    val answer: String? = null,
    val sources: List<RemoteSearchResult> = emptyList(),
    val pagination: RemotePagination? = null
)

/**
 * Individual search result from the API.
 * Matches ArticleResource schema from doc_api.json.
 */
@Serializable
data class RemoteSearchResult(
    val id: String,
    val number: String,
    val order: Int = 0,
    val content: String? = null,
    val document_id: String,
    val document_title: String = "",
    val document_type: String = "",
    val node_title: String = "",
    val breadcrumb: String = "",
    val validation_status: String = "validated",
    val score: Double? = null  // Relevance score from hybrid search
)

// =============================================================================
// Home Models
// =============================================================================

@Serializable
data class RemoteHomeData(
    val popular_codes: List<RemoteDocument>,
    val recently_added: List<RemoteDocument>,
    val ai_suggestions: List<String>
)

// =============================================================================
// Download Models
// =============================================================================

@Serializable
data class RemoteDownloadResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: RemoteDownloadData
)

@Serializable
data class RemoteDownloadData(
    val resource_id: String,
    val node_id: String? = null,
    val generated_at: String = "",
    val nodes: List<RemoteNode> = emptyList(),
    val articles: List<RemoteArticle> = emptyList()
)

// =============================================================================
// Export Models
// =============================================================================

@Serializable
data class DossierExportRequest(
    val title: String,
    val description: String? = null,
    val items: List<DossierExportItem>
)

@Serializable
data class DossierExportItem(
    val type: String,
    val id: String,
    val note: String? = null
)

// =============================================================================
// Dossier Sync Models
// =============================================================================

/**
 * Dossier au format d'échange avec l'API (push et pull).
 * Les horodatages sont en epoch millisecondes (horloge client).
 */
@Serializable
data class RemoteDossier(
    val id: String,
    val name: String,
    val legal_domain: String = "Général",
    val tag: String = "EN_COURS",
    val description: String? = null,
    val color: String = "#1B3D2F",
    val created_at: Long = 0L,
    val updated_at: Long,
    val articles: List<RemoteDossierArticle> = emptyList()
)

@Serializable
data class RemoteDossierArticle(
    val article_id: String,
    val personal_note: String? = null,
    val added_at: Long = 0L
)

/** Corps de la requête POST /dossiers/sync : état local complet. */
@Serializable
data class DossierSyncRequest(
    val dossiers: List<RemoteDossier>,
    val deleted_ids: List<String> = emptyList()
)

/** État autoritaire renvoyé par le serveur après fusion. */
@Serializable
data class DossierSyncData(
    val dossiers: List<RemoteDossier> = emptyList(),
    val deleted_ids: List<String> = emptyList(),
    val synced_at: Long = 0L
)

// =============================================================================
// Official Journal Models
// =============================================================================

@Serializable
data class RemoteOfficialJournalResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: List<RemoteOfficialJournal> = emptyList(),
    val meta: RemoteMeta? = null,
    val links: Map<String, String?> = emptyMap()
)

@Serializable
data class RemoteOfficialJournalSingleResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: RemoteOfficialJournal? = null
)

@Serializable
data class RemoteOfficialJournal(
    val id: String,
    val title: String,
    val number: String? = null,
    val publication_date: String,
    val transcription_status: String,
    val is_published: Boolean,
    val pdf_url: String? = null,
    val file_size_bytes: Long? = null,
    /** Nombre de textes publiés rattachés (présent sur la liste uniquement). */
    val legal_documents_count: Int? = null,
    val legal_documents: List<RemoteDocument> = emptyList(),
    val created_at: String,
    val updated_at: String
)

/**
 * Contrat de GET /v1/app-config (force-update).
 * `store_urls.ios` peut être null. Tout champ illisible = fail-open côté
 * client (VersionGate).
 */
@Serializable
data class AppConfigResponse(
    val min_supported_version: String,
    val latest_version: String,
    val message: String? = null,
    val store_urls: AppStoreUrls = AppStoreUrls()
)

@Serializable
data class AppStoreUrls(
    val android: String? = null,
    val ios: String? = null
)
