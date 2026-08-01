package com.mibeko.mibeko.ui.dossier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.data.local.dao.DossierWithCount
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.repository.DossierSyncState
import com.mibeko.mibeko.getCurrentTimeMillis
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as NavScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DossierScreen() {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<DossierViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val editingDossier by viewModel.editingDossier.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    // Suppression irrécupérable (propagée au serveur à la sync) : jamais en un tap.
    var pendingDelete by remember { mutableStateOf<DossierWithCount?>(null) }

    Scaffold(
        topBar = {
            if (uiState.searchQuery.isNotEmpty() || showSearch) {
                Surface(
                    modifier = Modifier.statusBarsPadding(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.searchDossiers(it) },
                        placeholder = { Text("Rechercher un dossier…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.searchDossiers("")
                                showSearch = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Fermer")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                TopAppBar(
                    title = { Text("Mes Dossiers", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Filled.Sync, contentDescription = "Synchroniser")
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Rechercher")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Mode d'affichage"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Créer un dossier",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            DossierSyncStatusBar(syncState = syncState, onRetry = { viewModel.refresh() })

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "Erreur",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    uiState.dossiers.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucun dossier",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Créez votre premier dossier pour organiser vos recherches",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        if (uiState.isGridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.dossiers, key = { it.dossier.id }) { item ->
                                    DossierGridCard(
                                        item = item,
                                        onClick = {
                                            navController.navigate(NavScreen.DossierDetail(item.dossier.id))
                                        },
                                        onEdit = { viewModel.showEditDialog(item.dossier) },
                                        onDelete = { pendingDelete = item }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.dossiers, key = { it.dossier.id }) { item ->
                                    DossierCard(
                                        item = item,
                                        onClick = {
                                            navController.navigate(NavScreen.DossierDetail(item.dossier.id))
                                        },
                                        onEdit = { viewModel.showEditDialog(item.dossier) },
                                        onDelete = { pendingDelete = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateDossierDialog(
            dossier = editingDossier,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { name, domain, tag, desc, color ->
                val editing = editingDossier
                if (editing != null) {
                    viewModel.updateDossier(editing.id, name, domain, tag, desc, color)
                } else {
                    viewModel.createDossier(name, domain, tag, desc, color)
                }
            }
        )
    }

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer « ${toDelete.dossier.name} » ?") },
            text = {
                Text(
                    "Les ${toDelete.articleCount} article${if (toDelete.articleCount > 1) "s" else ""} de ce " +
                        "dossier seront retirés, y compris sur vos autres appareils. Cette action est irréversible."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDossier(toDelete.dossier.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * Bandeau discret indiquant l'état de synchronisation avec le compte Mibeko.
 */
@Composable
private fun DossierSyncStatusBar(syncState: DossierSyncState, onRetry: () -> Unit) {
    val (text, color) = when (syncState) {
        is DossierSyncState.Idle -> return
        is DossierSyncState.Syncing -> "Synchronisation…" to MaterialTheme.colorScheme.onSurfaceVariant
        is DossierSyncState.Synced ->
            "Synchronisé à ${formatTime(syncState.at)}" to MaterialTheme.colorScheme.onSurfaceVariant
        is DossierSyncState.Offline -> {
            val last = syncState.lastSyncAt
            val suffix = if (last != null) " — dernière sync ${formatTime(last)}" else ""
            "Hors-ligne, modifications enregistrées localement$suffix" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        is DossierSyncState.Error -> "Échec de synchronisation" to MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (syncState) {
            is DossierSyncState.Syncing -> CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp
            )
            is DossierSyncState.Synced -> Icon(
                Icons.Filled.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            is DossierSyncState.Offline -> Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            is DossierSyncState.Error -> Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            else -> Unit
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (syncState is DossierSyncState.Error) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Réessayer", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun DossierCard(
    item: DossierWithCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dossier = item.dossier
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseColor(dossier.color).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = parseColor(dossier.color),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dossier.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${dossier.legal_domain} • ${item.articleCount} article${if (item.articleCount > 1) "s" else ""} • ${formatDate(dossier.updated_at)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                DossierTagChip(tag = dossier.tag)
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    )
                    if (dossier.tag != DossierTag.FAVORIS) {
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DossierTagChip(tag: DossierTag) {
    val (backgroundColor, textColor, label) = when (tag) {
        DossierTag.EN_COURS -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.primary,
            "En cours"
        )
        DossierTag.URGENT -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.error,
            "Urgent"
        )
        DossierTag.ARCHIVE -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Archivé"
        )
        DossierTag.FAVORIS -> Triple(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.secondary,
            "Favoris"
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDossierDialog(
    dossier: DossierEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, DossierTag, String?, String) -> Unit
) {
    var name by remember { mutableStateOf(dossier?.name ?: "") }
    var legalDomain by remember { mutableStateOf(dossier?.legal_domain ?: "") }
    var selectedTag by remember { mutableStateOf(dossier?.tag ?: DossierTag.EN_COURS) }
    var description by remember { mutableStateOf(dossier?.description ?: "") }
    var selectedColor by remember { mutableStateOf(dossier?.color ?: "#1B3D2F") }
    var expandedDomain by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val legalDomains = listOf(
        "Droit de la Famille",
        "Droit des Affaires",
        "Droit du Travail",
        "Droit Pénal",
        "Droit Civil",
        "Droit Administratif",
        "Général"
    )

    // Palette institutionnelle (vert forêt, terracotta, tons neutres).
    val colorOptions = listOf(
        "#1B3D2F", "#8F4C31", "#436556", "#73351D", "#142519", "#727974"
    )

    val isEditing = dossier != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(8.dp),
        title = {
            Text(
                text = if (isEditing) "Modifier le dossier" else "Nouveau dossier",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du dossier") },
                    placeholder = { Text("Ex: Affaire M. Tchicaya") },
                    singleLine = true,
                    enabled = dossier?.tag != DossierTag.FAVORIS,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDomain,
                    onExpandedChange = { expandedDomain = it }
                ) {
                    OutlinedTextField(
                        value = legalDomain,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Domaine juridique") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDomain) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDomain,
                        onDismissRequest = { expandedDomain = false }
                    ) {
                        legalDomains.forEach { domain ->
                            DropdownMenuItem(
                                text = { Text(domain) },
                                onClick = {
                                    legalDomain = domain
                                    expandedDomain = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Étiquette",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierTag.entries.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            shape = RoundedCornerShape(4.dp),
                            label = {
                                Text(
                                    when (tag) {
                                        DossierTag.EN_COURS -> "En cours"
                                        DossierTag.URGENT -> "Urgent"
                                        DossierTag.ARCHIVE -> "Archivé"
                                        DossierTag.FAVORIS -> "Favoris"
                                    }
                                )
                            }
                        )
                    }
                }

                Text(
                    text = "Couleur",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(parseColor(hex))
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor.equals(hex, ignoreCase = true)) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (facultatif)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        legalDomain.ifEmpty { "Général" },
                        selectedTag,
                        description.ifEmpty { null },
                        selectedColor
                    )
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(if (isEditing) "Modifier" else "Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// Helper functions
fun parseColor(hexColor: String): Color {
    return try {
        val hex = hexColor.removePrefix("#")
        val colorLong = hex.toLong(16)
        Color(
            red = ((colorLong shr 16) and 0xFF) / 255f,
            green = ((colorLong shr 8) and 0xFF) / 255f,
            blue = (colorLong and 0xFF) / 255f
        )
    } catch (e: Exception) {
        Color(0xFF1B3D2F)
    }
}

fun formatDate(timestamp: Long): String {
    val now = getCurrentTimeMillis()
    val diff = now - timestamp

    if (diff < 60000L) return "À l'instant"
    if (diff < 3600000L) return "Il y a ${diff / 60000L} min"
    if (diff < 86400000L) return "Il y a ${diff / 3600000L}h"
    if (diff < 604800000L) return "Il y a ${diff / 86400000L} jours"

    return com.mibeko.mibeko.util.formatEpochMillisDate(timestamp)
}

private fun formatTime(timestamp: Long): String {
    return com.mibeko.mibeko.util.formatEpochMillisDate(timestamp, includeTime = true)
        .substringAfter("à ")
}

@Composable
fun DossierGridCard(
    item: DossierWithCount,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dossier = item.dossier
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = parseColor(dossier.color),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dossier.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${item.articleCount} article${if (item.articleCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                    )
                    if (dossier.tag != DossierTag.FAVORIS) {
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                DossierTagChip(tag = dossier.tag)
            }
        }
    }
}
