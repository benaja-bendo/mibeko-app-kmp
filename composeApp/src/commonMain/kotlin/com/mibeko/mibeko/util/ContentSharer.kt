package com.mibeko.mibeko.util

interface ContentSharer {
    fun shareText(text: String, title: String? = null)
    fun shareUrl(url: String, title: String? = null)
    fun shareFile(bytes: ByteArray, fileName: String, mimeType: String)
    fun viewFile(bytes: ByteArray, fileName: String, mimeType: String)
    fun copyToClipboard(text: String)
}
