package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

// =============================================================================
// Contrats de la Bibliothèque — mêmes endpoints que le poste de travail web :
//   GET /v1/library/home     (textes fondamentaux, récents, stats, suggestions)
//   GET /v1/library/search   (full-text serveur, granularité article, paginé)
//   GET /v1/library/suggest  (autocomplétion : documents, articles, passages)
// La recherche ne renvoie JAMAIS de contenu généré par l'IA.
// =============================================================================

@Serializable
data class LibraryStats(
    val documents: Int = 0,
    val articles: Int = 0,
    val institutions: Int = 0
)

@Serializable
data class LibraryHomeDocument(
    val id: String,
    val title: String,
    val type_code: String? = null,
    val type_name: String? = null,
    val legal_scope: String? = null,
    val date_publication: String? = null,
    val articles_count: Int? = null
)

@Serializable
data class LibraryHomeData(
    val stats: LibraryStats = LibraryStats(),
    val essential_documents: List<LibraryHomeDocument> = emptyList(),
    val recent_documents: List<LibraryHomeDocument> = emptyList(),
    val suggestions: List<String> = emptyList()
)

@Serializable
data class LibraryHomeResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: LibraryHomeData? = null
)

/** Un résultat de recherche (granularité article, comme sur le web). */
@Serializable
data class LibrarySearchItem(
    val id: String,
    val number: String? = null,
    val content: String? = null,
    val document_id: String? = null,
    val document_title: String? = null,
    val document_type: String? = null,
    val node_title: String? = null,
    val breadcrumb: String? = null,
    val legal_scope: String? = null,
    val institution: String? = null,
    val date_publication: String? = null,
    val score: Double? = null
)

@Serializable
data class LibraryPagination(
    val total: Int = 0,
    val per_page: Int = 0,
    val current_page: Int = 1,
    val last_page: Int = 1
)

@Serializable
data class LibrarySearchResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: List<LibrarySearchItem> = emptyList(),
    val pagination: LibraryPagination? = null
)

@Serializable
data class SuggestDocument(
    val id: String,
    val title: String,
    val type_code: String? = null,
    val type_name: String? = null
)

@Serializable
data class SuggestArticle(
    val id: String,
    val number: String,
    val document_id: String,
    val document_title: String,
    val type_code: String? = null
)

/** Extrait de contenu correspondant à la frappe ; les marques sont balisées [[…]]. */
@Serializable
data class SuggestPassage(
    val id: String,
    val number: String,
    val document_id: String,
    val document_title: String,
    val snippet: String
)

@Serializable
data class LibrarySuggestions(
    val documents: List<SuggestDocument> = emptyList(),
    val articles: List<SuggestArticle> = emptyList(),
    val passages: List<SuggestPassage> = emptyList()
) {
    val isEmpty: Boolean
        get() = documents.isEmpty() && articles.isEmpty() && passages.isEmpty()
}

@Serializable
data class LibrarySuggestResponse(
    val success: Boolean = false,
    val data: LibrarySuggestions? = null
)

class LibraryApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun fetchHome(): LibraryHomeData {
        return client.get("$baseUrl/v1/library/home").body<LibraryHomeResponse>().data
            ?: LibraryHomeData()
    }

    suspend fun search(
        query: String,
        typeCode: String? = null,
        legalScope: String? = null,
        institutionId: String? = null,
        sort: String = "relevance",
        page: Int = 1,
        perPage: Int = 20
    ): LibrarySearchResponse {
        return client.get("$baseUrl/v1/library/search") {
            parameter("q", query)
            typeCode?.let { parameter("type", it) }
            legalScope?.let { parameter("legal_scope", it) }
            institutionId?.let { parameter("institution_id", it) }
            if (sort != "relevance") parameter("sort", sort)
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()
    }

    suspend fun suggest(query: String): LibrarySuggestions {
        return client.get("$baseUrl/v1/library/suggest") {
            parameter("q", query)
        }.body<LibrarySuggestResponse>().data ?: LibrarySuggestions()
    }
}
