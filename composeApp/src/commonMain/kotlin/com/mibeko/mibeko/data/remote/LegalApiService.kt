package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.timeout
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
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Fetch the global catalog status.
     * Used to detect if app needs to update its local database.
     */
    suspend fun fetchCatalog(): ApiResponse<RemoteCatalogData> {
        return client.get("$baseUrl/v1/catalog").body()
    }

    /**
     * Contrat force-update (GET /v1/app-config, public, caché serveur).
     * L'appelant DOIT traiter tout échec en fail-open (aucun blocage).
     */
    suspend fun fetchAppConfig(): AppConfigResponse {
        return client.get("$baseUrl/v1/app-config").body()
    }

    /**
     * Fetch paginated list of legal documents.
     */
    suspend fun fetchDocuments(page: Int = 1): RemoteDocumentResponse {
        val response = client.get("${baseUrl}/v1/legal-documents") {
            parameter("page", page)
            // Page de catalogue lue pendant la synchronisation initiale, sur le
            // même réseau lent que l'arbre : même marge.
            timeout { requestTimeoutMillis = 300_000 }
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
     * Résout un document publié par son slug d'URL publique (lien profond
     * `mibeko.fr/textes/{slug}`). Route publique, publiés uniquement. Renvoie
     * le document (avec son id interne) et l'index de ses articles ; `null` si
     * le slug est introuvable (404) ou en cas d'erreur réseau.
     */
    suspend fun fetchDocumentBySlug(slug: String): RemotePublicDocumentData? {
        return try {
            client.get("$baseUrl/v1/legal-documents/slug/${slug.encodeURLPathPart()}")
                .body<RemotePublicDocumentResponse>()
                .data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch the structure tree for a document with articles.
     *
     * L'arbre transporte le contenu de TOUS les articles : plusieurs Mo pour un
     * code volumineux. Le plafond global de 60 s (wall-clock, réception du corps
     * comprise) le ferait échouer sur une 3G congolaise alors qu'il progresse.
     */
    suspend fun fetchDocumentTree(documentId: String): List<RemoteNode> {
        return client.get("$baseUrl/v1/legal-documents/$documentId/tree") {
            timeout { requestTimeoutMillis = 300_000 }
        }.body<ApiResponse<List<RemoteNode>>>().data ?: emptyList()
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
            // Un code volumineux sur réseau lent peut dépasser le timeout
            // global de 60 s : on l'élargit pour ce seul appel.
            timeout { requestTimeoutMillis = 300_000 }
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
     * Résout le document parent d'un article (lecture d'un résultat de
     * recherche dont le document n'est pas en cache local).
     */
    suspend fun fetchArticleContext(articleId: String): RemoteArticleContext? {
        return client.get("$baseUrl/v1/articles/$articleId/context")
            .body<RemoteArticleContextResponse>().data
    }

    /**
     * Fetch paginated list of official journals.
     */
    suspend fun fetchOfficialJournals(page: Int = 1, number: String? = null, year: Int? = null, perPage: Int = 15): RemoteOfficialJournalResponse {
        return client.get("$baseUrl/v1/official-journals") {
            parameter("page", page)
            parameter("per_page", perPage)
            if (number != null) parameter("filter[number]", number)
            if (year != null) parameter("filter[year]", year)
        }.body()
    }

    /**
     * Fetch a specific official journal with its documents.
     */
    suspend fun fetchOfficialJournal(id: String): RemoteOfficialJournal {
        val response = client.get("$baseUrl/v1/official-journals/$id")
        val bodyAsText = response.bodyAsText()
        
        val jsonElement = json.parseToJsonElement(bodyAsText)
        return if (jsonElement is JsonObject && jsonElement.containsKey("data")) {
            // It's wrapped in a "data" field
            json.decodeFromJsonElement<RemoteOfficialJournalSingleResponse>(jsonElement).data 
                ?: throw Exception("Data object is null in response")
        } else {
            // It's the object directly
            json.decodeFromJsonElement<RemoteOfficialJournal>(jsonElement)
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
     * Envoie un signalement d'erreur pour un document ou un article.
     */
    suspend fun reportError(
        documentId: String?,
        articleId: String?,
        type: String,
        description: String
    ): ApiResponse<Unit> {
        return client.post("$baseUrl/v1/reports") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "document_id" to documentId,
                "article_id" to articleId,
                "type_probleme" to type,
                "description" to description
            ))
        }.body()
    }

    /**
     * Download a file from a URL.
     */
    suspend fun downloadFile(url: String): ByteArray {
        return client.get(url) {
            timeout { requestTimeoutMillis = 300_000 }
        }.body<ByteArray>()
    }

    /**
     * Envoie un message de support (POST /v1/contact, public, throttle 6/min).
     * Contrat serveur : name + email requis, message ≥ 10 caractères.
     */
    suspend fun sendContactMessage(name: String, email: String, message: String): Boolean {
        val response = client.post("$baseUrl/v1/contact") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "message" to message
                )
            )
        }
        return response.status.value in 200..299
    }
}
