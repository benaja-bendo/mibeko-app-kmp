package com.mibeko.mibeko.util

import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized

/**
 * Implémentation iOS de la gestion des notifications.
 */
class IosNotificationManager : NotificationManager {

    // L'API UNUserNotificationCenter est asynchrone : on mémorise le dernier
    // état connu (rafraîchi à chaque interrogation et à chaque demande) au
    // lieu de l'ancien `true` codé en dur qui mentait à l'UI des Réglages.
    private var lastKnownGranted: Boolean = false

    init {
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                lastKnownGranted =
                    settings?.authorizationStatus == UNAuthorizationStatusAuthorized
            }
    }

    override fun requestPermission(onPermissionResult: (Boolean) -> Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound

        center.requestAuthorizationWithOptions(options) { granted, _ ->
            lastKnownGranted = granted
            onPermissionResult(granted)
        }
    }

    override fun isPermissionGranted(): Boolean {
        refreshPermissionStatus()
        return lastKnownGranted
    }

    /**
     * Pas de push iOS en v1.1 : l'entitlement APNs et FirebaseMessaging ne
     * sont pas branchés (chantier veille légale v1.2). Token null = l'appareil
     * n'est simplement pas enregistré. L'ancien token simulé
     * (`fcm_token_simulated_ios_…`) polluait la table `devices` en production
     * et aurait fait échouer chaque envoi FCM v1.
     */
    override fun getPushToken(onTokenResult: (String?) -> Unit) {
        onTokenResult(null)
    }
}

actual fun getNotificationManager(): NotificationManager {
    return IosNotificationManager()
}
