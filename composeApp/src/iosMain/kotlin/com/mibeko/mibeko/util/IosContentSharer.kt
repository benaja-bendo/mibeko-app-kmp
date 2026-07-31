package com.mibeko.mibeko.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.darwin.*
import platform.Foundation.*
import platform.UIKit.*

class IosContentSharer : ContentSharer {
    
    override fun shareText(text: String, title: String?) {
        val viewController = getTopMostViewController() ?: return
        
        // Extract URL if present to provide rich preview on iOS
        val urlRegex = "(https?://[\\w\\d./?=#-]+)".toRegex()
        val urlMatch = urlRegex.find(text)
        val nsUrl = urlMatch?.value?.let { NSURL.URLWithString(it) }
        
        val items = if (nsUrl != null) {
            // If we have a URL, provide both text and URL for rich preview
            listOf(text, nsUrl)
        } else {
            listOf(text)
        }
        
        val activityViewController = UIActivityViewController(items, null)
        configurePopover(activityViewController, viewController)
        
        dispatch_async(dispatch_get_main_queue()) {
            viewController.presentViewController(activityViewController, true, null)
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
            
            val viewController = getTopMostViewController() ?: return
            val activityViewController = UIActivityViewController(listOf(fileURL), null)
            configurePopover(activityViewController, viewController)
            
            dispatch_async(dispatch_get_main_queue()) {
                viewController.presentViewController(activityViewController, true, null)
            }
        }
    }

    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    /**
     * Find the topmost view controller to present from.
     * This handles cases where modals are already presented.
     */
    private fun getTopMostViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.keyWindow 
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        var topController = window?.rootViewController
        while (topController?.presentedViewController != null) {
            topController = topController.presentedViewController
        }
        return topController
    }

    /**
     * Configure popover presentation for iPad.
     * On iPad, UIActivityViewController MUST be presented as a popover.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun configurePopover(activityVC: UIActivityViewController, sourceVC: UIViewController) {
        activityVC.popoverPresentationController?.let { popover ->
            popover.sourceView = sourceVC.view
            // Center the popover on screen
            sourceVC.view.bounds.useContents {
                popover.sourceRect = CGRectMake(
                    size.width / 2,
                    size.height / 2,
                    0.0,
                    0.0
                )
            }
            popover.permittedArrowDirections = 0u // No arrow
        }
    }

    @OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
        }
    }
}

