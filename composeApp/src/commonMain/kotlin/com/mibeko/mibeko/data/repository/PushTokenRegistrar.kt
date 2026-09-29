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
 * L'envoi n'a lieu que si le réglage de l'application est actif (il l'est
 * par défaut, APP-004) ET si le système autorise les notifications : un
 * appareil enregistré sans autorisation recevrait des push que le système
 * jette, et gonflerait le compte des appareils joignables (kmp#34).
 */
class PushTokenRegistrar(
    private val userPreferencesRepository: UserPreferencesRepository,
    /** `NotificationRepository.registerDevice` en production ; vrai si le serveur a accepté. */
    private val registerDevice: suspend (deviceId: String, pushToken: String, platform: String) -> Boolean,
    private val isPermissionGranted: () -> Boolean,
    // Cycle de vie applicatif : l'envoi d'un token ne doit pas être annulé
    // par la destruction d'un écran ou du service de messaging.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val deviceId: () -> String = { getDeviceId() },
    private val platformName: () -> String = { getPlatform().name.lowercase() }
) {

    /** Appelé par la couche native quand la plateforme délivre un nouveau token push. */
    fun onNewToken(token: String) {
        // Toujours mémorisé d'abord : en cas d'échec réseau, il sera renvoyé
        // au prochain démarrage connecté.
        userPreferencesRepository.setPendingPushToken(token)
        userPreferencesRepository.setLastPushToken(token)
        flushPendingToken()
    }

    /**
     * (Ré)enregistre l'appareil auprès du backend. À appeler après un login
     * réussi et au démarrage.
     *
     * On repart du jeton en attente ou, à défaut, du DERNIER jeton connu :
     * l'enregistrement ne transporte pas que le jeton, il rattache aussi
     * l'appareil au compte connecté. En ne renvoyant que les jetons « en
     * attente » — effacés dès le premier envoi réussi — tous les appareils
     * déjà enregistrés restaient orphelins côté serveur, et leurs préférences
     * de veille étaient donc ignorées.
     */
    fun flushPendingToken() {
        val token = userPreferencesRepository.getPendingPushToken()
            ?: userPreferencesRepository.getLastPushToken()
            ?: return
        if (!userPreferencesRepository.isLoggedIn()) return
        if (!userPreferencesRepository.isNotificationsEnabled()) return
        if (!isPermissionGranted()) return

        scope.launch {
            val backendPlatform = if (platformName().contains("android")) "android" else "ios"
            val registered = registerDevice(deviceId(), token, backendPlatform)
            if (registered) {
                userPreferencesRepository.setPendingPushToken(null)
                userPreferencesRepository.setLastPushToken(token)
            }
        }
    }
}
