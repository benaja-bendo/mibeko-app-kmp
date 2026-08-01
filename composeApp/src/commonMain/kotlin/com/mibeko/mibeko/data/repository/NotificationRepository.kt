package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.NotificationRemote
import com.mibeko.mibeko.data.remote.ApiResponse
import com.mibeko.mibeko.data.remote.LegalApiService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.mibeko.mibeko.util.getAppVersionName
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NotificationRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    /**
     * Récupère les notifications depuis l'API. [Result.failure] distingue un
     * réel échec d'une liste vide — jamais avalé en `emptyList()` silencieux
     * (une boîte vide mensongère sur panne réseau, règle produit non
     * négociable).
     */
    fun getNotifications(): Flow<Result<List<NotificationRemote>>> = flow {
        try {
            val response = httpClient.get("$baseUrl/v1/notifications")
            if (response.status.value in 200..299) {
                // L'API répond {success, message, data:[…]} : le désérialiser en
                // Map<String, List<…>> échouait systématiquement (`success` est
                // un booléen, `message` une chaîne) et l'exception, avalée plus
                // bas, rendait la liste TOUJOURS vide — même quand le serveur
                // avait des notifications à donner.
                val body: ApiResponse<List<NotificationRemote>> = response.body()
                emit(Result.success(body.data ?: emptyList()))
            } else {
                emit(Result.failure(IllegalStateException("HTTP ${response.status.value}")))
            }
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.getNotifications")
            emit(Result.failure(e))
        }
    }

    /**
     * Marque une notification comme lue.
     */
    suspend fun markAsRead(id: String) {
        try {
            httpClient.patch("$baseUrl/v1/notifications/$id/read")
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.markAsRead")
        }
    }

    /**
     * Marque toutes les notifications comme lues.
     */
    suspend fun markAllAsRead() {
        try {
            httpClient.post("$baseUrl/v1/notifications/read-all")
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.markAllAsRead")
        }
    }

    /**
     * Supprime une notification.
     */
    suspend fun deleteNotification(id: String) {
        try {
            httpClient.delete("$baseUrl/v1/notifications/$id")
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.deleteNotification")
        }
    }

    /**
     * Enregistre l'appareil pour les notifications push.
     * @return true si le backend a accepté l'enregistrement (permet à
     * PushTokenRegistrar de conserver le token en attente en cas d'échec).
     */
    suspend fun registerDevice(deviceId: String, pushToken: String, platform: String): Boolean {
        return try {
            val response = httpClient.post("$baseUrl/v1/devices/register") {
                setBody(mapOf(
                    "device_id" to deviceId,
                    "push_token" to pushToken,
                    "platform" to platform,
                    // Le serveur adapte la FORME du push à la version installée :
                    // data-only à partir de cette version (deep link fiable),
                    // format hérité en deçà — les binaires déjà publiés ne
                    // savent afficher qu'un bloc `notification`.
                    "app_version" to getAppVersionName()
                ))
                contentType(io.ktor.http.ContentType.Application.Json)
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.registerDevice")
            false
        }
    }

    /**
     * Désinscrit l'appareil des notifications push.
     */
    suspend fun unregisterDevice(deviceId: String) {
        try {
            httpClient.post("$baseUrl/v1/devices/unregister") {
                setBody(mapOf("device_id" to deviceId))
                contentType(io.ktor.http.ContentType.Application.Json)
            }
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.unregisterDevice")
        }
    }
}
