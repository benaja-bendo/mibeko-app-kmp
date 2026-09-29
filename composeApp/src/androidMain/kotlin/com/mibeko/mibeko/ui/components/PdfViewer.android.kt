package com.mibeko.mibeko.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.util.recordException

@Composable
actual fun PdfViewer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    var failure by remember(url) { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Document PDF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pour une meilleure expérience de lecture, ce document s'ouvrira dans votre lecteur PDF système.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { failure = openPdf(context, url) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir le document", fontWeight = FontWeight.Bold)
                }

                failure?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private const val NO_PDF_APP_MESSAGE =
    "Aucune application de ce téléphone ne sait ouvrir ce PDF. Installez un lecteur PDF, puis réessayez."

/**
 * Ouvre le PDF dans une application du téléphone (kmp#23). Sans lecteur PDF
 * capable d'ouvrir une adresse web, on se replie sur le navigateur, qui sait
 * l'afficher ou le télécharger : l'adresse est publique. Renvoie le message à
 * afficher si rien n'a pu l'ouvrir, `null` sinon. Jusqu'ici, l'échec était
 * avalé et le bouton ne faisait simplement rien.
 */
private fun openPdf(context: Context, url: String): String? {
    val uri = Uri.parse(url)
    val attempts = listOf(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf"),
        Intent(Intent.ACTION_VIEW, uri)
    )
    for (intent in attempts) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
            return null
        } catch (_: ActivityNotFoundException) {
            // Aucune application pour cette forme d'intent : essai suivant.
        } catch (e: Exception) {
            recordException(e, context = "PdfViewer.openPdf")
            return NO_PDF_APP_MESSAGE
        }
    }
    return NO_PDF_APP_MESSAGE
}
