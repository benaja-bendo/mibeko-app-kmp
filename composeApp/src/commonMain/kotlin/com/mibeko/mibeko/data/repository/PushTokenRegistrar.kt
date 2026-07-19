package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.getPlatform
import com.mibeko.mibeko.util.getDeviceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fait suivre le token push (FCM/APNs) au backend (`/v1/devices/register`).
 *
 * Le token peut être renouvelé par la plateforme à tout moment, y compris
 * quand l'utilisateur n'est pas connecté : il est alors mis en attente dans
 * les préférences et envoyé au prochain login ou démarrage connecté.
 * L'envoi n'a lieu que si l'utilisateur a activé les notifications dans
 * l'app : c'est le réglage qui pilote l'enregistrement de l'appareil.
 */
class PushTokenRegistrar(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository
) {
    // Cycle de vie applicatif : l'envoi d'un token ne doit pas être annulé
    // par la destruction d'un écran ou du service de messaging.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Appelé par la couche native quand la plateforme délivre un nouveau token push. */
    fun onNewToken(token: String) {
        // Toujours mémorisé d'abord : en cas d'échec réseau, il sera renvoyé
        // au prochain démarrage connecté.
        userPreferencesRepository.setPendingPushToken(token)
        flushPendingToken()
    }

    /**
     * Envoie le token en attente si l'utilisateur est connecté et a activé
     * les notifications. À appeler après un login réussi et au démarrage.
     */
    fun flushPendingToken() {
        val pending = userPreferencesRepository.getPendingPushToken() ?: return
        if (!userPreferencesRepository.isLoggedIn()) return
        if (!userPreferencesRepository.isNotificationsEnabled()) return

        scope.launch {
            val platformName = getPlatform().name.lowercase()
            val backendPlatform = if (platformName.contains("android")) "android" else "ios"
            val registered = notificationRepository.registerDevice(
                deviceId = getDeviceId(),
                pushToken = pending,
                platform = backendPlatform
            )
            if (registered) {
                userPreferencesRepository.setPendingPushToken(null)
            }
        }
    }
}
