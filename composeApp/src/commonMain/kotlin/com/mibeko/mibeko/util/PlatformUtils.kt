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
