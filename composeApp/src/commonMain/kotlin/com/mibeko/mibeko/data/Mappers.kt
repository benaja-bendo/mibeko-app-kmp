package com.mibeko.mibeko.data

import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity
import com.mibeko.mibeko.util.ArticleTable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

fun DocumentEntity.toLawCodeSpec(): LawCodeSpec {
    return LawCodeSpec(
        id = id,
        title = title,
        type = type_code,
        icon = type_code, // Using type_code as icon identifier as per plan
        lastUpdated = last_updated,
        isDownloaded = is_downloaded,
        institutionName = institution_name,
        dateSignature = date_signature,
        slug = slug,
        consolidationAsOf = consolidation_as_of
    )
}

fun com.mibeko.mibeko.data.local.dao.ArticleSearchResult.toArticleSpec(): ArticleSpec {
    return ArticleSpec(
        id = article.id,
        codeId = document_id,
        number = article.number,
        title = node_title,
        content = article.content,
        breadcrumb = node_title,
        typeCode = type_code, // Added
        isFavorite = article.is_favorite,
        isDownloaded = doc_is_downloaded || article.is_offline
    )
}

// Mapper that takes extra context from joins
fun ArticleEntity.toArticleSpec(
    codeId: String,
    title: String,
    breadcrumb: String,
    typeCode: String = "", // Added
    isDocDownloaded: Boolean = false
): ArticleSpec {
    return ArticleSpec(
        id = id,
        codeId = codeId,
        number = number,
        title = title,
        content = content,
        breadcrumb = breadcrumb,
        typeCode = typeCode, // Added
        isFavorite = is_favorite,
        isDownloaded = isDocDownloaded || is_offline,
        tables = decodeArticleTables(tables_json)
    )
}

/**
 * Relit les tableaux stockés en JSON dans `articles.tables_json`.
 *
 * Un JSON illisible (colonne écrite par une version antérieure, corpus
 * partiellement migré) ne doit jamais faire échouer l'ouverture d'un article :
 * on retombe sur aucune structure, et le lecteur affiche le texte — lisible,
 * simplement sans colonnes.
 */
internal fun decodeArticleTables(json: String?): List<ArticleTable> {
    if (json.isNullOrBlank()) return emptyList()

    return try {
        articleTablesJson.decodeFromString(ListSerializer(ArticleTable.serializer()), json)
    } catch (_: SerializationException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }
}

/** Sérialise les tableaux pour la colonne `articles.tables_json` (null si aucun). */
internal fun encodeArticleTables(tables: List<ArticleTable>): String? =
    if (tables.isEmpty()) {
        null
    } else {
        articleTablesJson.encodeToString(ListSerializer(ArticleTable.serializer()), tables)
    }

private val articleTablesJson = Json { ignoreUnknownKeys = true }

/**
 * Maps remote API search result to ArticleSpec.
 */
fun com.mibeko.mibeko.data.remote.RemoteSearchResult.toArticleSpec(): ArticleSpec {
    return ArticleSpec(
        id = id,
        codeId = document_id,
        number = number,
        title = node_title,
        content = content ?: "",
        breadcrumb = breadcrumb,
        typeCode = document_type, // Map document_type from API to typeCode
        isFavorite = false // Remote results don't have favorite status
    )
}
