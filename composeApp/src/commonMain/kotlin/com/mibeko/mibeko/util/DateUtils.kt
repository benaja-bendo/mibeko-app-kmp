package com.mibeko.mibeko.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
