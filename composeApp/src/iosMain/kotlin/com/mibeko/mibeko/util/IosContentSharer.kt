package com.mibeko.mibeko.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.*

class IosContentSharer : ContentSharer {
    
    override fun shareText(text: String, title: String?) {
        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        val rootViewController = window?.rootViewController
        if (rootViewController != null) {
            val activityViewController = UIActivityViewController(listOf(text), null)
            rootViewController.presentViewController(activityViewController, true, null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun shareFile(bytes: ByteArray, fileName: String, mimeType: String) {
        val fileManager = NSFileManager.defaultManager
        val documentDirectory = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        
        val fileURL = documentDirectory?.URLByAppendingPathComponent(fileName)
        
        if (fileURL != null) {
            bytes.toNSData().writeToURL(fileURL, true)
            
            // Partager le fichier
            val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
            val rootViewController = window?.rootViewController
            if (rootViewController != null) {
                val activityViewController = UIActivityViewController(listOf(fileURL), null)
                rootViewController.presentViewController(activityViewController, true, null)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
        }
    }
}
