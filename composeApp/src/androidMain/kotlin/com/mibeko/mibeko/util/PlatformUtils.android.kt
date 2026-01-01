package com.mibeko.mibeko.util

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper object for Context injection via Koin.
 */
private object PlatformContextProvider : KoinComponent {
    val context: Context by inject()
}

actual fun getDatabaseSize(): Long {
    val context = PlatformContextProvider.context
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
