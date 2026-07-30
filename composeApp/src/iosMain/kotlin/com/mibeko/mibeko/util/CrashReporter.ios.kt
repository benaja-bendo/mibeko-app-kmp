package com.mibeko.mibeko.util

/**
 * Report iOS : délègue à Crashlytics via [FirebaseIosBridge] quand Firebase
 * est configuré, sinon journalise dans la console Xcode. Ne relance jamais
 * (appelé depuis des catch).
 */
actual fun recordException(t: Throwable, context: String?) {
    // Voir CrashReporter.android.kt : les annulations de coroutines sont un
    // fonctionnement normal, pas des erreurs à rapporter.
    if (t is kotlinx.coroutines.CancellationException) return

    val label = context ?: "unspecified"
    val description = "${t::class.simpleName}: ${t.message}"
    val record = FirebaseIosBridge.recordError
    if (record != null) {
        try {
            record(label, description)
        } catch (_: Throwable) {
            // L'échec du report ne doit pas masquer l'erreur d'origine.
        }
    } else {
        println("[Mibeko/$label] erreur non fatale: $description")
    }
}
