package com.mibeko.mibeko.util

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
        val month = dateTime.month.toString().padStart(2, '0')
        val year = dateTime.year
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "$day/$month/$year à $hour:$minute"
    } catch (e: Exception) {
        try {
            // Try parsing as Date only (e.g. 2024-05-12)
            val date = LocalDate.parse(isoString)
            val day = date.day.toString().padStart(2, '0')
            val month = date.month.toString().padStart(2, '0')
            val year = date.year
            return "$day/$month/$year"
        } catch (e2: Exception) {
            return isoString
        }
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
