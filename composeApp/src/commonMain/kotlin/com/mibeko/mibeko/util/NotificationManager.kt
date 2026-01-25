package com.mibeko.mibeko.util

/**
 * Interface pour la gestion des notifications sur les différentes plateformes.
 */
interface NotificationManager {
    /**
     * Demande les permissions de notification à l'utilisateur.
     * @param onPermissionResult Callback appelé avec le résultat de la demande (true si accepté).
     */
    fun requestPermission(onPermissionResult: (Boolean) -> Unit)

    /**
     * Vérifie si les notifications sont activées au niveau du système.
     */
    fun isPermissionGranted(): Boolean

    /**
     * Récupère le token de notification push.
     */
    fun getPushToken(onTokenResult: (String?) -> Unit)
}

/**
 * Fonction expect pour obtenir le gestionnaire de notifications spécifique à la plateforme.
 */
expect fun getNotificationManager(): NotificationManager
