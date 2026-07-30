package com.mibeko.mibeko.util

/**
 * Pont Kotlin ← Swift pour les SDK Firebase iOS.
 *
 * Les SDK Firebase (Analytics, Crashlytics) sont intégrés au projet Xcode via
 * Swift Package Manager : ils ne sont pas visibles depuis Kotlin/Native. Au
 * démarrage, `iOSApp.swift` remplit ce registre avec des closures qui délèguent
 * aux SDK ; les actuals Kotlin (AnalyticsManager.ios.kt, CrashReporter.ios.kt)
 * ne parlent qu'au registre.
 *
 * Si le registre n'est pas rempli (GoogleService-Info.plist absent du build,
 * Firebase non configuré), chaque appel retombe sur un no-op journalisé :
 * l'app reste pleinement fonctionnelle sans Firebase.
 */
object FirebaseIosBridge {
    var logEvent: ((String, Map<String, Any>?) -> Unit)? = null
    var setCollectionEnabled: ((Boolean) -> Unit)? = null

    /** (contexte d'appel, description de l'erreur) → Crashlytics.record. */
    var recordError: ((String, String) -> Unit)? = null

    val isConfigured: Boolean
        get() = logEvent != null
}
