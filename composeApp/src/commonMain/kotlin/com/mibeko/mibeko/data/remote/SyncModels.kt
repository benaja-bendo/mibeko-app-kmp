package com.mibeko.mibeko.data.remote

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
    val message: String,
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
    val resources: List<RemoteCatalogResource>
)

/**
 * Individual resource in the catalog.
 */
@Serializable
data class RemoteCatalogResource(
    val id: String,
    val title: String,
    val type: String,
    val version_hash: String,
    val last_updated: String,
    val download_size_kb: Int
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
    val total: Int
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
    val reference: String? = null,
    val status: String,
    val type: RemoteDocumentType? = null,
    val institution: RemoteInstitution? = null,
    val dates: RemoteDocumentDates? = null,
    val structure: List<RemoteNode> = emptyList(),
    val articles: List<RemoteArticle> = emptyList(),
    val relations: List<RemoteDocumentRelation> = emptyList(),
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
    val type: String,
    val number: String? = null,
    val title: String? = null,
    val order: Int,
    val articles: List<RemoteArticleBrief> = emptyList()
)

@Serializable
data class RemoteArticleBrief(
    val id: String,
    val number: String,
    val order: Int,
    val content: String? = null,
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
    val tags: List<String> = emptyList(),
    val updated_at: String
)

// =============================================================================
// Sync Models
// =============================================================================

@Serializable
data class RemoteSyncResponse(
    val data: RemoteSyncData,
    val meta: RemoteMeta
)

@Serializable
data class RemoteSyncData(
    val updated: List<RemoteArticle>,
    val deleted_ids: List<String>
)

@Serializable
data class RemoteTreeResponse(
    val data: List<RemoteNode>
)

// =============================================================================
// Search Models
// =============================================================================

/**
 * Response from the article search endpoint.
 * Uses the new API wrapper format.
 */
@Serializable
data class RemoteSearchResponse(
    val success: Boolean = true,
    val message: String = "",
    val data: List<RemoteSearchResult> = emptyList(),
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
