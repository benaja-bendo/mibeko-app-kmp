package com.mibeko.mibeko.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

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
        // Use cache directory ("shares" folder must match file_paths.xml)
        val cachePath = java.io.File(context.cacheDir, "shares")
        cachePath.mkdirs()
        
        // Clean up old files to save space
        cachePath.listFiles()?.forEach { 
             // Optional: delete files older than X, or just clear all
             // For now, let's keep it simple: overwrite if same name, or just let them pile up slightly
        }
        
        val file = java.io.File(cachePath, fileName)
        
        try {
            java.io.FileOutputStream(file).use { outputStream ->
                outputStream.write(bytes)
            }
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                // Grant temporary read permission to the content URI
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // Compatibility for some apps
                clipData = android.content.ClipData.newRawUri("", uri)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Partager le fichier").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Grant permission to the chooser result as well
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Erreur de partage: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
