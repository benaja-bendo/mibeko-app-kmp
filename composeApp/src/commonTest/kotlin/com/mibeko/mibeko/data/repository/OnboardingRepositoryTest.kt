package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.OnboardingApiService
import com.mibeko.mibeko.data.remote.OnboardingEnrollment
import com.mibeko.mibeko.data.remote.OnboardingJourney
import com.mibeko.mibeko.data.remote.OnboardingJourneyData
import com.mibeko.mibeko.data.remote.OnboardingProgress
import com.mibeko.mibeko.data.remote.OnboardingStep
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class TestConnectivity(initial: Boolean) : NetworkConnectivityChecker {
    private val state = MutableStateFlow(initial)
    override fun isNetworkAvailable(): Boolean = state.value
    override val isOnline: StateFlow<Boolean> = state
    fun setOnline(online: Boolean) { state.value = online }
}

class OnboardingRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Test
    fun `offline mutation stays isolated then server truth wins after reconnect`() = runTest {
        var networkCalls = 0
        val engine = MockEngine { request ->
            networkCalls++
            if (request.url.encodedPath.endsWith("/journey")) {
                respond(
                    content = SERVER_JOURNEY,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json")
                )
            } else {
                respond("""{"success":true,"data":{}}""", headers = headersOf("Content-Type", "application/json"))
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        val settings = MapSettings()
        val preferences = UserPreferencesRepository(settings)
        val connectivity = TestConnectivity(false)
        val repository = OnboardingRepository(
            OnboardingApiService(client, "https://api.test/api"), preferences, json, connectivity
        )

        preferences.setUserInfo("Compte A", "a@example.test", "account-a")
        preferences.setAccountOnboardingCache(json.encodeToString(cachedJourney()))
        val offline = repository.updateStep("usage_context", "answer", JsonPrimitive("professional"))

        assertEquals(0, networkCalls)
        assertTrue(offline is OnboardingLoadResult.Ready && offline.offline)
        assertNotNull(preferences.getAccountOnboardingQueue())

        preferences.setUserInfo("Compte B", "b@example.test", "account-b")
        assertNull(preferences.getAccountOnboardingQueue())
        preferences.setUserInfo("Compte A", "a@example.test", "account-a")

        connectivity.setOnline(true)
        val refreshed = repository.load() as OnboardingLoadResult.Ready

        assertNull(preferences.getAccountOnboardingQueue())
        assertEquals(
            "personal",
            refreshed.data.journey?.steps?.first()?.progress?.value?.toString()?.trim('"'),
            "Le GET serveur après rejeu reste la vérité affichée"
        )
    }

    private fun cachedJourney() = OnboardingJourneyData(
        available = true,
        journey = OnboardingJourney(
            key = "onboarding",
            version = 1,
            steps = listOf(OnboardingStep("usage_context", "single_choice", progress = OnboardingProgress()))
        ),
        enrollment = OnboardingEnrollment(status = "in_progress")
    )

    private companion object {
        val SERVER_JOURNEY = """
            {"success":true,"data":{"available":true,"journey":{"key":"onboarding","version":1,"steps":[{"key":"usage_context","type":"single_choice","scope":"common","config":{},"conditions":[],"supported":true,"progress":{"completed_at":"2026-09-12T12:00:00Z","value":"personal"}}]},"enrollment":{"status":"completed","completed_at":"2026-09-12T12:00:00Z","replay_count":0}}}
        """.trimIndent()
    }
}
