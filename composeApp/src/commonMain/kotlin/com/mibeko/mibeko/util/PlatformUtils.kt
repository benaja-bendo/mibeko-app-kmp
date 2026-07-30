package com.mibeko.mibeko.util

import kotlin.math.*

/**
 * Platform-independent utility functions.
 */
expect fun getDatabaseSize(): Long

expect fun formatTimestampToDate(timestamp: Long): String

/**
 * Récupère un identifiant unique pour l'appareil.
 */
expect fun getDeviceId(): String

/**
 * Copies text to the system clipboard.
 */
expect fun copyToClipboard(text: String)

/**
 * Version marketing réelle du binaire installé (versionName Android,
 * CFBundleShortVersionString iOS) — remplace le « v1.0.0 » en dur des
 * Réglages et alimente le comparateur de VersionGate.
 */
expect fun getAppVersionName(): String

/**
 * Demande de notation in-app (Play ReviewManager / SKStoreReviewController).
 * Fire-and-forget : l'OS décide seul d'afficher ou non la boîte de dialogue,
 * et la demande ne doit JAMAIS être conditionnée à un avis positif
 * (guideline Apple 5.6.1).
 */
expect fun requestInAppReview()

// shareText removed to use ContentSharer service instead

/**
 * Formats a size in bytes to a human-readable string (KB, MB).
 */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    // Simple rounding for commonMain
    val rounded = ((value * 10).toInt() / 10.0)
    return "$rounded ${units[digitGroups]}"
}
