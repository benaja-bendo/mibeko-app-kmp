package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseLoginRequest(
    val id_token: String,
    val device_name: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: RemoteUser
)

@Serializable
data class RemoteUser(
    val id: String,
    val name: String,
    val email: String,
    val roles: List<RemoteRole> = emptyList(),
    val mobile_profile: RemoteMobileProfile? = null
)

@Serializable
data class RemoteRole(
    val name: String
)

@Serializable
data class RemoteMobileProfile(
    val id: Int? = null,
    val phone: String? = null,
    val profession: String? = null,
    val company: String? = null
)

class AuthApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun loginWithFirebase(idToken: String, deviceName: String): AuthResponse {
        return client.post("$baseUrl/v1/auth/firebase") {
            contentType(ContentType.Application.Json)
            setBody(FirebaseLoginRequest(idToken, deviceName))
        }.body()
    }
}
