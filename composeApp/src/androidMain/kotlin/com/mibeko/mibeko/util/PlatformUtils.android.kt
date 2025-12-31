package com.mibeko.mibeko.util

import com.mibeko.mibeko.MibekoApp
import java.text.SimpleDateFormat
import java.util.*

actual fun getDatabaseSize(): Long {
    val context = MibekoApp.INSTANCE.applicationContext
    val dbFile = context.getDatabasePath("mibeko.db")
    return if (dbFile.exists()) {
        dbFile.length()
    } else {
        0L
    }
}

actual fun formatTimestampToDate(timestamp: Long): String {
    if (timestamp == 0L) return "Jamais"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
