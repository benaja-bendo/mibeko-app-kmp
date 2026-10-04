package com.mibeko.mibeko.util

import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavUri
import androidx.savedstate.read
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Un lien partagé marqué UTM (kmp#67) ouvre-t-il toujours le bon texte ?
 *
 * Fait tourner le vrai moteur de correspondance d'androidx.navigation, celui
 * qu'appelle `navController.navigate(NavUri(uri))` dans App.kt, sur les motifs
 * réels de l'app ([PublicLinks.TEXTE_DEEP_LINK_PATTERNS]).
 *
 * iOS seulement : sur Android, `NavUri` est `android.net.Uri`, inutilisable
 * dans un test JVM sans Robolectric. Le moteur est le même code commun
 * d'androidx.navigation ; le parcours Android se vérifie sur émulateur.
 */
class SharedLinkDeepLinkTest {

    private val texteResolver = NavDestination("test").apply {
        PublicLinks.TEXTE_DEEP_LINK_PATTERNS.forEach { addDeepLink(it) }
    }

    /** Arguments extraits de [url], ou `null` si aucun motif ne la reconnaît. */
    private fun argumentsOf(url: String): Map<String, String?>? {
        val match = texteResolver.matchDeepLink(NavDeepLinkRequest(NavUri(url), null, null))
            ?: return null
        val args = match.matchingArgs ?: return emptyMap()
        return args.read {
            listOf("docSlug", "articleNumber").associateWith { key ->
                if (contains(key)) getString(key) else null
            }
        }
    }

    private fun shared(url: String) = PublicLinks.shared(url, PublicLinks.SHARE_FORMAT_LINK)

    @Test
    fun sharedDocumentLinkOpensSameDocument() {
        val url = PublicLinks.document("code-du-travail")
        val expected = mapOf("docSlug" to "code-du-travail", "articleNumber" to null)
        assertEquals(expected, argumentsOf(url))
        assertEquals(expected, argumentsOf(shared(url)))
    }

    @Test
    fun sharedArticleLinkOpensSameArticle() {
        val url = PublicLinks.article("code-du-travail", "42")
        val expected = mapOf("docSlug" to "code-du-travail", "articleNumber" to "42")
        assertEquals(expected, argumentsOf(url))
        assertEquals(expected, argumentsOf(shared(url)))
    }

    @Test
    fun sharedArticleLinkDecodesEncodedNumber() {
        // « 12 bis » voyage encodé dans le chemin ; « 12? » aussi, sans être
        // pris pour le début de la query.
        assertEquals(
            mapOf("docSlug" to "code-penal", "articleNumber" to "12 bis"),
            argumentsOf(shared(PublicLinks.article("code-penal", "12 bis")))
        )
        assertEquals(
            mapOf("docSlug" to "code-penal", "articleNumber" to "12?"),
            argumentsOf(shared(PublicLinks.article("code-penal", "12?")))
        )
    }

    @Test
    fun customSchemeStillMatches() {
        assertEquals(
            mapOf("docSlug" to "code-penal", "articleNumber" to "7"),
            argumentsOf("mibeko://textes/code-penal/article-7")
        )
    }
}
