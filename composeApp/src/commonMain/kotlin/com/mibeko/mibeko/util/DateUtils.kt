package com.mibeko.mibeko.util

import com.mibeko.mibeko.getCurrentTimeMillis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun formatIsoDate(isoString: String): String {
    try {
        // Try parsing as full Instant first (e.g. 2024-05-12T10:00:00Z)
        val instant = Instant.parse(isoString)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dateTime.day.toString().padStart(2, '0')
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val year = dateTime.year
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "$day/$month/$year à $hour:$minute"
    } catch (e: Exception) {
        try {
            // Try parsing as Date only (e.g. 2024-05-12)
            val date = LocalDate.parse(isoString)
            val day = date.day.toString().padStart(2, '0')
            val month = date.monthNumber.toString().padStart(2, '0')
            val year = date.year
            return "$day/$month/$year"
        } catch (e2: Exception) {
            return isoString
        }
    }
}

private val FRENCH_MONTHS = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre"
)

/** Date « éditoriale » sans heure : « 12 juin 2026 ». Pour les JO et textes. */
fun formatIsoDateLong(isoString: String): String {
    val dateOnly = isoString.take(10)
    return try {
        val date = LocalDate.parse(dateOnly)
        "${date.day} ${FRENCH_MONTHS[date.monthNumber - 1]} ${date.year}"
    } catch (e: Exception) {
        isoString
    }
}

fun parseRemoteDateToEpochMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null

    try {
        return Instant.parse(raw).toEpochMilliseconds()
    } catch (_: Exception) {
    }

    try {
        val date = LocalDate.parse(raw)
        return date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    } catch (_: Exception) {
    }

    if (!raw.all { it.isDigit() }) return null
    val n = raw.toLongOrNull() ?: return null

    return when {
        raw.length >= 16 -> n / 1_000_000L
        raw.length >= 13 -> n
        raw.length >= 10 -> n * 1_000L
        else -> null
    }
}

fun formatEpochMillisDate(epochMillis: Long, includeTime: Boolean = false): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dateTime.day.toString().padStart(2, '0')
    val month = dateTime.monthNumber.toString().padStart(2, '0')
    val year = dateTime.year

    if (!includeTime) {
        return "$day/$month/$year"
    }

    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$day/$month/$year à $hour:$minute"
}

fun formatRemoteDateForUi(raw: String?): String? {
    val ms = parseRemoteDateToEpochMillis(raw) ?: return raw
    return formatEpochMillisDate(ms)
}

fun yearFromRemoteDate(raw: String?): String? {
    val ms = parseRemoteDateToEpochMillis(raw) ?: return null
    val instant = Instant.fromEpochMilliseconds(ms)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.year.toString()
}

fun yearFromEpochMillis(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.year.toString()
}

/**
 * Temps relatif court pour les listes (notifications…) : « À l'instant »,
 * « Il y a 5 min », « Hier »… Au-delà d'une semaine, on affiche la date.
 */
fun formatRelativeTime(epochMillis: Long, nowMillis: Long = getCurrentTimeMillis()): String {
    val diff = nowMillis - epochMillis
    if (diff < 0) return formatEpochMillisDate(epochMillis)

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000

    return when {
        minutes < 1 -> "À l'instant"
        minutes < 60 -> "Il y a $minutes min"
        hours == 1L -> "Il y a 1 heure"
        hours < 24 -> "Il y a $hours heures"
        days == 1L -> "Hier"
        days < 7 -> "Il y a $days jours"
        else -> formatEpochMillisDate(epochMillis)
    }
}
