package com.mibeko.mibeko.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MibekoBottomBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
            label = { Text("Accueil") },
            selected = currentRoute == Screen.Home::class.qualifiedName,
            onClick = { 
                navController.navigate(Screen.Home) {
                    popUpTo(Screen.Home) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Explorer") },
            label = { Text("Explorer") },
            selected = currentRoute == Screen.Explorer::class.qualifiedName,
            onClick = { 
                navController.navigate(Screen.Explorer) {
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Dossiers") },
            label = { Text("Dossiers") },
            selected = currentRoute == Screen.Dossiers::class.qualifiedName,
            onClick = { 
                navController.navigate(Screen.Dossiers) {
                    launchSingleTop = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Réglages") },
            label = { Text("Réglages") },
            selected = currentRoute == Screen.Settings::class.qualifiedName,
            onClick = { 
                navController.navigate(Screen.Settings) {
                    launchSingleTop = true
                }
            }
        )
    }
}
