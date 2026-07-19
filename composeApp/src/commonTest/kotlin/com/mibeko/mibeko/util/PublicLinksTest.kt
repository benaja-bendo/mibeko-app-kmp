package com.mibeko.mibeko.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PublicLinksTest {

    @Test
    fun documentLinkUsesTextesSlugPath() {
        assertEquals(
            "https://mibeko.fr/textes/code-du-travail",
            PublicLinks.document("code-du-travail")
        )
    }

    @Test
    fun documentLinkFallsBackToOriginWhenSlugMissing() {
        assertEquals("https://mibeko.fr", PublicLinks.document(null))
        assertEquals("https://mibeko.fr", PublicLinks.document(""))
        assertEquals("https://mibeko.fr", PublicLinks.document("   "))
    }

    @Test
    fun articleLinkMatchesAstroRoute() {
        // Parité avec articlePath : /textes/{slug}/article-{number}.
        assertEquals(
            "https://mibeko.fr/textes/code-du-travail/article-42",
            PublicLinks.article("code-du-travail", "42")
        )
    }

    @Test
    fun articleLinkEncodesNumberSegment() {
        // Un numéro « 12 bis » doit être encodé comme segment de chemin.
        val url = PublicLinks.article("code-penal", "12 bis")
        assertEquals("https://mibeko.fr/textes/code-penal/article-12%20bis", url)
    }

    @Test
    fun articleLinkFallsBackToOriginWhenSlugMissing() {
        // Sans slug, impossible de résoudre le texte : lien d'accueil, pas mort.
        assertEquals("https://mibeko.fr", PublicLinks.article(null, "42"))
        assertEquals("https://mibeko.fr", PublicLinks.article("", "42"))
    }
}
