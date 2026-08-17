package com.mibeko.mibeko.util

import kotlin.test.Test
import kotlin.test.assertEquals

/** Parité avec `mibeko-front/src/shared/lib/legalLabels.ts`. */
class LegalLabelsTest {

    @Test
    fun numeroOrdinaireDonneUnLibelleDArticle() {
        assertEquals("Article 1er", articleLeafLabel("1er"))
        assertEquals("Art. 1er", articleLeafLabel("1er", short = true))
    }

    @Test
    fun feuillesTechniquesOntLeurPropreLibelle() {
        assertEquals("Préambule", articleLeafLabel("PREAMBULE"))
        assertEquals("Signature", articleLeafLabel("SIGNATURE"))
        assertEquals("Tableau 1", articleLeafLabel("TABLEAU_1"))
        assertEquals("Tab. 2", articleLeafLabel("TABLEAU_2", short = true))
    }

    @Test
    fun tableauSansRangNestPasUnLibelleDeTableau() {
        assertEquals("Article TABLEAU_X", articleLeafLabel("TABLEAU_X"))
    }

    @Test
    fun suffixeDeCollisionEstMasqueALAffichage() {
        // `_doublon_N` est un artefact d'unicité en base, pas un numéro
        // juridique : il reste dans la donnée, jamais à l'écran.
        assertEquals("12 bis", displayArticleNumber("12 bis_doublon_3"))
        assertEquals("Article 12 bis", articleLeafLabel("12 bis_doublon_3"))
        assertEquals("Tableau 1", articleLeafLabel("TABLEAU_1_doublon_2"))
    }

    @Test
    fun numeroAbsentNeCasseRien() {
        assertEquals("", displayArticleNumber(null))
        assertEquals("Article ", articleLeafLabel(null))
    }

    @Test
    fun objetDeriveSAjouteAuTitreOfficiel() {
        assertEquals(
            "Décret n° 2025-240 du 20 juin 2025 — Nomination : président du Conseil supérieur",
            documentLineLabel(
                "Décret n° 2025-240 du 20 juin 2025.",
                "Nomination : président du Conseil supérieur",
            ),
        )
    }

    @Test
    fun titreSeulQuandAucunObjetNestConnu() {
        assertEquals("Code du travail", documentLineLabel("Code du travail", null))
        assertEquals("Code du travail", documentLineLabel("Code du travail", "   "))
    }

    @Test
    fun titreAbsentNeFabriquePasDIntitule() {
        assertEquals("Nomination", documentLineLabel(null, "Nomination"))
        assertEquals("Document", documentLineLabel(null, null))
        assertEquals("Document", documentLineLabel("", ""))
    }
}
