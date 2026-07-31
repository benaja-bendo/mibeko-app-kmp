package com.mibeko.mibeko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Feuille de partage commune. Chaque option est optionnelle : un écran ne
 * propose que ce qui a du sens pour son contenu (ex : pas de « Partager le
 * lien » quand le slug public est inconnu — le lien retomberait sur l'accueil).
 * Le lien est volontairement la première option : c'est le format le plus
 * léger en data et celui qui ramène le destinataire vers Mibeko.
 */
@Composable
fun ShareOptionsSheet(
    title: String = "Partager",
    subtitle: String = "Choisissez comment partager cet élément",
    onSharePdf: () -> Unit,
    onShareText: (() -> Unit)? = null,
    onShareLink: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onCopyLink: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (onShareLink != null) {
            ShareOption(
                icon = Icons.Default.Link,
                title = "Partager le lien",
                subtitle = "Lien vers l'élément sur mibeko.fr",
                onClick = onShareLink
            )
        }

        if (onShareText != null) {
            ShareOption(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Partager le texte",
                subtitle = "Texte brut de l'élément",
                onClick = onShareText
            )
        }

        // « Partager », pas « Exporter » : l'action ouvre la feuille de partage
        // système avec le fichier PDF, rien n'est enregistré localement.
        ShareOption(
            icon = Icons.Default.PictureAsPdf,
            title = "Partager en PDF",
            subtitle = "Envoyer le document en fichier PDF",
            isLoading = isLoading,
            onClick = onSharePdf
        )

        if (onCopy != null || onCopyLink != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        if (onCopy != null) {
            ShareOption(
                icon = Icons.Default.ContentCopy,
                title = "Copier le texte",
                subtitle = "Dans le presse-papiers",
                onClick = onCopy
            )
        }

        if (onCopyLink != null) {
            ShareOption(
                icon = Icons.Default.ContentCopy,
                title = "Copier le lien",
                subtitle = "Dans le presse-papiers",
                onClick = onCopyLink
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Individual share option row item.
 */
@Composable
fun ShareOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
