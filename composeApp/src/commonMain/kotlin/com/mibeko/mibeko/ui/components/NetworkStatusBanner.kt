package com.mibeko.mibeko.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Network status banner displayed at the top of the screen.
 * Shows offline mode indicator as per FR3 of PRD.
 */
@Composable
fun NetworkStatusBanner(
    isNetworkAvailable: Boolean,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Show banner when offline or syncing
    AnimatedVisibility(
        visible = !isNetworkAvailable || isSyncing,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier
    ) {
        val backgroundColor = if (!isNetworkAvailable) {
            Color(0xFFFFF3E0) // Light orange for offline
        } else {
            Color(0xFFE3F2FD) // Light blue for syncing
        }
        
        val contentColor = if (!isNetworkAvailable) {
            Color(0xFFE65100) // Deep orange text
        } else {
            Color(0xFF1565C0) // Blue text
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (!isNetworkAvailable) Icons.Filled.CloudOff else Icons.Filled.Sync,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (!isNetworkAvailable) {
                    "Mode hors-ligne. Seuls vos textes téléchargés sont accessibles."
                } else {
                    "Synchronisation en cours…"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
