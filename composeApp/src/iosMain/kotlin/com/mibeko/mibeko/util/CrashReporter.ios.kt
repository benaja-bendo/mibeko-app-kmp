package com.mibeko.mibeko.util

/**
 * Report iOS : no-op journalisé.
 *
 * Le SDK Firebase Crashlytics iOS s'intègre via un pod (ou SPM) dans le projet
 * Xcode `iosApp`, non branché ici → hors périmètre de cette tâche (userAction :
 * ajouter `FirebaseCrashlytics` au Podfile puis relayer cet appel vers
 * `Crashlytics.crashlytics().record(error:)` via un pont Kotlin/Native).
 *
 * En attendant, on journalise l'erreur pour qu'elle reste visible dans la
 * console Xcode plutôt que d'être totalement silencieuse.
 */
actual fun recordException(t: Throwable, context: String?) {
    val prefix = if (context != null) "[Mibeko/$context]" else "[Mibeko]"
    println("$prefix erreur non fatale: ${t::class.simpleName}: ${t.message}")
}
