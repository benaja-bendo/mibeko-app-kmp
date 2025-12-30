package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class LegalApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    
    suspend fun fetchDocuments(page: Int = 1): RemoteDocumentResponse {
        return client.get("$baseUrl/legal-documents") {
            parameter("page", page)
        }.body()
    }

    suspend fun fetchDocumentTree(documentId: String): List<RemoteNode> {
        return client.get("$baseUrl/legal-documents/$documentId/tree").body<RemoteTreeResponse>().data
    }

    suspend fun fetchUpdates(since: String): RemoteSyncResponse {
        return client.get("$baseUrl/sync/updates") {
            parameter("since", since)
        }.body()
    }
}
