package com.mibeko.mibeko.util

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verrouille le passage de relais des liens entre deux compositions (kmp#51).
 *
 * Le bug réel : sur Android, un lien qui démarre l'application fait relancer
 * l'activité. La nouvelle composition enregistrait son écouteur, puis
 * l'ancienne, en se détruisant, remettait l'écouteur à `null` : tous les liens
 * suivants étaient ignorés, application ouverte.
 */
class ExternalUriHandlerTest {

    @AfterTest
    fun reset() {
        ExternalUriHandler.listener = null
    }

    @Test
    fun `l ancienne composition ne retire pas l ecouteur de la nouvelle`() {
        val recus = mutableListOf<String>()
        val ancien: (String) -> Unit = { recus += "ancien:$it" }
        val nouveau: (String) -> Unit = { recus += "nouveau:$it" }

        ExternalUriHandler.listener = ancien
        ExternalUriHandler.listener = nouveau
        ExternalUriHandler.unregister(ancien)
        ExternalUriHandler.onNewUri("https://mibeko.fr/textes/code-penal")

        assertEquals(listOf("nouveau:https://mibeko.fr/textes/code-penal"), recus)
    }

    @Test
    fun `une composition retire bien son propre ecouteur`() {
        val ecouteur: (String) -> Unit = {}
        ExternalUriHandler.listener = ecouteur
        ExternalUriHandler.unregister(ecouteur)
        assertNull(ExternalUriHandler.listener)
    }

    @Test
    fun `un lien arrive avant l interface est remis au premier ecouteur`() {
        val recus = mutableListOf<String>()
        ExternalUriHandler.onNewUri("mibeko://textes/constitution")
        ExternalUriHandler.listener = { recus += it }
        assertEquals(listOf("mibeko://textes/constitution"), recus)
    }
}
