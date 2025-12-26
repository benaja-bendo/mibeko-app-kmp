package com.mibeko.mibeko.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(
    val documents: List<RemoteDocument>,
    val nodes: List<RemoteNode>,
    val articles: List<RemoteArticle>
)

@Serializable
data class RemoteDocument(
    val id: String,
    val title: String,
    val type_code: String,
    val last_updated: Long
)

@Serializable
data class RemoteNode(
    val id: String,
    val document_id: String,
    val parent_id: String?,
    val title: String,
    val sort_order: Int
)

@Serializable
data class RemoteArticle(
    val id: String,
    val node_id: String,
    val number: String,
    val content: String,
    val is_favorite: Boolean = false
)
