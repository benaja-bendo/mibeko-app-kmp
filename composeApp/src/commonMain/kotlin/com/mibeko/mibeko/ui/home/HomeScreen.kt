package com.mibeko.mibeko.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibeko.mibeko.di.AppModule
import com.mibeko.mibeko.ui.navigation.MibekoNavigator
import com.mibeko.mibeko.ui.navigation.NavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigator: MibekoNavigator,
    viewModel: HomeViewModel = viewModel { HomeViewModel(AppModule.repository) }
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val lawCodes by viewModel.lawCodes.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    Scaffold(
        bottomBar = { MibekoBottomBar(navigator) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            // Header with Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mibeko",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = { viewModel.syncData() }, enabled = !isSyncing) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Sync Status
            if (lawCodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Aucune donnée disponible", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Veuillez synchroniser pour accéder aux textes hors-ligne.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = { viewModel.syncData() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Synchroniser maintenant")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Large Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher un article (ex: Art 45)...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotEmpty()) {
                        focusManager.clearFocus()
                        navigator.navigateTo(NavDestination.SearchResults(searchQuery))
                    }
                })
            )

            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Expert Section: Codes
                item {
                    Text(
                        text = "Codes Officiels",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (lawCodes.isNotEmpty()) {
                    // Use a Chunked flow or simple grid calculation since LazyColumn item can't easily contain a dynamic Grid without fixed height.
                    // Or keep it simple: Horizontal scrolling row OR just vertical list for now inside the column?
                    // PRD says "Hierarchical navigation".
                    // Let's use a dynamic grid logic by chunking the list into pairs, OR use a FlowRow if available (Compose Multiplatform usually has it).
                    // For safety in standard Compose, let's render items in pairs.
                    
                    val chunkedCodes = lawCodes.chunked(2)
                    items(chunkedCodes) { rowCodes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowCodes.forEach { code ->
                                HomeGridItem(
                                    title = code.title,
                                    id = code.id,
                                    icon = if (code.title.contains("Pénal", ignoreCase = true)) Icons.Default.Gavel else Icons.Default.Balance,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Navigate to Explorer filtering by this code effectively, 
                                    // or just go to Explorer root as requested (Navigator currently simple).
                                    // Ideally: navigate to code detail directly?
                                    // Current Navigator: NavDestination.Explorer (no args).
                                    navigator.navigateTo(NavDestination.Explorer)
                                }
                            }
                            // Fill empty space if odd number
                            if (rowCodes.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSyncing) {
                                Text("Chargement des codes...", fontSize = 14.sp, color = Color.Gray)
                            } else {
                                Text("Aucun code. Synchronisez pour commencer.", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Novice Section: Themes
                item {
                    Text(
                        text = "Thématiques Populaires",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThemeItem("Contrat de Travail & Licenciement", Icons.Default.Work) {
                            navigator.navigateTo(NavDestination.SearchResults("Travail"))
                        }
                        ThemeItem("Mariage, Divorce & Famille", Icons.Default.FamilyRestroom) {
                            navigator.navigateTo(NavDestination.SearchResults("Famille"))
                        }
                        ThemeItem("Location & Logement", Icons.Default.HomeWork) {
                            navigator.navigateTo(NavDestination.SearchResults("Location"))
                        }
                        ThemeItem("Créations d'entreprises", Icons.Default.BusinessCenter) {
                            navigator.navigateTo(NavDestination.SearchResults("Commerce"))
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun HomeGridItem(title: String, id: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isPrimary = id == "1" || id == "2"
    
    Card(
        onClick = onClick,
        modifier = modifier
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) MaterialTheme.colorScheme.primary else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                color = if (isPrimary) Color.White else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ThemeItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MibekoBottomBar(navigator: MibekoNavigator) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
            label = { Text("Accueil") },
            selected = navigator.currentDestination is NavDestination.Home,
            onClick = { navigator.navigateTo(NavDestination.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
            label = { Text("Explorer") },
            selected = navigator.currentDestination is NavDestination.Explorer,
            onClick = { navigator.navigateTo(NavDestination.Explorer) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Star, contentDescription = "Favoris") },
            label = { Text("Favoris") },
            selected = navigator.currentDestination is NavDestination.Favorites,
            onClick = { navigator.navigateTo(NavDestination.Favorites) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Réglages") },
            label = { Text("Réglages") },
            selected = navigator.currentDestination is NavDestination.Settings,
            onClick = { navigator.navigateTo(NavDestination.Settings) }
        )
    }
}
