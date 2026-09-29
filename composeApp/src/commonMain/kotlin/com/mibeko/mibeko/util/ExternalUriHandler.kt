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

    /**
     * Retire [listener] seulement s'il est encore l'écouteur courant.
     *
     * Sur Android, un lien qui démarre l'application fait relancer l'activité
     * par la bibliothèque de navigation : la nouvelle composition enregistre
     * son écouteur, puis l'ancienne est détruite. Un retrait inconditionnel
     * effaçait alors l'écouteur de la nouvelle, et tous les liens suivants
     * étaient ignorés, application ouverte (kmp#51).
     */
    fun unregister(listener: (String) -> Unit) {
        if (this.listener === listener) {
            this.listener = null
        }
    }

    /** Appelé par la couche native (Swift, `onNewIntent` Android) à la réception d'une URL. */
    fun onNewUri(uri: String) {
        val current = listener
        if (current != null) {
            current.invoke(uri)
        } else {
            cachedUri = uri
        }
    }
}
