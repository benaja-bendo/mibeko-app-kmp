package com.mibeko.mibeko.util

/**
 * Point de report d'erreur commun, minimal et multiplateforme.
 *
 * Objectif (audit 07/2026, P2.12b) : les erreurs aujourd'hui « avalées »
 * (catch qui affiche un message générique ou retourne une liste vide) laissent
 * les crashs opaques — notamment le chemin favoris. On les remonte désormais à
 * un collecteur pour pouvoir diagnostiquer sans stacktrace manuel.
 *
 * - Android : `FirebaseCrashlytics.getInstance().recordException(t)` (voir
 *   CrashReporter.android.kt). Crashlytics est initialisé automatiquement par
 *   l'app Firebase déjà configurée (google-services.json).
 * - iOS : no-op journalisé (voir CrashReporter.ios.kt). Le SDK Crashlytics iOS
 *   passe par un pod/SPM non branché ici → hors périmètre (userAction).
 *
 * `context` : étiquette courte du site d'appel (ex : "toggleFavorite"), reprise
 * comme clé Crashlytics pour regrouper/filtrer les rapports côté console.
 *
 * Cette fonction ne doit JAMAIS relancer : elle est appelée depuis des `catch`
 * et son échec éventuel ne doit pas masquer l'erreur d'origine.
 */
expect fun recordException(t: Throwable, context: String? = null)
