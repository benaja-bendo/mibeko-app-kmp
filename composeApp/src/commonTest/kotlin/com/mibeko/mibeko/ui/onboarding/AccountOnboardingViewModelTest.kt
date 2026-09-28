package com.mibeko.mibeko.ui.onboarding

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.LibraryApiService
import com.mibeko.mibeko.data.remote.OnboardingApiService
import com.mibeko.mibeko.data.repository.OnboardingRepository
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

private class OnlineConnectivity : NetworkConnectivityChecker {
    private val state = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = state
    override fun isNetworkAvailable(): Boolean = true
}

/**
 * Même contrainte que `ProfileSetupViewModelTest` : l'appel passe par
 * `MockEngine` sur le dispatcher interne de Ktor, hors de tout temps virtuel.
 * On attend donc la fin réelle des jobs, bornée par un délai maximal.
 */
class AccountOnboardingViewModelTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `la derniere action guidee ouvre la Bibliotheque sans revenir a l Accueil`() = runBlocking {
        var answered = false
        val engine = MockEngine { request ->
            val body = when {
                request.url.encodedPath.endsWith("/journey") ->
                    if (answered) COMPLETED_JOURNEY else PENDING_JOURNEY
                request.url.encodedPath.contains("/steps/") && request.method == HttpMethod.Patch -> {
                    answered = true
                    """{"success":true,"data":{}}"""
                }
                request.url.encodedPath.endsWith("/themes") -> """{"success":true,"data":[]}"""
                else -> """{"success":true,"data":{}}"""
            }
            respond(body, headers = headersOf("Content-Type", "application/json"))
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(json) } }
        val preferences = UserPreferencesRepository(MapSettings())
        preferences.setUserInfo("Compte A", "a@example.test", "account-a")
        val connectivity = OnlineConnectivity()
        val viewModel = AccountOnboardingViewModel(
            OnboardingRepository(OnboardingApiService(client, "https://api.test/api"), preferences, json, connectivity),
            LibraryApiService(client, "https://api.test/api"),
            connectivity
        )

        viewModel.initialize(replay = false)
        withTimeout(5_000) { while (viewModel.uiState.value.loading) delay(10) }
        assertTrue(viewModel.uiState.value.currentStep?.key == "discover_sources", "le parcours doit s ouvrir sur l action guidée : ${viewModel.uiState.value}")

        var libraryOpened = false
        viewModel.answer(after = { libraryOpened = true })
        withTimeout(5_000) { while (!libraryOpened) delay(10) }

        val state = viewModel.uiState.value
        assertTrue(answered, "la réponse doit être envoyée au serveur avant de quitter le guide")
        assertTrue(libraryOpened, "le bouton doit ouvrir la Bibliothèque")
        assertTrue(state.finished, "le serveur a clos le parcours")
        assertTrue(
            state.leftByGuidedAction,
            "la fin du parcours ne doit pas déclencher onFinished (retour à l'Accueil) après l'ouverture de la Bibliothèque"
        )
    }

    private companion object {
        const val STEP = """{"key":"discover_sources","type":"guided_action","scope":"common","config":{},"conditions":[],"supported":true,"progress":%s}"""
        val PENDING_JOURNEY = """
            {"success":true,"data":{"available":true,"journey":{"key":"onboarding","version":3,"steps":[${STEP.replace("%s", "{}")}]},"enrollment":{"status":"in_progress","replay_count":0}}}
        """.trimIndent()
        val COMPLETED_JOURNEY = """
            {"success":true,"data":{"available":true,"journey":{"key":"onboarding","version":3,"steps":[${STEP.replace("%s", "{\"completed_at\":\"2026-09-28T18:37:43Z\"}")}]},"enrollment":{"status":"completed","completed_at":"2026-09-28T18:37:43Z","replay_count":0}}}
        """.trimIndent()
    }
}
