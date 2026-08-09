package com.mibeko.mibeko.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cas de référence repris du jumeau `mibeko-site/src/lib/tables.ts` : les deux
 * implémentations doivent segmenter le même contenu de la même façon.
 */
class LegalTablesTest {

    @Test
    fun tableauSimpleSansEnTete() {
        val content = "<table><tr><td>1</td><td>2</td></tr><tr><td>3</td><td>4</td></tr></table>"

        val segments = articleSegments(content)

        assertEquals(1, segments.size)
        val table = (segments[0] as ContentSegment.Table).table
        // Deux rangées entièrement numériques : aucune n'est un en-tête.
        assertEquals(emptyList<String>(), table.headers)
        assertEquals(listOf(listOf("1", "2"), listOf("3", "4")), table.rows)
    }

    @Test
    fun premiereRangeeToutEnLettresDevientEnTete() {
        val content = """
            <table>
              <tr><td>Sommets</td><td>Longitudes</td></tr>
              <tr><td>A</td><td>11</td></tr>
            </table>
        """.trimIndent()

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(listOf("Sommets", "Longitudes"), table.headers)
        assertEquals(listOf(listOf("A", "11")), table.rows)
    }

    @Test
    fun rangeeUniqueNestJamaisUnEnTete() {
        val content = "<table><tr><td>Sommets</td><td>Longitudes</td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(emptyList<String>(), table.headers)
        assertEquals(listOf(listOf("Sommets", "Longitudes")), table.rows)
    }

    @Test
    fun rangeeAvecCelluleNumeriqueResteUneRangeeDeDonnees() {
        val content = "<table><tr><td>3-2-1</td><td>Budget</td></tr><tr><td>a</td><td>b</td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(emptyList<String>(), table.headers)
        assertEquals(2, table.rows.size)
    }

    @Test
    fun colspanEstAplatiEnCellulesVides() {
        val content = "<table><tr><td colspan=\"3\">2026</td></tr>" +
            "<tr><td>a</td><td>b</td><td>c</td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        // La cellule fusionnée occupe bien trois colonnes : la rangée suivante
        // reste alignée sous les bonnes valeurs.
        assertEquals(listOf("2026", "", ""), table.rows[0])
        assertEquals(listOf("a", "b", "c"), table.rows[1])
    }

    @Test
    fun colspanAberrantCompteePourUneCellule() {
        // Au-delà de 32 c'est du bruit OCR : gonfler la rangée de milliers de
        // cellules vides serait pire que le mal. Même décision dans les deux
        // jumeaux ; le pipeline, lui, en fait une anomalie de curation.
        val content = "<table><tr><td colspan='9999'>1</td></tr><tr><td>2</td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(listOf("1"), table.rows[0])
    }

    @Test
    fun entitesHtmlSontDecodees() {
        val content = "<table><tr><td>11&#xB0; 22&#x27;22, 40&#x27; E</td>" +
            "<td>Alpha &amp; B&eacute;ta</td><td>&lt;seuil&gt;</td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(listOf("11° 22'22, 40' E", "Alpha & Béta", "<seuil>"), table.rows[0])
    }

    @Test
    fun entiteDecimaleEstDecodee() {
        assertEquals("l'article", decodeEntities("l&#39;article"))
    }

    @Test
    fun entiteInconnueResteTelleQuelle() {
        // Le texte est officiel : à défaut de savoir décoder, on ne devine pas.
        assertEquals("&inconnue; & suite", decodeEntities("&inconnue; &amp; suite"))
    }

    @Test
    fun balisesInternesEtEspacesSontNormalisesDansUneCellule() {
        val content = "<table><tr><td>  <b>Zone</b>\n   nord  </td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(listOf("Zone nord"), table.rows[0])
    }

    @Test
    fun texteEtTableauAlternentDansLOrdre() {
        val content = "Les sommets sont ainsi fixés :\n" +
            "<table><tr><td>A</td><td>11</td></tr></table>\n" +
            "Le présent arrêté prend effet immédiatement."

        val segments = articleSegments(content)

        assertEquals(3, segments.size)
        assertTrue((segments[0] as ContentSegment.Text).text.startsWith("Les sommets"))
        assertTrue(segments[1] is ContentSegment.Table)
        assertTrue((segments[2] as ContentSegment.Text).text.contains("prend effet"))
    }

    @Test
    fun contenuSansTableauRessortInchange() {
        val content = "Article premier. — La présente loi\nfixe les règles applicables."

        val segments = articleSegments(content)

        assertEquals(listOf(ContentSegment.Text(content)), segments)
        assertEquals(content, articlePlainText(content))
        assertFalse(hasRawTableMarkup(content))
    }

    @Test
    fun contenuVideNeProduitAucunSegment() {
        assertEquals(emptyList<ContentSegment>(), articleSegments(null))
        assertEquals(emptyList<ContentSegment>(), articleSegments(""))
        assertEquals(emptyList<ContentSegment>(), articleSegments("   \n  "))
        assertEquals("", articlePlainText(null))
    }

    @Test
    fun tableauSansRangeeExploitableNestPasEscamote() {
        val content = "Avant<table><caption>vide</caption></table>Après"

        val plainText = articlePlainText(content)

        assertTrue(plainText.contains("Avant"))
        assertTrue(plainText.contains("vide"))
        assertTrue(plainText.contains("Après"))
        assertFalse(plainText.contains('<'))
    }

    @Test
    fun htmlMalformeNePerdAucunTexte() {
        // `<table>` jamais refermé : la segmentation garde tout le texte…
        val content = "Texte avant\n<table><tr><td>A</td><td>11</td></tr>\nTexte après"

        val segments = articleSegments(content)

        assertEquals(1, segments.size)
        assertEquals(content, (segments[0] as ContentSegment.Text).text)

        // … et la mise en texte pur en retire le balisage sans rien perdre.
        val plainText = articlePlainText(content)
        assertTrue(plainText.contains("Texte avant"))
        assertTrue(plainText.contains("A"))
        assertTrue(plainText.contains("11"))
        assertTrue(plainText.contains("Texte après"))
        assertFalse(plainText.contains('<'))
    }

    @Test
    fun articlePlainTextNeLaisseAucuneBalise() {
        val content = "Coordonnées :\n<table><tr><td>Sommets</td><td>Longitudes</td></tr>" +
            "<tr><td>A</td><td>11&#xB0; 22&#x27;22, 40&#x27; E</td></tr></table>"

        val plainText = articlePlainText(content)

        assertEquals(
            "Coordonnées :\n\nSommets | Longitudes\nA | 11° 22'22, 40' E",
            plainText
        )
        assertFalse(plainText.contains('<'))
        assertFalse(plainText.contains('>'))
    }

    @Test
    fun linearisationPlaceLIntituleEnTete() {
        val table = LegalTable(
            caption = "Tableau 1 — Répartition",
            headers = listOf("Poste", "Montant"),
            rows = listOf(listOf("Fonctionnement", "1 250 000"))
        )

        assertEquals(
            "Tableau 1 — Répartition\nPoste | Montant\nFonctionnement | 1 250 000",
            linearizeTable(table)
        )
    }

    @Test
    fun linearisationSansIntituleNiEnTete() {
        val table = LegalTable(rows = listOf(listOf("A", "11"), listOf("B", "12")))

        assertEquals("A | 11\nB | 12", linearizeTable(table))
    }

    @Test
    fun baliseVoisineNestPasPriseePourUnTableau() {
        // `<tableau>` n'est pas `<table>` : la frontière de mot du jumeau.
        assertFalse(hasRawTableMarkup("<tableau>Annexe</tableau>"))
        assertTrue(hasRawTableMarkup("<TABLE class=\"x\">"))
    }

    @Test
    fun rangeesSontDetecteesQuelleQueSoitLaCasse() {
        val content = "<TABLE><TR><TD>A</TD><TD>11</TD></TR></TABLE>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(listOf(listOf("A", "11")), table.rows)
    }

    @Test
    fun cellulesThSontLuesCommeDesCellules() {
        val content = "<table><tr><th>Poste</th><th>Montant</th></tr><tr><td>a</td><td>1</td></tr></table>"

        val table = (articleSegments(content).single() as ContentSegment.Table).table

        assertEquals(listOf("Poste", "Montant"), table.headers)
        assertEquals(listOf(listOf("a", "1")), table.rows)
    }
}

/**
 * Chemin structuré : les tableaux arrivent par la synchronisation, ancrés sur
 * les lignes qu'ils occupent dans le texte linéarisé. C'est le cas normal
 * depuis la normalisation du corpus ; le HTML n'est plus qu'un repli hérité.
 */
class LegalTablesStructureesTest {

    private val budget = ArticleTable(
        caption = "Crédits ouverts",
        headers = listOf("Chapitre", "Montant"),
        rows = listOf(listOf("3-2-1", "50.000.000")),
        line_start = 1,
        line_end = 3
    )

    @Test
    fun remplaceLesLignesAncreesEtGardeLeTexteAutour() {
        val content = "Introduction.\nChapitre | Montant\n3-2-1 | 50.000.000\nFait à Brazzaville."

        val segments = articleSegments(content, listOf(budget))

        assertEquals(3, segments.size)
        assertEquals("Introduction.", (segments[0] as ContentSegment.Text).text)
        val table = (segments[1] as ContentSegment.Table).table
        assertEquals(listOf("Chapitre", "Montant"), table.headers)
        assertEquals("Crédits ouverts", table.caption)
        assertEquals("Fait à Brazzaville.", (segments[2] as ContentSegment.Text).text)
    }

    @Test
    fun laStructurePrimeSurLeBalisageResiduel() {
        // Un corpus à moitié migré peut porter les deux : la structure fait foi.
        val content = "<table><tr><td>vieux</td></tr></table>"

        val segments = articleSegments(content, listOf(budget.copy(line_start = null, line_end = null)))

        assertEquals(1, segments.count { it is ContentSegment.Table })
        assertEquals(listOf("Chapitre", "Montant"), (segments.first { it is ContentSegment.Table } as ContentSegment.Table).table.headers)
    }

    @Test
    fun unTableauSansAncrageEstRenduEnFinPlutotQuePerdu() {
        val segments = articleSegments("Texte seul.", listOf(budget.copy(line_start = null, line_end = null)))

        assertEquals(2, segments.size)
        assertTrue(segments[0] is ContentSegment.Text)
        assertTrue(segments[1] is ContentSegment.Table)
    }

    @Test
    fun desBornesAuDelaDuTexteNAmputentRien() {
        val segments = articleSegments("une seule ligne", listOf(budget.copy(line_start = 0, line_end = 99)))

        assertEquals(1, segments.size)
        assertTrue(segments[0] is ContentSegment.Table)
    }

    @Test
    fun deuxAncragesQuiSeChevauchentNeProduisentQuUnTableau() {
        val content = "a\nb\nc\nd"

        val segments = articleSegments(
            content,
            listOf(budget.copy(line_start = 0, line_end = 3), budget.copy(line_start = 1, line_end = 2))
        )

        assertEquals(1, segments.count { it is ContentSegment.Table })
    }

    @Test
    fun leTextePurLinearisePasseParLaStructure() {
        val content = "Introduction.\nChapitre | Montant\n3-2-1 | 50.000.000"

        val plain = articlePlainText(content, listOf(budget))

        assertFalse(plain.contains("<"))
        assertTrue(plain.contains("Introduction."))
        assertTrue(plain.contains("Chapitre | Montant"))
    }
}
