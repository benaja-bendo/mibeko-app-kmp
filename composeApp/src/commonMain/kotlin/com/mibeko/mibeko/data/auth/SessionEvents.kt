package com.mibeko.mibeko.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Diffuse les événements de session transverses.
 *
 * Le client HTTP notifie ici l'expiration de session (401 sur une route
 * authentifiée : jeton révoqué ou expiré côté serveur) après avoir purgé la
 * session locale ; l'UI (App) observe [sessionExpired] pour ramener
 * l'utilisateur à l'écran de connexion au lieu de le laisser dans une
 * session zombie.
 */
class SessionEvents {

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
