package com.mibeko.mibeko.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verrouille la reprise de la synchronisation initiale.
 *
 * Un bug réel motive ces tests : la borne de pagination était initialisée à 1
 * avant une boucle à pré-test, si bien qu'une reprise (curseur toujours ≥ 2)
 * n'exécutait jamais le corps de boucle — puis marquait le corpus comme
 * complet. Sur un réseau coupé à mi-parcours, l'utilisateur restait bloqué sur
 * une bibliothèque tronquée que l'app affirmait à jour.
 */
class SyncPaginationTest {

    @Test
    fun `sans curseur on demarre a la premiere page`() {
        assertEquals(1, SyncPagination.startPage(0))
    }

    @Test
    fun `un curseur pose reprend a cette page`() {
        assertEquals(4, SyncPagination.startPage(4))
    }

    @Test
    fun `une reprise en milieu de catalogue traite bien la page`() {
        // Le cas qui échouait : page 2 sur 8, la page DOIT être traitée.
        val decision = SyncPagination.decide(currentPage = 2, totalPages = 8)
        assertEquals(3, assertIs<SyncPageDecision.Process>(decision).nextPage)
    }

    @Test
    fun `la premiere page d un catalogue mono-page est traitee`() {
        val decision = SyncPagination.decide(currentPage = 1, totalPages = 1)
        assertEquals(2, assertIs<SyncPageDecision.Process>(decision).nextPage)
    }

    @Test
    fun `la derniere page est traitee puis le parcours se termine`() {
        assertIs<SyncPageDecision.Process>(SyncPagination.decide(currentPage = 8, totalPages = 8))
        assertTrue(SyncPagination.isComplete(processedPage = 8, totalPages = 8))
    }

    @Test
    fun `le parcours n est pas termine avant la derniere page`() {
        assertFalse(SyncPagination.isComplete(processedPage = 2, totalPages = 8))
    }

    @Test
    fun `un curseur au-dela du catalogue relance depuis le debut`() {
        // Le catalogue a rétréci pendant l'interruption : conclure « terminé »
        // laisserait un corpus partiel définitivement incomplet.
        assertIs<SyncPageDecision.RestartFromFirstPage>(
            SyncPagination.decide(currentPage = 9, totalPages = 3)
        )
    }

    @Test
    fun `un catalogue vide se termine sans boucler`() {
        assertIs<SyncPageDecision.Finished>(SyncPagination.decide(currentPage = 1, totalPages = 0))
    }

    @Test
    fun `un parcours complet visite toutes les pages depuis une reprise`() {
        // Simulation de bout en bout : reprise à la page 3 d'un catalogue de 8.
        val visited = mutableListOf<Int>()
        var page = SyncPagination.startPage(3)
        val totalPages = 8

        while (true) {
            when (val decision = SyncPagination.decide(page, totalPages)) {
                is SyncPageDecision.Finished -> break
                is SyncPageDecision.RestartFromFirstPage -> page = 1
                is SyncPageDecision.Process -> {
                    visited += page
                    val processed = page
                    page = decision.nextPage
                    if (SyncPagination.isComplete(processed, totalPages)) break
                }
            }
        }

        assertEquals(listOf(3, 4, 5, 6, 7, 8), visited)
    }
}
