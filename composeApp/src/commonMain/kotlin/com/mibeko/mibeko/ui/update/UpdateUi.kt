package com.mibeko.mibeko.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.util.UpdateState

/**
 * Écran bloquant : la version installée n'est plus supportée. Aucun autre
 * contenu n'est accessible ; le seul chemin est le store. Pas de mention
 * d'achat (conforme stores), pas de bouton fermer.
 */
@Composable
fun ForceUpdateScreen(state: UpdateState.ForceUpdate, onUpdateClick: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Mise à jour requise",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.message
                    ?: "Cette version de Mibeko n'est plus supportée. Mettez à jour l'application pour continuer à consulter le droit congolais à jour.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            val storeUrl = state.storeUrl
            if (storeUrl != null) {
                Button(
                    onClick = {
                        onUpdateClick()
                        uriHandler.openUri(storeUrl)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Mettre à jour")
                }
            }
        }
    }
}

/** Bannière discrète « mise à jour disponible », ignorable par version. */
@Composable
fun UpdateBanner(
    state: UpdateState.SoftUpdate,
    modifier: Modifier = Modifier,
    onUpdateClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text = "Nouvelle version disponible (${state.latestVersion})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            val storeUrl = state.storeUrl
            if (storeUrl != null) {
                androidx.compose.material3.TextButton(onClick = {
                    onUpdateClick()
                    uriHandler.openUri(storeUrl)
                }) {
                    Text("Mettre à jour")
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Ignorer", modifier = Modifier.size(16.dp))
            }
        }
    }
}
