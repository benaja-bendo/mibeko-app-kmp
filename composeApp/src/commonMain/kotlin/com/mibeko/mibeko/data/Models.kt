package com.mibeko.mibeko.data

data class LawCodeSpec(
    val id: String,
    val title: String,
    val icon: String, // Simple identifier for icon selection
    val lastUpdated: String
)

data class ArticleSpec(
    val id: String,
    val codeId: String,
    val number: String,
    val title: String,
    val content: String,
    val breadcrumb: String,
    val isFavorite: Boolean = false
)

data class ExplorerNode(
    val id: String,
    val title: String,
    val type: NodeType,
    val children: List<ExplorerNode> = emptyList()
)

enum class NodeType {
    CODE, BOOK, TITLE, CHAPTER, ARTICLE
}
