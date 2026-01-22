package com.mibeko.mibeko.ui.dossier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as NavScreen
import com.mibeko.mibeko.getCurrentTimeMillis
import org.koin.compose.viewmodel.koinViewModel

class DossierScreen : Screen {

    /**
     * Contenu principal de l'écran des dossiers.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<DossierViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val showCreateDialog by viewModel.showCreateDialog.collectAsState()
        val editingDossier by viewModel.editingDossier.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mes Dossiers", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { /* TODO: Show search */ }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Rechercher"
                            )
                        }
                        IconButton(onClick = { /* TODO: Show filter */ }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Filtrer"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
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
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    uiState.dossiers.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Créez votre premier dossier pour organiser vos recherches",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.dossiers) { dossier ->
                                DossierCard(
                                    dossier = dossier,
                                    onClick = {
                                        navController.navigate(NavScreen.DossierDetail(dossier.id))
                                    },
                                    onEdit = { viewModel.showEditDialog(dossier) },
                                    onDelete = { viewModel.deleteDossier(dossier.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Create/Edit Dialog
        if (showCreateDialog) {
            CreateDossierDialog(
                dossier = editingDossier,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, domain, tag, desc, color ->
                    if (editingDossier != null) {
                        viewModel.updateDossier(editingDossier!!.id, name, domain, tag, desc, color)
                    } else {
                        viewModel.createDossier(name, domain, tag, desc, color)
                    }
                }
            )
        }
    }
}

@Composable
fun DossierCard(
    dossier: DossierEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon with color
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parseColor(dossier.color).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = parseColor(dossier.color),
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dossier.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${dossier.legal_domain} • ${formatDate(dossier.updated_at)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tag chip
                DossierTagChip(tag = dossier.tag)
            }
            
            // Menu
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

@Composable
fun DossierTagChip(tag: DossierTag) {
    val (backgroundColor, textColor, label) = when (tag) {
        DossierTag.EN_COURS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "En Cours"
        )
        DossierTag.URGENT -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Urgent"
        )
        DossierTag.ARCHIVE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Archivé"
        )
        DossierTag.FAVORIS -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Favoris"
        )
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
    var selectedColor by remember { mutableStateOf(dossier?.color ?: "#1565C0") }
    var expandedDomain by remember { mutableStateOf(false) }
    
    val legalDomains = listOf(
        "Droit de la Famille",
        "Droit des Affaires",
        "Droit du Travail",
        "Droit Pénal",
        "Droit Civil",
        "Droit Administratif",
        "Général"
    )
    
    val isEditing = dossier != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Modifier le Dossier" else "Nouveau Dossier",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du dossier") },
                    placeholder = { Text("Ex: Affaire M. Tchicaya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Legal domain dropdown
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
                
                // Tags
                Text(
                    text = "Étiquette",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DossierTag.entries.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { 
                                Text(
                                    when (tag) {
                                        DossierTag.EN_COURS -> "En Cours"
                                        DossierTag.URGENT -> "Urgent"
                                        DossierTag.ARCHIVE -> "Archivé"
                                        DossierTag.FAVORIS -> "Favoris"
                                    }
                                )
                            }
                        )
                    }
                }
                
                // Description
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
                enabled = name.isNotBlank()
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
        Color(0xFF1565C0)
    }
}

fun formatDate(timestamp: Long): String {
    val now = getCurrentTimeMillis()
    val diff = now - timestamp
    
    if (diff < 60000L) return "À l'instant"
    if (diff < 3600000L) return "Il y a ${diff / 60000L} min"
    if (diff < 86400000L) return "Il y a ${diff / 3600000L}h"
    if (diff < 604800000L) return "Il y a ${diff / 86400000L} jours"
    
    return "Modifié hier"
}
