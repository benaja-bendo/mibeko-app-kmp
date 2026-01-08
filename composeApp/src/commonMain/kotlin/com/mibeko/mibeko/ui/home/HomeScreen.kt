package com.mibeko.mibeko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// Brand colors matching the reference design
private val MibekoPrimaryBlue = Color(0xFF1A3A6B)
private val MibekoSecondaryBlue = Color(0xFF2E5A9C)
private val MibekoGold = Color(0xFFB8860B)

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<HomeViewModel>()
        
        val lawCodes by viewModel.lawCodes.collectAsState()
        val recentItems by viewModel.recentItems.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()
        val error by viewModel.error.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color(0xFFF5F5F5) // Light gray background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Room for bottom nav
            ) {
                // Blue Header with Logo
                item {
                    HomeHeader()
                }
                
                // Search Bar
                item {
                    SearchBar(
                        onClick = { navController.navigate(MibekoScreen.ActiveSearch) }
                    )
                }
                
                // Quick Access Grid (2x2)
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + slideInVertically { 20 }
                    ) {
                        QuickAccessGrid(
                            onCodesClick = { navController.navigate(MibekoScreen.Explorer) },
                            onLoisClick = { navController.navigate(MibekoScreen.Explorer) },
                            onDownloadsClick = { navController.navigate(MibekoScreen.Settings) },
                            onFavorisClick = { navController.navigate(MibekoScreen.Dossiers) }
                        )
                    }
                }
                
                // Consultés Récemment Section
                item {
                    AnimatedVisibility(
                        visible = showContent && recentItems.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { 40 }
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Text(
                                text = "Consultés Récemment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                
                // Recent items list
                if (showContent && recentItems.isNotEmpty()) {
                    items(recentItems.take(5)) { item ->
                        RecentItemRow(
                            title = item.title,
                            onClick = { navController.navigate(MibekoScreen.Reader(item.id)) }
                        )
                    }
                }
                
                // Example recent items if empty (for demo)
                if (showContent && recentItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Text(
                                text = "Consultés Récemment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            RecentItemRow(title = "Code Pénal - Art. 12", onClick = { })
                            RecentItemRow(title = "Loi n° 2023-15", onClick = { })
                        }
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

/**
 * 2x2 Grid of quick access cards
 */
@Composable
private fun QuickAccessGrid(
    onCodesClick: () -> Unit,
    onLoisClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onFavorisClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessTile(
                title = "Codes en vigueur",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = MibekoSecondaryBlue,
                modifier = Modifier.weight(1f),
                onClick = onCodesClick
            )
            QuickAccessTile(
                title = "Lois & Décrets\nRécents",
                icon = Icons.Filled.Gavel,
                iconTint = MibekoGold,
                modifier = Modifier.weight(1f),
                onClick = onLoisClick
            )
        }
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessTile(
                title = "Mes\nTéléchargements",
                icon = Icons.Filled.CloudDownload,
                iconTint = MibekoSecondaryBlue,
                modifier = Modifier.weight(1f),
                onClick = onDownloadsClick
            )
            QuickAccessTile(
                title = "Favoris",
                icon = Icons.Filled.Star,
                iconTint = MibekoGold,
                modifier = Modifier.weight(1f),
                onClick = onFavorisClick
            )
        }
    }
}

/**
 * Individual quick access tile card
 */
@Composable
private fun QuickAccessTile(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Recent item row with chevron
 */
@Composable
private fun RecentItemRow(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ouvrir",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
