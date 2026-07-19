package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.NotificationRemote
import com.mibeko.mibeko.data.remote.LegalApiService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NotificationRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    /**
     * Récupère les notifications depuis l'API.
     */
    fun getNotifications(): Flow<List<NotificationRemote>> = flow {
        try {
            // Dans un cas réel, nous utiliserions un token d'auth.
            // Pour l'instant on utilise l'URL de base configurée.
            val response = httpClient.get("$baseUrl/v1/notifications")
            if (response.status.value in 200..299) {
                val body: Map<String, List<NotificationRemote>> = response.body()
                emit(body["data"] ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            recordException(e, context = "NotificationRepository.getNotifications")
            emit(emptyList())
        }
    }

    /**
     * Marque une notification comme lue.
     */
    suspend fun markAsRead(id: String) {
        try {
            httpClient.patch("$baseUrl/v1/notifications/$id/read")
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Marque toutes les notifications comme lues.
     */
    suspend fun markAllAsRead() {
        try {
            httpClient.post("$baseUrl/v1/notifications/read-all")
        } catch (e: Exception) {
            // Log error
        }
    }

    /**
     * Supprime une notification.
     */
    suspend fun deleteNotification(id: String) {
        try {
            httpClient.delete("$baseUrl/v1/notifications/$id")
        } catch (e: Exception) {
            // Log error
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
                    "platform" to platform
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
            // Log error
        }
    }
}
