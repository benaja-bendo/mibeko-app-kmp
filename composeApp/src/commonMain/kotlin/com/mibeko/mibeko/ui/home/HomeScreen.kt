package com.mibeko.mibeko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.delay
import mibeko.composeapp.generated.resources.Res
import mibeko.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as MibekoScreen
import com.mibeko.mibeko.ui.components.NetworkStatusBanner
import com.mibeko.mibeko.ui.theme.MibekoGold
import com.mibeko.mibeko.ui.theme.MibekoBluePrimary
import com.mibeko.mibeko.ui.theme.MibekoBlueDark

class HomeScreen : Screen {

    /**
     * Contenu principal de la page d'accueil.
     */
    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<HomeViewModel>()
        
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
        var showContent by remember { mutableStateOf(false) }
        
        // Refresh data on resume
        LaunchedEffect(Unit) {
            viewModel.refreshNetworkStatus()
            delay(100)
            showContent = true
        }

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Blue Header with Logo
                item {
                    HomeHeader(
                        onNotificationsClick = {
                            navController.navigate(MibekoScreen.Notifications)
                        }
                    )
                }
                
                // Search Trigger Button (navigates to SearchResults)
                item {
                    SearchTriggerButton(
                        onClick = { 
                            navController.navigate(MibekoScreen.SearchResults())
                        }
                    )
                }

                // Network Status Banner (FR3)
                item {
                    NetworkStatusBanner(
                        isNetworkAvailable = uiState.isNetworkAvailable,
                        isSyncing = uiState.isSyncing
                    )
                }
                
                // Zone 1: Popular Codes (Horizontal List)
                item {
                    AnimatedVisibility(
                        visible = !uiState.isLoading && uiState.popularCodes.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { 30 }
                    ) {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Codes Populaires",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.popularCodes) { doc ->
                                    PopularCodeCard(
                                        title = doc.title,
                                        onClick = { 
                                            if (doc.id.isNotBlank()) {
                                                navController.navigate(MibekoScreen.DocumentDetail(doc.id))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Zone 2: Recently Added (Vertical List)
                item {
                    AnimatedVisibility(
                        visible = !uiState.isLoading && uiState.recentlyAdded.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { 40 }
                    ) {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Documents",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (!uiState.isLoading) {
                    items(uiState.recentlyAdded) { doc ->
                        RecentDocumentItem(
                            title = doc.title,
                            date = "Récemment", 
                            onClick = {
                                if (doc.id.isNotBlank()) {
                                    navController.navigate(MibekoScreen.DocumentDetail(doc.id))
                                } else {
                                    viewModel.refreshNetworkStatus() // Just to trigger something or we could show a snackbar
                                }
                            }
                        )
                    }
                }

                // Empty state or Loading
                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * En-tête avec dégradé bleu institutionnel, logo Mibeko et titre.
 */
@Composable
private fun HomeHeader(onNotificationsClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MibekoBluePrimary, MibekoBlueDark)
                )
            )
            .statusBarsPadding()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Notification Icon (Top Right)
        IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Icon(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Mibeko Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Mibeko",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Mobile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MibekoGold,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Bouton déguisé en barre de recherche.
 * Naviguer vers SearchResultsScreen au clic.
 */
@Composable
private fun SearchTriggerButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-20).dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Rechercher un article, une loi...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Surface(
                color = MibekoBluePrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "IA",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Carte pour afficher un code populaire.
 * Design Premium avec dégradé et icône institutionnelle.
 */
@Composable
private fun PopularCodeCard(title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 160.dp, height = 110.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle background decoration
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )
            
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Consulter",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Élément de liste pour un document récent.
 */
@Composable
private fun RecentDocumentItem(title: String, date: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MibekoBluePrimary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MibekoBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Blue gradient header with Mibeko logo and title
 */