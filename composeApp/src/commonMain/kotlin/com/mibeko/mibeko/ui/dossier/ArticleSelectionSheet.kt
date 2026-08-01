package com.mibeko.mibeko.ui.dossier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.data.remote.LibrarySearchItem
import com.mibeko.mibeko.ui.components.MibekoErrorState
import org.koin.compose.viewmodel.koinViewModel

/** Recherche un article et l'ajoute au dossier, sans quitter l'écran du dossier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleSelectionSheet(
    dossierId: String,
    onDismiss: () -> Unit
) {
    val viewModel = koinViewModel<ArticleSelectionViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dossierId) {
        viewModel.start(dossierId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Ajouter un article",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text("Code du travail, article 45…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.results.isEmpty() -> {
                    MibekoErrorState(offline = uiState.error!!.offline, onRetry = uiState.error!!.retry)
                }

                uiState.query.trim().length < 2 -> {
                    Text(
                        text = "Tapez au moins deux caractères pour rechercher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                uiState.results.isEmpty() -> {
                    Text(
                        text = "Aucun texte pour « ${uiState.query.trim()} ».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.results, key = { it.id }) { item ->
                            ArticleSelectionRow(
                                item = item,
                                alreadyAdded = item.id in uiState.addedArticleIds,
                                onAdd = { viewModel.addArticle(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleSelectionRow(
    item: LibrarySearchItem,
    alreadyAdded: Boolean,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.document_title ?: item.breadcrumb ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.number?.let { "Article $it" } ?: "Article",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onAdd, enabled = !alreadyAdded) {
                Icon(
                    imageVector = if (alreadyAdded) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (alreadyAdded) "Déjà ajouté" else "Ajouter",
                    tint = if (alreadyAdded) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}
