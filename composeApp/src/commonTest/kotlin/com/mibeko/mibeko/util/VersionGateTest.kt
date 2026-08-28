package com.mibeko.mibeko.util

import com.mibeko.mibeko.data.remote.AppConfigResponse
import com.mibeko.mibeko.data.remote.AppStoreUrls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionGateTest {

    private fun config(min: String, latest: String, message: String? = null) = AppConfigResponse(
        min_supported_version = min,
        latest_version = latest,
        message = message,
        store_urls = AppStoreUrls(android = "https://play.google.com/x", ios = null)
    )

    // --- compare ---

    @Test
    fun `compare gere les longueurs differentes`() {
        assertEquals(0, VersionGate.compare("1.0", "1.0.0"))
        assertTrue(VersionGate.compare("1.0", "1.0.1")!! < 0)
        assertTrue(VersionGate.compare("1.10", "1.9")!! > 0)
    }

    @Test
    fun `compare tolere le prefixe v et les suffixes de pre-version`() {
        assertEquals(0, VersionGate.compare("v1.1.0", "1.1.0"))
        assertEquals(0, VersionGate.compare("1.1.0-beta", "1.1.0"))
    }

    @Test
    fun `compare retourne null sur version illisible`() {
        assertNull(VersionGate.compare("abc", "1.0"))
        assertNull(VersionGate.compare("1.0", ""))
        assertNull(VersionGate.compare("1.x.0", "1.0"))
    }

    // --- evaluate : fail-open ---

    @Test
    fun `config absente = a jour`() {
        assertIs<UpdateState.UpToDate>(VersionGate.evaluate("1.0.0", null, null))
    }

    @Test
    fun `version courante illisible = a jour donc jamais de blocage`() {
        val state = VersionGate.evaluate("garbage", config(min = "2.0", latest = "2.1"), null)
        assertIs<UpdateState.UpToDate>(state)
    }

    @Test
    fun `min illisible = pas de blocage meme si latest pousse une banniere`() {
        val state = VersionGate.evaluate("1.0.0", config(min = "n/a", latest = "1.1.0"), "url")
        assertIs<UpdateState.SoftUpdate>(state)
    }

    // --- evaluate : décisions ---

    @Test
    fun `sous le minimum = force update avec message et url`() {
        val state = VersionGate.evaluate(
            "1.0",
            config(min = "1.1.0", latest = "1.2.0", message = "Version obsolète."),
            "https://store"
        )
        val force = assertIs<UpdateState.ForceUpdate>(state)
        assertEquals("Version obsolète.", force.message)
        assertEquals("https://store", force.storeUrl)
    }

    @Test
    fun `au-dessus du minimum mais sous latest = soft update`() {
        val state = VersionGate.evaluate("1.1.0", config(min = "1.0", latest = "1.2.0"), null)
        val soft = assertIs<UpdateState.SoftUpdate>(state)
        assertEquals("1.2.0", soft.latestVersion)
    }

    @Test
    fun `a jour = rien`() {
        assertIs<UpdateState.UpToDate>(
            VersionGate.evaluate("1.2.0", config(min = "1.0", latest = "1.2.0"), null)
        )
    }

    @Test
    fun `egal au minimum = pas de blocage`() {
        val state = VersionGate.evaluate("1.1.0", config(min = "1.1.0", latest = "1.1.0"), null)
        assertIs<UpdateState.UpToDate>(state)
    }
}
