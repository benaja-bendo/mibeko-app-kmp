package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class LegalApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    
    /**
     * Fetch paginated list of legal documents.
     */
    suspend fun fetchDocuments(page: Int = 1): RemoteDocumentResponse {
        return client.get("$baseUrl/v1/legal-documents") {
            parameter("page", page)
        }.body()
    }

    /**
     * Fetch the structure tree for a document with articles.
     */
    suspend fun fetchDocumentTree(documentId: String): List<RemoteNode> {
        return client.get("$baseUrl/v1/legal-documents/$documentId/tree").body<RemoteTreeResponse>().data
    }

    /**
     * Fetch articles updated since a given timestamp.
     */
    suspend fun fetchUpdates(since: String): RemoteSyncResponse {
        return client.get("$baseUrl/v1/sync/updates") {
            parameter("since", since)
        }.body()
    }

    /**
     * Search legal documents by title.
     */
    suspend fun searchDocuments(query: String, page: Int = 1): RemoteDocumentResponse {
        return client.get("$baseUrl/v1/legal-documents") {
            parameter("search", query)
            parameter("page", page)
        }.body()
    }

    /**
     * Search articles by content or number.
     * Returns results with document context and breadcrumb.
     */
    suspend fun searchArticles(query: String, type: String? = null, page: Int = 1): RemoteSearchResponse {
        return client.get("$baseUrl/v1/articles/search") {
            parameter("q", query)
            type?.let { parameter("type", it) }
            parameter("page", page)
        }.body()
    }
}
