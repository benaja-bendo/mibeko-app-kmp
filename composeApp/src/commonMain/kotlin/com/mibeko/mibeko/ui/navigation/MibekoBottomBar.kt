package com.mibeko.mibeko.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.mibeko.mibeko.ui.home.HomeScreen
import com.mibeko.mibeko.ui.explorer.ExplorerScreen
import com.mibeko.mibeko.ui.favorites.FavoritesScreen
import com.mibeko.mibeko.ui.settings.SettingsScreen

@Composable
fun MibekoBottomBar(navigator: Navigator) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
            label = { Text("Accueil") },
            selected = navigator.lastItem is HomeScreen,
            onClick = { 
                if (navigator.lastItem !is HomeScreen) {
                    navigator.replaceAll(HomeScreen())
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
            label = { Text("Explorer") },
            selected = navigator.lastItem is ExplorerScreen,
            onClick = { 
                if (navigator.lastItem !is ExplorerScreen) {
                    navigator.replaceAll(ExplorerScreen())
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Star, contentDescription = "Favoris") },
            label = { Text("Favoris") },
            selected = navigator.lastItem is FavoritesScreen,
            onClick = { 
                if (navigator.lastItem !is FavoritesScreen) {
                    navigator.replaceAll(FavoritesScreen())
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Réglages") },
            label = { Text("Réglages") },
            selected = navigator.lastItem is SettingsScreen,
            onClick = { 
                if (navigator.lastItem !is SettingsScreen) {
                    navigator.replaceAll(SettingsScreen())
                }
            }
        )
    }
}
