package com.mibeko.mibeko.data

import com.mibeko.mibeko.util.ArticleTable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Les tableaux transitent en JSON dans `articles.tables_json`.
 *
 * Ce que ces tests protègent : un corpus partiellement migré (colonne absente,
 * JSON écrit par une version antérieure, donnée tronquée) ne doit JAMAIS
 * empêcher d'ouvrir un article. Au pire on perd les colonnes, jamais le texte.
 */
class ArticleTablesStorageTest {

    private val budget = ArticleTable(
        caption = "Crédits ouverts",
        headers = listOf("Chapitre", "Montant"),
        rows = listOf(listOf("3-2-1", "50.000.000")),
        line_start = 0,
        line_end = 2
    )

    @Test
    fun allerRetourFidele() {
        val json = encodeArticleTables(listOf(budget))

        assertEquals(listOf(budget), decodeArticleTables(json))
    }

    @Test
    fun aucunTableauNEcritRien() {
        assertNull(encodeArticleTables(emptyList()))
    }

    @Test
    fun colonneAbsenteOuVideDonneAucunTableau() {
        assertEquals(emptyList(), decodeArticleTables(null))
        assertEquals(emptyList(), decodeArticleTables(""))
        assertEquals(emptyList(), decodeArticleTables("   "))
    }

    @Test
    fun jsonCorrompuNeFaitPasEchouerLaLecture() {
        assertEquals(emptyList(), decodeArticleTables("{pas du json"))
        assertEquals(emptyList(), decodeArticleTables("[{\"headers\": 42}]"))
    }

    @Test
    fun uneCleInconnueEstIgnoree() {
        // Le serveur peut enrichir la forme avant que l'app ne soit mise à jour.
        val json = "[{\"headers\":[\"A\"],\"rows\":[[\"1\"]],\"nouveaute\":true}]"

        assertEquals(listOf(listOf("1")), decodeArticleTables(json).single().rows)
    }
}
