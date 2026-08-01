package com.mibeko.mibeko.util

/**
 * Pattern d'état unique pour un écran chargeant des données réseau/API.
 * Règle produit non négociable (CLAUDE.md) : un état vide ne s'affiche
 * jamais sur un échec — seulement sur un [Success] dont la liste est
 * réellement vide. [Error.offline] distingue « pas de réseau » de
 * « API en échec » pour adapter le message ; [Error.retry] relance
 * exactement la même opération.
 */
sealed interface UiResult<out T> {
    data object Loading : UiResult<Nothing>
    data class Success<T>(val data: T) : UiResult<T>
    data class Error(val offline: Boolean, val retry: () -> Unit) : UiResult<Nothing>
}
