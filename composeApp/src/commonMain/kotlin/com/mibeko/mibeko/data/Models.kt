package com.mibeko.mibeko.data

data class LawCodeSpec(
    val id: String,
    val title: String,
    val type: String, // Added for filtering
    val icon: String, // Simple identifier for icon selection
    val lastUpdated: Long,
    val isDownloaded: Boolean = false,
    val institutionName: String? = null,
    val dateSignature: String? = null
)

data class ArticleSpec(
    val id: String,
    val codeId: String,
    val number: String,
    val title: String,
    val content: String?,
    val breadcrumb: String,
    val typeCode: String = "", // Added for filtering
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false
)

@kotlinx.serialization.Serializable
data class NotificationRemote(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val data: Map<String, String>? = null,
    val read_at: String? = null,
    val created_at: String
)
