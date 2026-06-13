package com.mibeko.mibeko.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Accès réseau pour la synchronisation des dossiers utilisateur.
 * Toutes les routes exigent un jeton Sanctum (injecté par le client Ktor).
 */
class DossierApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    /** Récupère l'état autoritaire du serveur sans rien pousser. */
    suspend fun fetchDossiers(): ApiResponse<DossierSyncData> {
        return client.get("$baseUrl/v1/dossiers").body()
    }

    /** Pousse l'état local et reçoit l'état fusionné (last-write-wins). */
    suspend fun sync(request: DossierSyncRequest): ApiResponse<DossierSyncData> {
        return client.post("$baseUrl/v1/dossiers/sync") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
