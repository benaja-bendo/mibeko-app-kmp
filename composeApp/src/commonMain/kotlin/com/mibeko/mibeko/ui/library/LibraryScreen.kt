package com.mibeko.mibeko.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.ui.navigation.LocalNavController
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Search

class LibraryScreen : Screen {

    /**
     * Contenu principal de la bibliothèque juridique.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<LibraryViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Bibliothèque Juridique", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { /* Open Search */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. Hero Section (Constitution)
                item {
                    HeroCard(
                        title = "CONSTITUTION",
                        subtitle = "Loi Fondamentale de la République du Congo",
                        onClick = { 
                            navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.DocumentList(typeCode = "CONST", typeName = "Constitution"))
                        }
                    )
                }

                // 2. Hiérarchie des Normes (Vertical List)
                item {
                    Column(modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)) {
                        Text(
                            text = "Hiérarchie des Normes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column {
                                if (uiState.documentTypes.isEmpty()) {
                                    // Fallback if API fails
                                    val fallbackTypes = listOf(
                                        "Constitution" to Icons.Default.Balance,
                                        "Traités Internationaux" to Icons.Default.Public,
                                        "Lois Organiques" to Icons.Default.AccountBalance,
                                        "Lois Ordinaires" to Icons.Default.Description,
                                        "Ordonnances" to Icons.Default.Gavel,
                                        "Décrets" to Icons.Default.Article,
                                        "Arrêtés" to Icons.Default.Assignment
                                    )
                                    fallbackTypes.forEachIndexed { index, pair ->
                                        HierarchyItem(
                                            icon = pair.second,
                                            title = pair.first,
                                            count = uiState.stats.find { it.type_name.contains(pair.first.split(" ").last(), ignoreCase = true) }?.count ?: 0,
                                            onClick = { 
                                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.DocumentList(typeCode = pair.first, typeName = pair.first))
                                            }
                                        )
                                        if (index < fallbackTypes.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        }
                                    }
                                } else {
                                    uiState.documentTypes.forEachIndexed { index, type ->
                                        HierarchyItem(
                                            icon = when {
                                                type.name.contains("Constitution", true) -> Icons.Default.Balance
                                                type.name.contains("Traité", true) -> Icons.Default.Public
                                                type.name.contains("Organique", true) -> Icons.Default.AccountBalance
                                                type.name.contains("Ordinaire", true) -> Icons.Default.Description
                                                type.name.contains("Ordonnance", true) -> Icons.Default.Gavel
                                                type.name.contains("Décret", true) -> Icons.Default.Article
                                                else -> Icons.Default.Assignment
                                            },
                                            title = type.name,
                                            count = uiState.stats.find { it.type_code == type.code }?.count ?: 0,
                                            onClick = { 
                                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.DocumentList(typeCode = type.code, typeName = type.name))
                                            }
                                        )
                                        if (index < uiState.documentTypes.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Carte de héros pour mettre en avant un document (ex: Constitution).
 */
@Composable
private fun HeroCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Balance,
                    contentDescription = null,
                    tint = Color(0xFFB8860B), // Gold
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Carte pour afficher un code avec option de téléchargement.
 */
@Composable
private fun CodeCard(
    code: LawCodeSpec,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (code.isDownloaded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hors-ligne", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                    }
                } else {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onDownload() }
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Télécharger", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Élément de liste pour la hiérarchie des normes.
 */
@Composable
private fun HierarchyItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
