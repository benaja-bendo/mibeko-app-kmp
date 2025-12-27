package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class LegalApiService(private val client: HttpClient) {
    
    // Placeholder URL - change to real endpoint when available
    private val BASE_URL = "http://192.168.1.149:8000/api/v1"

    suspend fun fetchDocuments(page: Int = 1): RemoteDocumentResponse {
        return client.get("$BASE_URL/legal-documents") {
            parameter("page", page)
        }.body()
    }

    suspend fun fetchDocumentTree(documentId: String): List<RemoteNode> {
        return client.get("$BASE_URL/legal-documents/$documentId/tree").body<RemoteTreeResponse>().data
    }

    suspend fun fetchUpdates(since: String): RemoteSyncResponse {
        return client.get("$BASE_URL/sync/updates") {
            parameter("since", since)
        }.body()
    }
}
