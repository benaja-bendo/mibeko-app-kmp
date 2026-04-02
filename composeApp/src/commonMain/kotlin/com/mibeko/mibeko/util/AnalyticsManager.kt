package com.mibeko.mibeko.util

/**
 * Interface pour la gestion des événements Analytics de l'application sur les différentes plateformes.
 */
interface AnalyticsManager {
    /**
     * Enregistre un événement spécifique avec des paramètres optionnels.
     * @param name Nom de l'événement.
     * @param params Liste des paramètres associés à l'événement (optionnel).
     */
    fun logEvent(name: String, params: Map<String, Any>? = null)

    /**
     * Définit l'identifiant unique de l'utilisateur pour l'analytics.
     * @param id L'identifiant de l'utilisateur ou null pour l'effacer.
     */
    fun setUserId(id: String?)

    /**
     * Définit une propriété personnalisée pour l'utilisateur courant.
     * @param name Nom de la propriété.
     * @param value Valeur de la propriété.
     */
    fun setUserProperty(name: String, value: String)
}

/**
 * Fonction expect pour obtenir le gestionnaire d'analytics spécifique à la plateforme.
 */
expect fun getAnalyticsManager(): AnalyticsManager
