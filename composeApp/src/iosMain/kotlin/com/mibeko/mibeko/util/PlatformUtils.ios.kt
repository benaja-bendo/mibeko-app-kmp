package com.mibeko.mibeko.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*
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

actual fun getDeviceId(): String {
    return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_ios"
}

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun shareText(text: String) {
    val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = window?.rootViewController
    if (rootViewController != null) {
        val activityViewController = UIActivityViewController(listOf(text), null)
        rootViewController.presentViewController(activityViewController, true, null)
    }
}
