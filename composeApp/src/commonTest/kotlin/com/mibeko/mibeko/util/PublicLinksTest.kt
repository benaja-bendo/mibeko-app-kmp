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

    @Test
    fun sharedArticleLinkCarriesUtmMarkers() {
        // kmp#67 : le bouche-à-oreille issu de l'app doit se distinguer du
        // « direct » dans Umami.
        assertEquals(
            "https://mibeko.fr/textes/code-du-travail/article-42" +
                "?utm_source=app-mibeko&utm_medium=partage&utm_campaign=link",
            PublicLinks.shared(PublicLinks.article("code-du-travail", "42"), PublicLinks.SHARE_FORMAT_LINK)
        )
    }

    @Test
    fun sharedDocumentLinkCarriesUtmMarkers() {
        assertEquals(
            "https://mibeko.fr/textes/code-du-travail" +
                "?utm_source=app-mibeko&utm_medium=partage&utm_campaign=link",
            PublicLinks.shared(PublicLinks.document("code-du-travail"), PublicLinks.SHARE_FORMAT_LINK)
        )
    }

    @Test
    fun sharedFallbackLinkPointsToHomePath() {
        // Sans slug, le partage mène à l'accueil : il reste mesuré.
        assertEquals(
            "https://mibeko.fr/?utm_source=app-mibeko&utm_medium=partage&utm_campaign=link",
            PublicLinks.shared(PublicLinks.document(null), PublicLinks.SHARE_FORMAT_LINK)
        )
    }

    @Test
    fun sharedLinkKeepsEncodedArticleNumberAndExistingQuery() {
        // Le « ? » d'un numéro reste encodé dans le chemin ; une query déjà
        // présente est prolongée, pas écrasée.
        assertEquals(
            "https://mibeko.fr/textes/code-penal/article-12%3F" +
                "?utm_source=app-mibeko&utm_medium=partage&utm_campaign=link",
            PublicLinks.shared(PublicLinks.article("code-penal", "12?"), PublicLinks.SHARE_FORMAT_LINK)
        )
        assertEquals(
            "https://mibeko.fr/textes/code-penal?au=2020-01-01" +
                "&utm_source=app-mibeko&utm_medium=partage&utm_campaign=link",
            PublicLinks.shared("https://mibeko.fr/textes/code-penal?au=2020-01-01", PublicLinks.SHARE_FORMAT_LINK)
        )
    }
}
