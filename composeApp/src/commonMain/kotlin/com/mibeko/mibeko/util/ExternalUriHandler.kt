package com.mibeko.mibeko.util

/**
 * Pont de deep links pour les plateformes sans gestion native par la
 * bibliothèque de navigation (iOS : `onOpenURL` SwiftUI → Compose).
 *
 * L'URI est mise en cache si elle arrive avant que l'interface ne soit prête
 * (démarrage à froid), puis consommée dès l'enregistrement de l'écouteur.
 */
object ExternalUriHandler {

    private var cachedUri: String? = null

    var listener: ((String) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cachedUri?.let { value.invoke(it) }
                cachedUri = null
            }
        }

    /** Appelé par la couche native (Swift) à la réception d'une URL. */
    fun onNewUri(uri: String) {
        val current = listener
        if (current != null) {
            current.invoke(uri)
        } else {
            cachedUri = uri
        }
    }
}
