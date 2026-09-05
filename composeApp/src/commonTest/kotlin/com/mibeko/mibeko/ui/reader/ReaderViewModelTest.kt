package com.mibeko.mibeko.ui.reader

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * buildTocRows : aplatissement du sommaire en lignes affichables
 * (mibeko-app-kmp#9 — sert aussi à calculer l'index de défilement initial
 * sur l'article courant, plutôt que de toujours ouvrir en haut).
 */
class ReaderViewModelTest {

    private fun entry(id: String, number: String, nodeTitle: String) =
        ReaderTocEntry(id = id, number = number, nodeTitle = nodeTitle)

    @Test
    fun `un en-tete est insere a chaque changement de section`() {
        val entries = listOf(
            entry("a1", "1", "Chapitre 1"),
            entry("a2", "2", "Chapitre 1"),
            entry("a3", "3", "Chapitre 2")
        )

        val rows = buildTocRows(entries)

        assertEquals(
            listOf(
                "Header(Chapitre 1)",
                "Article(1)",
                "Article(2)",
                "Header(Chapitre 2)",
                "Article(3)"
            ),
            rows.map { it.describe() }
        )
    }

    @Test
    fun `une liste vide ne produit aucune ligne`() {
        assertEquals(emptyList(), buildTocRows(emptyList()))
    }

    @Test
    fun `l'index de l'article courant tient compte des en-tetes intercales`() {
        val entries = listOf(
            entry("a1", "1", "Chapitre 1"),
            entry("a2", "2", "Chapitre 1"),
            entry("a3", "3", "Chapitre 2")
        )

        val rows = buildTocRows(entries)
        val currentIndex = rows.indexOfFirst { it is TocRow.ArticleRow && it.entry.id == "a3" }

        // Chapitre 1 (0), a1 (1), a2 (2), Chapitre 2 (3), a3 (4)
        assertEquals(4, currentIndex)
    }

    private fun TocRow.describe(): String = when (this) {
        is TocRow.Header -> "Header($nodeTitle)"
        is TocRow.ArticleRow -> "Article(${entry.number})"
    }
}
