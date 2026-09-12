package com.mibeko.mibeko.ui.auth

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.util.AnalyticsManager
import com.mibeko.mibeko.util.MibekoAnalytics
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class NoOpAnalyticsManager : AnalyticsManager {
    override fun logEvent(name: String, params: Map<String, Any>?) {}
    override fun setCollectionEnabled(enabled: Boolean) {}
}

/**
 * mibeko-dashboard#135 : `selectProfileType` doit omettre `phone`/`company` du
 * corps JSON envoyé (clé absente = inchangé côté serveur), jamais les envoyer
 * vides — c'est exactement le bug qui écrasait un téléphone/une organisation
 * déjà saisis sur le web. On capture le JSON réellement sérialisé par Ktor
 * (pas seulement l'objet Kotlin construit) pour prouver l'effet de
 * `encodeDefaults = false` sur des champs désormais nullables.
 *
 * `Dispatchers.Main` est mis sur un dispatcher RÉEL (`Dispatchers.Default`),
 * pas un `TestDispatcher` à temps virtuel : `AuthApiService`/`HttpClient`
 * exécutent l'appel via `MockEngine` sur leur propre dispatcher interne, hors
 * du scheduler de test — `advanceUntilIdle()` ne verrait jamais ce travail.
 * On attend donc la fin réelle du job (polling borné par un timeout) plutôt
 * que d'avancer un temps virtuel qui ne couvre pas ce dispatcher.
 */
class ProfileSetupViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectProfileType omet phone et company et envoie seulement profession`() = runBlocking {
        var capturedBody: String? = null
        val mockEngine = MockEngine { request: HttpRequestData ->
            capturedBody = when (val content = request.body) {
                is TextContent -> content.text
                is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                else -> error("Corps de requête inattendu : ${content::class}")
            }
            respond(
                content = """{"success":true,"data":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = false })
            }
        }
        val authApiService = AuthApiService(client, "https://api.test")
        val userPreferences = UserPreferencesRepository(MapSettings())
        val analytics = MibekoAnalytics(NoOpAnalyticsManager(), userPreferences)
        val viewModel = ProfileSetupViewModel(authApiService, userPreferences, analytics)

        viewModel.selectProfileType(ProfileType.CITIZEN)

        withTimeout(5_000) {
            while (viewModel.setupState.value == ProfileSetupState.Idle || viewModel.setupState.value == ProfileSetupState.Loading) {
                delay(10)
            }
        }

        val body = requireNotNull(capturedBody) {
            "Aucune requête capturée — état du ViewModel : ${viewModel.setupState.value}"
        }
        assertFalse(body.contains("\"phone\""), "phone ne doit pas être sérialisé : $body")
        assertFalse(body.contains("\"company\""), "company ne doit pas être sérialisé : $body")
        assertTrue(body.contains("\"profession\":\"Citoyen\""), "profession attendu dans le corps : $body")
    }
}
