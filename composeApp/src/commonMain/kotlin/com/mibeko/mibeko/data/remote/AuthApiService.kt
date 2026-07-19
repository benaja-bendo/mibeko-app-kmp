package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
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
    val device_name: String,
    /** Code TOTP exigé quand la double authentification est active. */
    val code: String? = null,
    val recovery_code: String? = null
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val password: String,
    val password_confirmation: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String,
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

/**
 * Réponse du login quand le compte exige la double authentification (HTTP 423,
 * `errors.two_factor_required`). Les erreurs y sont un objet hétérogène, d'où
 * un modèle dédié plutôt que la map de AuthResponse.
 */
@Serializable
data class TwoFactorChallengeResponse(
    val success: Boolean = false,
    val message: String? = null,
    val errors: TwoFactorErrors? = null
) {
    @Serializable
    data class TwoFactorErrors(val two_factor_required: Boolean = false)
}

@Serializable
data class RemoteUser(
    val id: String,
    val name: String,
    val email: String,
    // Vérification d'e-mail — exposée par `UserProfileResource` (GET /v1/profile).
    // `email_verified` : booléen ; `email_verified_at` : ISO8601 ou null.
    // Absents des réponses login/register (formatUser) : d'où les valeurs par
    // défaut, qui provoquent une dégradation silencieuse côté UI.
    val email_verified: Boolean? = null,
    val email_verified_at: String? = null,
    // L'API normalise les rôles en tableau de chaînes (AuthController::formatUser).
    val roles: List<String> = emptyList(),
    val mobile_profile: RemoteMobileProfile? = null
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
    val name: String? = null,
    val phone: String,
    val profession: String,
    val company: String
)

@Serializable
data class PasswordUpdateRequest(
    val current_password: String,
    val password: String,
    val password_confirmation: String
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
    /**
     * Invalide le jeton mis en cache par le plugin Auth de Ktor.
     * À appeler après connexion, inscription ou déconnexion : sinon le client
     * continue d'utiliser l'ancien jeton (ou aucun) jusqu'au redémarrage.
     */
    fun invalidateTokenCache() {
        client.authProviders
            .filterIsInstance<io.ktor.client.plugins.auth.providers.BearerAuthProvider>()
            .forEach { it.clearToken() }
    }

    private suspend inline fun <reified T> safeApiCall(apiCall: () -> io.ktor.client.statement.HttpResponse): T {
        return try {
            apiCall().body()
        } catch (e: ClientRequestException) {
            val status = e.response.status
            if (status == HttpStatusCode.UnprocessableEntity || status == HttpStatusCode.Unauthorized) {
                e.response.body()
            } else {
                throw e
            }
        }
    }

    suspend fun getProfile(): ProfileResponse = safeApiCall {
        client.get("$baseUrl/v1/profile")
    }

    /**
     * Renvoie l'e-mail de vérification (contrat livré : POST
     * `/v1/email/verification-notification`, Bearer → 202, idempotent, quota
     * serré 3/min). Distingue le succès, l'étranglement (429) et l'échec pour
     * un retour utilisateur clair sans exposer d'exception.
     */
    suspend fun resendEmailVerification(): ResendVerificationResult {
        return try {
            client.post("$baseUrl/v1/email/verification-notification")
            ResendVerificationResult.SENT
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.TooManyRequests) {
                ResendVerificationResult.THROTTLED
            } else {
                ResendVerificationResult.ERROR
            }
        } catch (e: Exception) {
            ResendVerificationResult.ERROR
        }
    }

    suspend fun updateProfile(request: ProfileUpdateRequest): ProfileResponse = safeApiCall {
        client.put("$baseUrl/v1/profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updatePassword(request: PasswordUpdateRequest): ProfileResponse = safeApiCall {
        client.put("$baseUrl/v1/profile/password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun logout(): AuthResponse = safeApiCall {
        client.post("$baseUrl/v1/logout")
    }

    suspend fun loginWithFirebase(idToken: String, deviceName: String): AuthResponse = safeApiCall {
        client.post("$baseUrl/v1/auth/firebase") {
            contentType(ContentType.Application.Json)
            setBody(FirebaseLoginRequest(idToken, deviceName))
        }
    }

    /**
     * @throws TwoFactorRequiredException si le compte exige un code TOTP (HTTP 423) :
     * l'appelant doit représenter l'étape de saisie du code puis rappeler avec `code`.
     */
    suspend fun loginWithEmail(request: LoginRequest): AuthResponse {
        return try {
            safeApiCall {
                client.post("$baseUrl/v1/login") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Locked) {
                val challenge = runCatching { e.response.body<TwoFactorChallengeResponse>() }.getOrNull()
                if (challenge?.errors?.two_factor_required == true) {
                    throw TwoFactorRequiredException(challenge.message ?: "Code de double authentification requis.")
                }
            }
            throw e
        }
    }

    suspend fun register(request: RegisterRequest): AuthResponse = safeApiCall {
        client.post("$baseUrl/v1/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun forgotPassword(email: String): AuthResponse = safeApiCall {
        client.post("$baseUrl/v1/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email))
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest): AuthResponse = safeApiCall {
        client.post("$baseUrl/v1/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteAccount(currentPassword: String): AuthResponse = safeApiCall {
        client.delete("$baseUrl/v1/profile") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("current_password" to currentPassword))
        }
    }
}

/** Le compte est protégé par double authentification : un code TOTP est requis. */
class TwoFactorRequiredException(message: String) : Exception(message)

/** Issue de l'appel de renvoi d'e-mail de vérification. */
enum class ResendVerificationResult {
    /** 202 — l'e-mail a (le cas échéant) été renvoyé. */
    SENT,

    /** 429 — quota d'envoi atteint, réessayer plus tard. */
    THROTTLED,

    /** Autre erreur (réseau, 5xx…). */
    ERROR
}
