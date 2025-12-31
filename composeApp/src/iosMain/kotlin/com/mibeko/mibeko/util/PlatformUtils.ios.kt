package com.mibeko.mibeko.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseSize(): Long {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dbFilePath = documentDirectory!!.path!! + "/mibeko.db"
    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(dbFilePath, null)
    return attributes?.get(NSFileSize) as? Long ?: 0L
}

actual fun formatTimestampToDate(timestamp: Long): String {
    if (timestamp == 0L) return "Jamais"
    val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd/MM/yyyy HH:mm"
    }
    return formatter.stringFromDate(date)
}
