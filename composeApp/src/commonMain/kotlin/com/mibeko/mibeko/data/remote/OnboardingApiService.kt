package com.mibeko.mibeko.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class OnboardingProgress(
    val viewed_at: String? = null,
    val skipped_at: String? = null,
    val completed_at: String? = null,
    val value: JsonElement? = null
) {
    val resolved: Boolean get() = skipped_at != null || completed_at != null
}

@Serializable
data class OnboardingStep(
    val key: String,
    val type: String,
    val scope: String = "common",
    val config: JsonObject = JsonObject(emptyMap()),
    val binding: String? = null,
    val conditions: JsonArray = JsonArray(emptyList()),
    val supported: Boolean = true,
    val progress: OnboardingProgress = OnboardingProgress()
)

@Serializable
data class OnboardingJourney(val key: String, val version: Int, val steps: List<OnboardingStep>)

@Serializable
data class OnboardingEnrollment(
    val status: String,
    val started_at: String? = null,
    val completed_at: String? = null,
    val postponed_at: String? = null,
    val replay_count: Int = 0
)

@Serializable
data class OnboardingJourneyData(
    val available: Boolean,
    val journey: OnboardingJourney? = null,
    val enrollment: OnboardingEnrollment? = null
)

@Serializable
data class OnboardingStepMutation(
    val action: String,
    val value: JsonElement? = null,
    val client_mutation_id: String,
    val client_updated_at: Long,
    val platform: String
)

@Serializable
data class OnboardingControlMutation(val client_mutation_id: String)

class OnboardingApiService(private val client: HttpClient, private val baseUrl: String) {
    suspend fun journey(platform: String, knownStepTypes: List<String>): OnboardingJourneyData {
        return client.get("$baseUrl/v1/onboarding/journey") {
            parameter("platform", platform)
            knownStepTypes.forEach { parameter("known_step_types[]", it) }
        }.body<ApiResponse<OnboardingJourneyData>>().data ?: OnboardingJourneyData(available = false)
    }

    suspend fun updateStep(stepKey: String, mutation: OnboardingStepMutation) {
        client.patch("$baseUrl/v1/onboarding/steps/$stepKey") {
            contentType(ContentType.Application.Json)
            setBody(mutation)
        }
    }

    suspend fun postpone(mutationId: String) {
        client.post("$baseUrl/v1/onboarding/postpone") {
            contentType(ContentType.Application.Json)
            setBody(OnboardingControlMutation(mutationId))
        }
    }

    suspend fun replay(mutationId: String) {
        client.post("$baseUrl/v1/onboarding/replay") {
            contentType(ContentType.Application.Json)
            setBody(OnboardingControlMutation(mutationId))
        }
    }
}
