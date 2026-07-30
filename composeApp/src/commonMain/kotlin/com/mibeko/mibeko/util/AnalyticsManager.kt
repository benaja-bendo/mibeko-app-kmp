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
     * Active ou coupe la collecte au niveau du SDK (toggle de consentement
     * « Partage de statistiques anonymes » des Réglages). Coupée, plus aucun
     * événement ne quitte l'appareil, y compris ceux émis automatiquement.
     */
    fun setCollectionEnabled(enabled: Boolean)
}

/**
 * Fonction expect pour obtenir le gestionnaire d'analytics spécifique à la plateforme.
 */
expect fun getAnalyticsManager(): AnalyticsManager
