package com.mibeko.mibeko.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiStreamEventParserTest {

    // --- Événements à ignorer ---

    @Test
    fun `data null est ignore`() {
        assertNull(AiStreamEventParser.parse("status", null))
    }

    @Test
    fun `DONE est ignore quel que soit le type`() {
        assertNull(AiStreamEventParser.parse(null, "[DONE]"))
        assertNull(AiStreamEventParser.parse("message", "[DONE]"))
    }

    @Test
    fun `json malforme sans type est ignore`() {
        assertNull(AiStreamEventParser.parse(null, "pas du json {"))
    }

    @Test
    fun `evenement inconnu est ignore`() {
        assertNull(AiStreamEventParser.parse(null, """{"type":"autre_chose","delta":"x"}"""))
    }

    // --- status ---

    @Test
    fun `status avec message emet Status`() {
        val event = AiStreamEventParser.parse("status", """{"message":"Recherche des sources"}""")
        assertEquals(AiStreamEvent.Status("Recherche des sources"), event)
    }

    @Test
    fun `status sans message est ignore`() {
        assertNull(AiStreamEventParser.parse("status", """{"autre":"champ"}"""))
    }

    @Test
    fun `status malforme est ignore`() {
        assertNull(AiStreamEventParser.parse("status", "{invalide"))
    }

    // --- sources ---

    @Test
    fun `sources valides emettent Sources`() {
        val data = """[{"id":"a1","number":"12","content":"Texte","document_id":"d1",
            "document_title":"Code civil","document_type":"code","node_title":"Titre I",
            "breadcrumb":"Livre I > Titre I","score":0.9}]""".trimIndent()
        val event = AiStreamEventParser.parse("sources", data)
        val sources = (event as AiStreamEvent.Sources).sources
        assertEquals(1, sources.size)
        assertEquals("a1", sources[0].id)
        assertEquals("Code civil", sources[0].document_title)
    }

    @Test
    fun `sources malformees sont ignorees`() {
        assertNull(AiStreamEventParser.parse("sources", """{"pas":"une liste"}"""))
    }

    // --- error : jamais avale, jamais de texte serveur ---

    @Test
    fun `error avec code transporte le code seul`() {
        val event = AiStreamEventParser.parse(
            "error",
            """{"code":"AI_RATE_LIMITED","message":"Texte serveur a ne jamais afficher"}"""
        )
        assertEquals(AiStreamEvent.Error(code = "AI_RATE_LIMITED"), event)
    }

    @Test
    fun `error sans code emet Error code null`() {
        val event = AiStreamEventParser.parse("error", """{"message":"Oups"}""")
        assertEquals(AiStreamEvent.Error(code = null), event)
    }

    @Test
    fun `error malformee emet quand meme Error`() {
        val event = AiStreamEventParser.parse("error", "{{{pas du json")
        assertEquals(AiStreamEvent.Error(code = null), event)
    }

    // --- text_delta (événement sans type SSE) ---

    @Test
    fun `text_delta emet Delta`() {
        val event = AiStreamEventParser.parse(null, """{"type":"text_delta","delta":"Bonjour"}""")
        assertEquals(AiStreamEvent.Delta("Bonjour"), event)
    }

    @Test
    fun `text_delta sans delta est ignore`() {
        assertNull(AiStreamEventParser.parse(null, """{"type":"text_delta"}"""))
    }

    @Test
    fun `champs inconnus sont toleres`() {
        val event = AiStreamEventParser.parse(
            null,
            """{"type":"text_delta","delta":"a","extra":123}"""
        )
        assertEquals(AiStreamEvent.Delta("a"), event)
    }
}
