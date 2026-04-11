package com.mibeko.mibeko.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AndroidContentSharer(private val context: Context) : ContentSharer {
    
    override fun shareText(text: String, title: String?) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            title?.let { putExtra(Intent.EXTRA_TITLE, it) }
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title ?: "Partager").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    override fun shareFile(bytes: ByteArray, fileName: String, mimeType: String) {
        val uri = saveFileAndGetUri(bytes, fileName) ?: return
        
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("", uri)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Partager le fichier").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur de partage: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun viewFile(bytes: ByteArray, fileName: String, mimeType: String) {
        val uri = saveFileAndGetUri(bytes, fileName) ?: return
        
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback if no PDF viewer is installed
            Toast.makeText(context, "Aucune application trouvée pour ouvrir ce fichier", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveFileAndGetUri(bytes: ByteArray, fileName: String): android.net.Uri? {
        val cachePath = java.io.File(context.cacheDir, "shares")
        cachePath.mkdirs()
        
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        cachePath.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
        
        val file = java.io.File(cachePath, fileName)
        
        return try {
            java.io.FileOutputStream(file).use { outputStream ->
                outputStream.write(bytes)
            }
            
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur de préparation du fichier: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Mibeko", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun shareUrl(url: String, title: String?) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            title?.let { 
                putExtra(Intent.EXTRA_SUBJECT, it) 
                putExtra(Intent.EXTRA_TITLE, it)
            }
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title ?: "Partager le lien").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }
}

