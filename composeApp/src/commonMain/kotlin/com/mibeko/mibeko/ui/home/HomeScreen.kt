package com.mibeko.mibeko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.mibeko.mibeko.ui.components.DashboardButtonsRow
import com.mibeko.mibeko.ui.components.FundamentalTextCard
import com.mibeko.mibeko.ui.components.LifeThemeItem

// Brand colors matching the reference design
private val MibekoPrimaryBlue = Color(0xFF1A3A6B)
private val MibekoSecondaryBlue = Color(0xFF2E5A9C)
private val MibekoGold = Color(0xFFB8860B)

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<HomeViewModel>()
        
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
        var showContent by remember { mutableStateOf(false) }
        
        // Refresh network status on resume
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
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Blue Header with Logo
                item {
                    HomeHeader()
                }
                
                // Network Status Banner (FR3)
                item {
                    NetworkStatusBanner(
                        isNetworkAvailable = uiState.isNetworkAvailable,
                        isSyncing = uiState.isSyncing
                    )
                }
                
                // Search Bar
                item {
                    SearchBar(
                        onClick = { navController.navigate(MibekoScreen.ActiveSearch()) }
                    )
                }
                
                // Zone 1: Dashboard Buttons (Dossiers + Téléchargements)
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { 20 }
                    ) {
                        DashboardButtonsRow(
                            onDossiersClick = { navController.navigate(MibekoScreen.Dossiers) },
                            onDownloadsClick = { navController.navigate(MibekoScreen.Settings) },
                            downloadProgress = uiState.downloadInProgress?.progress,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                // Zone 2: Textes Fondamentaux (Horizontal Carousel)
                item {
                    AnimatedVisibility(
                        visible = showContent && uiState.fundamentalTexts.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { 30 }
                    ) {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Textes Fondamentaux",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.fundamentalTexts) { text ->
                                    FundamentalTextCard(
                                        text = text,
                                        onClick = { 
                                            navController.navigate(MibekoScreen.DocumentDetail(text.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Empty state placeholder when no fundamental texts loaded
                if (showContent && uiState.fundamentalTexts.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Textes Fondamentaux",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Demo placeholders
                                items(listOf(
                                    FundamentalText("1", "Constitution", "Constitution", false, "CONSTITUTION"),
                                    FundamentalText("2", "Code de la Famille", "Famille", false, "CODE"),
                                    FundamentalText("3", "Code Pénal", "Pénal", false, "CODE")
                                )) { text ->
                                    FundamentalTextCard(
                                        text = text,
                                        onClick = { }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Zone 3: Thématiques de Vie (Vertical List)
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { 40 }
                    ) {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            Text(
                                text = "Thématiques de Vie",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // Life themes list items
                if (showContent) {
                    items(uiState.lifeThemes) { theme ->
                        LifeThemeItem(
                            theme = theme,
                            onClick = {
                                navController.navigate(MibekoScreen.SearchResults(tag = theme.filterTag))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Blue gradient header with Mibeko logo and title
 */
@Composable
private fun HomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MibekoPrimaryBlue, MibekoSecondaryBlue)
                )
            )
            .statusBarsPadding()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
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
 * Search bar styled like the reference design
 */
@Composable
private fun SearchBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-20).dp)
            .padding(horizontal = 24.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rechercher dans les textes officiels...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
