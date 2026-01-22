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
                contentPadding = PaddingValues(bottom = 32.dp)
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

                // 2. Hiérarchie des Normes
                item {
                    Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hiérarchie des Normes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            Text(
                                text = "Importance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                fallbackTypes.forEach { pair ->
                                    HierarchyCard(
                                        icon = pair.second,
                                        title = pair.first,
                                        count = uiState.stats.find { it.type_name.contains(pair.first.split(" ").last(), ignoreCase = true) }?.count ?: 0,
                                        onClick = { 
                                            navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.DocumentList(typeCode = pair.first, typeName = pair.first))
                                        }
                                    )
                                }
                            } else {
                                uiState.documentTypes.forEach { type ->
                                    HierarchyCard(
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
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background decoration
            Icon(
                Icons.Default.Balance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color(0xFFB8860B).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "LOI SUPRÊME",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB8860B),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Consulter",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Carte modernisée pour la hiérarchie des normes.
 */
@Composable
private fun HierarchyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count documents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
