package com.mibeko.mibeko.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingApiServiceTest {
    @Test
    fun `journey decodes shared contract and sends mobile capabilities`() = runTest {
        var request: HttpRequestData? = null
        val client = HttpClient(MockEngine { captured ->
            request = captured
            respond(
                content = JOURNEY_RESPONSE,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = false }) }
        }

        val data = OnboardingApiService(client, "https://api.test/api")
            .journey("android", listOf("welcome", "single_choice"))

        assertTrue(data.available)
        assertEquals("in_progress", data.enrollment?.status)
        assertEquals("Bienvenue depuis l'administration", data.journey?.steps?.first()?.config?.get("title")?.jsonPrimitive?.content)
        assertEquals("Commencer le parcours", data.journey?.steps?.first()?.config?.get("cta")?.jsonPrimitive?.content)
        assertEquals("Retrouver un texte OHADA", data.journey?.steps?.first()?.config?.get("examples")?.jsonArray?.first()?.jsonPrimitive?.content)
        assertEquals("personal", data.journey?.steps?.get(1)?.progress?.value?.toString()?.trim('"'))
        assertFalse(data.journey?.steps?.first()?.progress?.resolved ?: true)
        val url = requireNotNull(request).url.toString()
        assertTrue(url.contains("platform=android"), url)
        assertTrue(url.contains("known_step_types"), url)
    }

    private companion object {
        val JOURNEY_RESPONSE = """
            {
              "success": true,
              "data": {
                "available": true,
                "journey": {
                  "key": "onboarding",
                  "version": 1,
                  "steps": [
                    {
                      "key": "welcome", "type": "welcome", "scope": "common",
                      "config": {
                        "title":"Bienvenue depuis l'administration",
                        "body":"Un contenu publié sans nouvelle version mobile.",
                        "cta":"Commencer le parcours",
                        "examples":["Retrouver un texte OHADA"]
                      },
                      "conditions": [], "supported": true,
                      "progress": {"viewed_at":null,"skipped_at":null,"completed_at":null,"value":null}
                    },
                    {
                      "key": "usage_context", "type": "single_choice", "scope": "common",
                      "config": {"options":[{"code":"personal","label_key":"onboarding.usage_context.personal"}]},
                      "binding": "profile.usage_context", "conditions": [], "supported": true,
                      "progress": {"viewed_at":null,"skipped_at":null,"completed_at":"2026-09-12T00:00:00Z","value":"personal"}
                    }
                  ]
                },
                "enrollment": {"status":"in_progress","started_at":"2026-09-12T00:00:00Z","completed_at":null,"postponed_at":null,"replay_count":0}
              }
            }
        """.trimIndent()
    }
}
