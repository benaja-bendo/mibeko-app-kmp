package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class LegalApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    
    /**
     * Fetch the global catalog status.
     * Used to detect if app needs to update its local database.
     */
    suspend fun fetchCatalog(): ApiResponse<RemoteCatalogData> {
        return client.get("$baseUrl/v1/catalog").body()
    }

    /**
     * Fetch paginated list of legal documents.
     */
    suspend fun fetchDocuments(page: Int = 1): RemoteDocumentResponse {
        val response = client.get("${baseUrl}/v1/legal-documents") {
            parameter("page", page)
        }
        return response.body()
    }

    suspend fun fetchStats(): ApiResponse<List<DocumentStats>> {
        val response = client.get("${baseUrl}/v1/catalog/stats")
        return response.body()
    }

    /**
     * Fetch home page data (popular codes, recent documents, AI suggestions).
     */
    suspend fun fetchHomeData(): ApiResponse<RemoteHomeData> {
        return client.get("$baseUrl/v1/home").body()
    }

    /**
     * Fetch a single legal document with its structure and articles.
     */
    suspend fun fetchDocument(documentId: String): ApiResponse<RemoteDocument> {
        return client.get("$baseUrl/v1/legal-documents/$documentId").body()
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
     * Search articles by content using hybrid search (vector + full-text).
     * Returns results with document context and breadcrumb.
     */
    suspend fun searchArticles(
        query: String? = null,
        documentId: String? = null,
        tag: String? = null,
        perPage: Int = 20
    ): RemoteSearchResponse {
        return client.get("$baseUrl/v1/search") {
            query?.let { parameter("q", it) }
            documentId?.let { parameter("document_id", it) }
            tag?.let { parameter("tag", it) }
            parameter("per_page", perPage)
        }.body()
    }

    /**
     * Download a full document (structure + articles) for offline use.
     */
    suspend fun downloadDocument(documentId: String, nodeId: String? = null): RemoteDownloadResponse {
        return client.get("$baseUrl/v1/legal-documents/$documentId/download") {
            nodeId?.let { parameter("node_id", it) }
        }.body()
    }

    /**
     * Fetch list of document types.
     */
    suspend fun fetchDocumentTypes(): ApiResponse<List<RemoteDocumentType>> {
        return client.get("$baseUrl/v1/document-types").body()
    }

    /**
     * Get the export URL for a specific document.
     */
    fun getDocumentExportUrl(documentId: String): String {
        return "$baseUrl/v1/legal-documents/$documentId/export"
    }

    /**
     * Get the export URL for a specific article.
     */
    fun getArticleExportUrl(articleId: String): String {
        return "$baseUrl/v1/articles/$articleId/export"
    }

    /**
     * Get the PDF proxy URL for an official journal.
     */
    fun getOfficialJournalPdfUrl(id: String): String {
        return "$baseUrl/v1/legal-documents/$id/pdf?type=journal"
    }

    suspend fun fetchInstitutions(): ApiResponse<List<RemoteInstitution>> {
        return client.get("$baseUrl/v1/institutions").body()
    }

    /**
     * Fetch paginated list of official journals.
     */
    suspend fun fetchOfficialJournals(page: Int = 1): RemoteOfficialJournalResponse {
        return client.get("$baseUrl/v1/official-journals") {
            parameter("page", page)
        }.body()
    }

    /**
     * Fetch a specific official journal with its documents.
     */
    suspend fun fetchOfficialJournal(id: String): RemoteOfficialJournal {
        val response = client.get("$baseUrl/v1/official-journals/$id")
        val bodyAsText = response.bodyAsText()
        
        val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(bodyAsText)
        return if (jsonElement is JsonObject && jsonElement.containsKey("data")) {
            // It's wrapped in a "data" field
            Json { ignoreUnknownKeys = true }.decodeFromJsonElement<RemoteOfficialJournalSingleResponse>(jsonElement).data 
                ?: throw Exception("Data object is null in response")
        } else {
            // It's the object directly
            Json { ignoreUnknownKeys = true }.decodeFromJsonElement<RemoteOfficialJournal>(jsonElement)
        }
    }

    /**
     * Export dossier as PDF.
     */
    suspend fun exportDossierPdf(request: DossierExportRequest): ByteArray {
        return client.post("$baseUrl/v1/dossiers/export-pdf") {
            header(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }.body<ByteArray>()
    }

    /**
     * Download a file from a URL.
     */
    suspend fun downloadFile(url: String): ByteArray {
        return client.get(url).body<ByteArray>()
    }
}
