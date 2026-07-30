package com.mibeko.mibeko.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Report Android : remonte l'exception à Firebase Crashlytics comme erreur
 * non fatale. Crashlytics est initialisé par l'app Firebase (google-services),
 * donc `getInstance()` est disponible dès le démarrage.
 *
 * On protège l'appel : si Crashlytics n'est pas disponible (ex : app Firebase
 * non initialisée dans un contexte de test), on n'ajoute pas d'exception à
 * celle qu'on est en train de rapporter.
 */
actual fun recordException(t: Throwable, context: String?) {
    // Une annulation de coroutine n'est pas une anomalie : quitter un écran
    // pendant un chargement, ou atteindre un withTimeoutOrNull, en produit
    // constamment. Les remonter noierait les vraies erreurs.
    if (t is kotlinx.coroutines.CancellationException) return

    try {
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (context != null) {
            crashlytics.setCustomKey("mibeko_context", context)
            crashlytics.log("mibeko error @ $context: ${t.message}")
        }
        crashlytics.recordException(t)
    } catch (_: Throwable) {
        // Le report ne doit jamais masquer l'erreur d'origine.
    }
}
