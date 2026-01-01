package com.mibeko.mibeko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as MibekoScreen
import com.mibeko.mibeko.ui.components.MibekoSearchBar
import com.mibeko.mibeko.ui.components.QuickAccessCard
import com.mibeko.mibeko.ui.components.ThemeCard

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<HomeViewModel>()
        
        val lawCodes by viewModel.lawCodes.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()
        val error by viewModel.error.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
        // Animation states
        var showContent by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            delay(100)
            showContent = true
        }

        LaunchedEffect(error) {
            error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header with Logo and Status
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { -40 }
                    ) {
                        HomeHeader(
                            isSyncing = isSyncing,
                            isOfflineReady = lawCodes.isNotEmpty(),
                            onSyncClick = { viewModel.syncData() }
                        )
                    }
                }
                
                // Search Bar
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { -20 }
                    ) {
                        MibekoSearchBar(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            onClick = { navController.navigate(MibekoScreen.ActiveSearch) }
                        )
                    }
                }
                
                // No data warning
                if (lawCodes.isEmpty() && !isSyncing) {
                    item {
                        NoDataCard(onSyncClick = { viewModel.syncData() })
                    }
                }
                
                // Quick Access Section (for Experts)
                item {
                    AnimatedVisibility(
                        visible = showContent && lawCodes.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { 20 }
                    ) {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            SectionHeader(
                                title = "Accès Rapide",
                                subtitle = "Codes les plus consultés"
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(lawCodes.take(4)) { code ->
                                    QuickAccessCard(
                                        title = code.title,
                                        icon = getCodeIcon(code.title)
                                    ) {
                                        navController.navigate(MibekoScreen.Explorer)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Loading state for codes
                if (isSyncing && lawCodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Chargement des codes...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Life Themes Section (for Citizens)
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { 40 }
                    ) {
                        Column(modifier = Modifier.padding(top = 32.dp)) {
                            SectionHeader(
                                title = "Thématiques de Vie",
                                subtitle = "Trouvez par situation"
                            )
                            
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ThemeCard(
                                    title = "Contrat de Travail & Licenciement",
                                    subtitle = "Droits des travailleurs et employeurs",
                                    icon = Icons.Default.Work
                                ) {
                                    navController.navigate(MibekoScreen.SearchResults("Travail licenciement"))
                                }
                                
                                ThemeCard(
                                    title = "Mariage, Divorce & Famille",
                                    subtitle = "Vie privée et protection familiale",
                                    icon = Icons.Default.FamilyRestroom
                                ) {
                                    navController.navigate(MibekoScreen.SearchResults("Famille mariage"))
                                }
                                
                                ThemeCard(
                                    title = "Location & Logement",
                                    subtitle = "Baux d'habitation et droits locatifs",
                                    icon = Icons.Default.HomeWork
                                ) {
                                    navController.navigate(MibekoScreen.SearchResults("Location bail"))
                                }
                                
                                ThemeCard(
                                    title = "Création d'entreprises",
                                    subtitle = "Droit des affaires et entrepreneuriat",
                                    icon = Icons.Default.BusinessCenter
                                ) {
                                    navController.navigate(MibekoScreen.SearchResults("Commerce entreprise"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    isSyncing: Boolean,
    isOfflineReady: Boolean,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo and title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Gavel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Mibeko",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Status and sync
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Offline status indicator
            if (isOfflineReady) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Hors-ligne",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Sync button
            IconButton(
                onClick = onSyncClick,
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "Synchroniser",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NoDataCard(onSyncClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Aucune donnée disponible",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Synchronisez pour accéder aux textes juridiques même hors-ligne.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synchroniser maintenant")
            }
        }
    }
}

private fun getCodeIcon(title: String): ImageVector {
    return when {
        title.contains("Pénal", ignoreCase = true) -> Icons.Default.Gavel
        title.contains("Civil", ignoreCase = true) -> Icons.Default.Balance
        title.contains("Travail", ignoreCase = true) -> Icons.Default.Work
        title.contains("Famille", ignoreCase = true) -> Icons.Default.FamilyRestroom
        title.contains("Commercial", ignoreCase = true) -> Icons.Default.Business
        else -> Icons.Default.MenuBook
    }
}
