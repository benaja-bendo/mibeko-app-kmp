package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseLoginRequest(
    val id_token: String,
    val device_name: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val device_name: String
)

@Serializable
data class AuthResponseData(
    val token: String,
    val user: RemoteUser? = null
)

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: AuthResponseData? = null,
    val errors: Map<String, List<String>>? = null
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

@Serializable
data class ProfileUpdateRequest(
    val phone: String,
    val profession: String,
    val company: String
)

@Serializable
data class ProfileResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: RemoteUser? = null,
    val errors: Map<String, List<String>>? = null
)

class AuthApiService(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun getProfile(): ProfileResponse {
        try {
            val response = client.get("$baseUrl/v1/profile")
            return response.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.UnprocessableEntity || e.response.status == HttpStatusCode.Unauthorized) {
                return e.response.body()
            }
            throw e
        }
    }

    suspend fun updateProfile(request: ProfileUpdateRequest): ProfileResponse {
        try {
            val response = client.put("$baseUrl/v1/profile") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            return response.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                return e.response.body()
            }
            throw e
        }
    }

    suspend fun logout(): AuthResponse {
        try {
            val response = client.post("$baseUrl/v1/logout")
            return response.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                return e.response.body()
            }
            throw e
        }
    }

    suspend fun loginWithFirebase(idToken: String, deviceName: String): AuthResponse {
        try {
            val response = client.post("$baseUrl/v1/auth/firebase") {
                contentType(ContentType.Application.Json)
                setBody(FirebaseLoginRequest(idToken, deviceName))
            }
            return response.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                return e.response.body()
            }
            throw e
        }
    }

    suspend fun loginWithEmail(request: LoginRequest): AuthResponse {
        try {
            val response = client.post("$baseUrl/v1/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            return response.body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.UnprocessableEntity) {
                return e.response.body()
            }
            throw e
        }
    }
}
