package com.mibeko.mibeko.data

import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity

fun DocumentEntity.toLawCodeSpec(): LawCodeSpec {
    return LawCodeSpec(
        id = id,
        title = title,
        icon = type_code, // Using type_code as icon identifier as per plan
        lastUpdated = last_updated.toString() // Simple conversion for now
    )
}

// Mapper that takes extra context from joins
fun ArticleEntity.toArticleSpec(
    codeId: String,
    title: String,
    breadcrumb: String
): ArticleSpec {
    return ArticleSpec(
        id = id,
        codeId = codeId,
        number = number,
        title = title,
        content = content,
        breadcrumb = breadcrumb,
        isFavorite = is_favorite
    )
}

/**
 * Maps remote API search result to ArticleSpec.
 */
fun com.mibeko.mibeko.data.remote.RemoteSearchResult.toArticleSpec(): ArticleSpec {
    return ArticleSpec(
        id = id,
        codeId = document_id,
        number = number,
        title = node_title ?: "",
        content = content,
        breadcrumb = breadcrumb,
        isFavorite = false // Remote results don't have favorite status
    )
}
