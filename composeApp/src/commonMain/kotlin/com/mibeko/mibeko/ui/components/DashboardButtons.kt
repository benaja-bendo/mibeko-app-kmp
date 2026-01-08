package com.mibeko.mibeko.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val MibekoPrimaryBlue = Color(0xFF1A3A6B)
private val MibekoSecondaryBlue = Color(0xFF2E5A9C)

/**
 * Dashboard buttons row with "Mes Dossiers" and "Mes Téléchargements".
 * Zone 1 of the new home page layout.
 */
@Composable
fun DashboardButtonsRow(
    onDossiersClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    downloadProgress: Float? = null, // null = no download, 0.0-1.0 = progress
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardButton(
            title = "Mes Dossiers",
            icon = Icons.Filled.Folder,
            onClick = onDossiersClick,
            modifier = Modifier.weight(1f)
        )
        
        DashboardButton(
            title = "Mes Téléchargements",
            icon = Icons.Filled.CloudDownload,
            onClick = onDownloadsClick,
            downloadProgress = downloadProgress,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual dashboard button with icon and optional download progress indicator.
 */
@Composable
private fun DashboardButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadProgress: Float? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon with optional progress indicator
            Box(contentAlignment = Alignment.Center) {
                // Background circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MibekoSecondaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MibekoSecondaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // Mini progress indicator overlay
                if (downloadProgress != null && downloadProgress > 0f && downloadProgress < 1f) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 2.dp,
                        color = MibekoPrimaryBlue,
                        trackColor = Color.Transparent
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 2
            )
        }
    }
}
