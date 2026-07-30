package com.mibeko.mibeko.data.repository

/**
 * Décision de parcours d'une page de catalogue pendant la synchronisation
 * initiale.
 */
sealed class SyncPageDecision {
    /** Traiter cette page, puis passer à [nextPage]. */
    data class Process(val nextPage: Int) : SyncPageDecision()

    /** Le curseur pointe au-delà du catalogue : reprendre depuis la page 1. */
    data object RestartFromFirstPage : SyncPageDecision()

    /** Parcours terminé. */
    data object Finished : SyncPageDecision()
}

/**
 * Logique de pagination de la synchronisation reprenable, isolée pour être
 * testable sans réseau ni base.
 *
 * Elle existe parce qu'une boucle à pré-test s'était révélée inerte : le
 * nombre total de pages n'est connu qu'après le premier appel, et un curseur
 * de reprise (toujours ≥ 2) sortait immédiatement de la boucle en laissant
 * croire à une synchronisation terminée.
 */
object SyncPagination {

    /** Page à demander au démarrage, à partir du curseur persisté. */
    fun startPage(cursor: Int): Int = if (cursor > 0) cursor else 1

    /**
     * Que faire une fois la page [currentPage] récupérée, sachant que le
     * serveur annonce [totalPages] pages au total.
     */
    fun decide(currentPage: Int, totalPages: Int): SyncPageDecision = when {
        // Catalogue rétréci depuis l'interruption : le curseur pointe dans le
        // vide. On repart du début plutôt que de conclure « terminé » sur un
        // corpus partiel.
        currentPage > totalPages && currentPage > 1 -> SyncPageDecision.RestartFromFirstPage
        currentPage > totalPages -> SyncPageDecision.Finished
        else -> SyncPageDecision.Process(nextPage = currentPage + 1)
    }

    /** Le parcours est-il terminé après avoir traité [processedPage] ? */
    fun isComplete(processedPage: Int, totalPages: Int): Boolean = processedPage >= totalPages
}
