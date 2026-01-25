package com.mibeko.mibeko.util

import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNNotificationSettings

/**
 * Implémentation iOS de la gestion des notifications.
 */
class IosNotificationManager : NotificationManager {

    /**
     * Demande les permissions de notification sur iOS.
     */
    override fun requestPermission(onPermissionResult: (Boolean) -> Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        
        center.requestAuthorizationWithOptions(options) { granted, error ->
            onPermissionResult(granted)
        }
    }

    /**
     * Vérifie si les notifications sont autorisées.
     */
    override fun isPermissionGranted(): Boolean {
        // Sur iOS, la vérification est asynchrone.
        return true 
    }

    /**
     * Récupère le token FCM.
     * Note: L'intégration FCM pour iOS nécessite l'ajout du SDK Firebase iOS dans le projet Xcode
     * et la configuration du GoogleService-Info.plist.
     */
    override fun getPushToken(onTokenResult: (String?) -> Unit) {
        // Pour l'instant, iOS n'a pas encore l'intégration Firebase native.
        // On retourne null ou un token simulé pour le développement.
        // FCM v1 rejettera ce token car il n'est pas valide.
        onTokenResult("fcm_token_simulated_ios_" + getDeviceId())
    }
}

actual fun getNotificationManager(): NotificationManager {
    return IosNotificationManager()
}
