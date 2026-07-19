package com.mibeko.mibeko.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncModelsTest {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    @Test
    fun testOfficialJournalParsing() {
        val jsonString = """
            {
                "id": "019d776d-f026-7256-af40-94a10abff1e2",
                "title": "jo05",
                "publication_date": "2026-10-31T00:00:00+00:00",
                "transcription_status": "pending",
                "is_published": true,
                "pdf_url": "http://127.0.0.1:8000/pdf-proxy/019d776d",
                "file_size_bytes": 1257311,
                "legal_documents": [],
                "created_at": "2026-04-10T12:46:26+00:00",
                "updated_at": "2026-04-10T12:46:26+00:00"
            }
        """.trimIndent()

        val parsedJournal = jsonParser.decodeFromString<RemoteOfficialJournal>(jsonString)

        assertEquals("019d776d-f026-7256-af40-94a10abff1e2", parsedJournal.id)
        assertEquals("jo05", parsedJournal.title)
        assertTrue(parsedJournal.is_published)
        assertEquals(1257311L, parsedJournal.file_size_bytes)
        assertNotNull(parsedJournal.pdf_url)
        assertEquals(0, parsedJournal.legal_documents.size)
    }

    @Test
    fun testOfficialJournalListParsing() {
        val jsonString = """
            {
                "data": [
                    {
                        "id": "019d776d-f026-7256-af40-94a10abff1e2",
                        "title": "jo05",
                        "publication_date": "2026-10-31T00:00:00+00:00",
                        "transcription_status": "pending",
                        "is_published": true,
                        "pdf_url": null,
                        "file_size_bytes": 1257311,
                        "created_at": "2026-04-10T12:46:26+00:00",
                        "updated_at": "2026-04-10T12:46:26+00:00"
                    }
                ],
                "meta": {
                    "current_page": 1,
                    "last_page": 1,
                    "per_page": 15,
                    "total": 4
                }
            }
        """.trimIndent()

        val parsedResponse = jsonParser.decodeFromString<RemoteOfficialJournalResponse>(jsonString)

        assertEquals(1, parsedResponse.data.size)
        val journal = parsedResponse.data[0]
        assertEquals("jo05", journal.title)
        assertEquals(1257311L, journal.file_size_bytes)
        
        assertNotNull(parsedResponse.meta)
        assertEquals(1, parsedResponse.meta?.current_page)
        assertEquals(4, parsedResponse.meta?.total)
    }
    
    @Test
    fun testOfficialJournalWithNullSize() {
        val jsonString = """
            {
                "id": "123",
                "title": "jo_null_size",
                "publication_date": "2026-10-31T00:00:00+00:00",
                "transcription_status": "pending",
                "is_published": true,
                "pdf_url": null,
                "file_size_bytes": null,
                "created_at": "2026-04-10T12:46:26+00:00",
                "updated_at": "2026-04-10T12:46:26+00:00"
            }
        """.trimIndent()

        val parsedJournal = jsonParser.decodeFromString<RemoteOfficialJournal>(jsonString)

        assertEquals("jo_null_size", parsedJournal.title)
        assertEquals(null, parsedJournal.file_size_bytes)
    }

    @Test
    fun testHomeDataParsing() {
        val jsonString = """
            {
                "success": true,
                "message": "Données de la page d'accueil récupérées avec succès",
                "data": {
                    "popular_codes": [
                        {
                            "id": "019d68a2-8445-71ff-ad47-b03a95a7e42c",
                            "title": "Loi sur la protection",
                            "status": "vigueur",
                            "updated_at": "2026-04-07T15:49:34+00:00"
                        }
                    ],
                    "recently_added": [
                        {
                            "id": "019d68a2-978a-7301-88aa-ccb1bf4e6f36",
                            "title": "Loi création hopital",
                            "status": "vigueur",
                            "updated_at": "2026-04-07T15:49:39+00:00"
                        }
                    ],
                    "ai_suggestions": [
                        "Quels sont mes droits en cas de licenciement abusif ?",
                        "Comment contester un bail commercial ?"
                    ]
                }
            }
        """.trimIndent()

        val parsedResponse = jsonParser.decodeFromString<ApiResponse<RemoteHomeData>>(jsonString)
        assertTrue(parsedResponse.success)
        assertNotNull(parsedResponse.data)
        
        val homeData = parsedResponse.data!!
        assertEquals(1, homeData.popular_codes.size)
        assertEquals("Loi sur la protection", homeData.popular_codes[0].title)
        
        assertEquals(1, homeData.recently_added.size)
        assertEquals("Loi création hopital", homeData.recently_added[0].title)
        
        assertEquals(2, homeData.ai_suggestions.size)
        assertEquals("Quels sont mes droits en cas de licenciement abusif ?", homeData.ai_suggestions[0])
    }

    @Test
    fun testDocumentParsesSlug() {
        // L'API (LegalDocumentResource) expose `slug` : le modèle doit le lire.
        val jsonString = """
            {
                "id": "019d68a2-8445-71ff-ad47-b03a95a7e42c",
                "title": "Code du travail",
                "slug": "code-du-travail",
                "status": "published",
                "updated_at": "2026-04-07T15:49:34+00:00"
            }
        """.trimIndent()

        val doc = jsonParser.decodeFromString<RemoteDocument>(jsonString)
        assertEquals("code-du-travail", doc.slug)
    }

    @Test
    fun testDocumentWithoutSlugIsNull() {
        // Absence de slug (JO, coquille) → null, pas d'échec de désérialisation.
        val jsonString = """
            {
                "id": "019d68a2-8445-71ff-ad47-b03a95a7e42c",
                "title": "Acte sans slug",
                "status": "published",
                "updated_at": "2026-04-07T15:49:34+00:00"
            }
        """.trimIndent()

        val doc = jsonParser.decodeFromString<RemoteDocument>(jsonString)
        assertNull(doc.slug)
    }

    @Test
    fun testPublicDocumentBySlugParsing() {
        // Réponse de GET /v1/legal-documents/slug/{slug}, réduite à l'essentiel.
        val jsonString = """
            {
                "success": true,
                "message": "ok",
                "data": {
                    "document": {
                        "id": "doc-1",
                        "title": "Code du travail",
                        "slug": "code-du-travail",
                        "status": "published",
                        "updated_at": "2026-04-07T15:49:34+00:00"
                    },
                    "articles": [
                        { "id": "art-10", "number": "10", "order": 1 },
                        { "id": "art-11", "number": "11", "order": 2 }
                    ],
                    "structure": [],
                    "has_pdf": false,
                    "current_article": null
                }
            }
        """.trimIndent()

        val response = jsonParser.decodeFromString<RemotePublicDocumentResponse>(jsonString)
        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals("doc-1", response.data?.document?.id)
        assertEquals("code-du-travail", response.data?.document?.slug)
        assertEquals(2, response.data?.articles?.size)
        assertEquals("art-11", response.data?.articles?.firstOrNull { it.number == "11" }?.id)
    }

    @Test
    fun testUserProfileParsesEmailVerification() {
        // UserProfileResource expose email_verified (bool) + email_verified_at.
        val jsonString = """
            {
                "id": "u-1",
                "name": "Jean",
                "email": "jean@example.cg",
                "email_verified": false,
                "email_verified_at": null,
                "roles": ["citizen"]
            }
        """.trimIndent()

        val user = jsonParser.decodeFromString<RemoteUser>(jsonString)
        assertEquals(false, user.email_verified)
        assertNull(user.email_verified_at)
    }

    @Test
    fun testUserWithoutVerificationFieldsDegrades() {
        // Réponse login/register (formatUser) : pas de champ email_verified →
        // null (dégradation silencieuse, aucune bannière côté UI).
        val jsonString = """
            {
                "id": "u-1",
                "name": "Jean",
                "email": "jean@example.cg",
                "roles": []
            }
        """.trimIndent()

        val user = jsonParser.decodeFromString<RemoteUser>(jsonString)
        assertNull(user.email_verified)
    }
}