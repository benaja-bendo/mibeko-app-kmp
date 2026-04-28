package com.mibeko.mibeko.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.ui.dossier.CreateDossierDialog
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DossierSelectionSheet(
    articleId: String,
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<DossierSelectionViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(articleId) {
        viewModel.loadDossiers(articleId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Ajouter à un dossier",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.dossiers.isEmpty()) {
                Text(
                    text = "Aucun dossier disponible.",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(uiState.dossiers) { item ->
                        DossierSelectionItem(
                            dossierWithSelection = item,
                            onToggle = { isSelected ->
                                viewModel.toggleDossierSelection(item.dossier.id, isSelected)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Créer un dossier",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Créer un nouveau dossier",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateDossierDialog(
            dossier = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, domain, tag, desc, color ->
                viewModel.createAndAddToDossier(name, domain, tag, desc, color)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun DossierSelectionItem(
    dossierWithSelection: DossierWithSelection,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!dossierWithSelection.isSelected) }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = parseColor(dossierWithSelection.dossier.color),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = dossierWithSelection.dossier.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Checkbox(
            checked = dossierWithSelection.isSelected,
            onCheckedChange = { onToggle(it) }
        )
    }
}

// Helper to parse hex color
private fun parseColor(hexString: String): Color {
    return try {
        val cleanHex = if (hexString.startsWith("#")) hexString.substring(1) else hexString
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color((colorInt or 0xFF000000).toULong())
        } else {
            Color(colorInt.toULong())
        }
    } catch (e: Exception) {
        Color.Gray
    }
}
