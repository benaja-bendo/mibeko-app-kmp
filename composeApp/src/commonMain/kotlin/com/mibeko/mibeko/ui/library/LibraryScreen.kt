package com.mibeko.mibeko.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
                    title = { 
                        Text(
                            "Bibliothèque juridique", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp
                )
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
            .height(220.dp)
            .padding(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Premium background decoration
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 60.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "LOI SUPRÊME",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Consulter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(18.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count documents disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
