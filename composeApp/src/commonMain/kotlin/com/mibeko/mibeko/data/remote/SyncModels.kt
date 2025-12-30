package com.mibeko.mibeko.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RemoteDocumentResponse(
    val data: List<RemoteDocument>,
    val links: Map<String, String?>,
    val meta: RemoteMeta
)

@Serializable
data class RemoteMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int
)

@Serializable
data class RemoteDocument(
    val id: String,
    val title: String,
    val reference: String?,
    val status: String,
    val type: RemoteDocumentType,
    val structure: List<RemoteNode> = emptyList(),
    val articles: List<RemoteArticle> = emptyList(),
    val updated_at: String
)

@Serializable
data class RemoteDocumentType(
    val code: String,
    val name: String
)

@Serializable
data class RemoteNode(
    val id: String,
    val type: String,
    val number: String?,
    val title: String?,
    val order: Int,
    val articles: List<RemoteArticleBrief> = emptyList()
)

@Serializable
data class RemoteArticleBrief(
    val id: String,
    val number: String,
    val order: Int,
    val content: String? = null,
    val validation_status: String
)

@Serializable
data class RemoteArticle(
    val id: String,
    val document_id: String,
    val parent_node_id: String,
    val number: String,
    val order: Int, 
    val content: String? = null,
    val tags: List<String> = emptyList(),
    val updated_at: String
)

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

/**
 * Response from the article search endpoint.
 */
@Serializable
data class RemoteSearchResponse(
    val data: List<RemoteSearchResult>,
    val meta: RemoteMeta
)

/**
 * Individual search result from the API.
 */
@Serializable
data class RemoteSearchResult(
    val id: String,
    val number: String,
    val content: String,
    val document_id: String,
    val document_title: String?,
    val document_type: String?,
    val node_title: String?,
    val breadcrumb: String
)

