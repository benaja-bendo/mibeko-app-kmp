package com.mibeko.mibeko.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
    val route: String?
)

/**
 * Barre de navigation principale : Accueil, Bibliothèque, Dossiers, Profil.
 * L'assistant n'a pas d'onglet dédié — il vit sur l'accueil (champ hero) et
 * dans les actions contextuelles, pour réduire la charge cognitive.
 */
@Composable
fun MibekoBottomBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val items = listOf(
        BottomBarItem("Accueil", Icons.Filled.Home, Screen.Home, Screen.Home::class.qualifiedName),
        BottomBarItem("Bibliothèque", Icons.AutoMirrored.Filled.MenuBook, Screen.Library, Screen.Library::class.qualifiedName),
        BottomBarItem("Dossiers", Icons.Filled.Folder, Screen.Dossiers, Screen.Dossiers::class.qualifiedName),
        BottomBarItem("Profil", Icons.Filled.Person, Screen.Settings, Screen.Settings::class.qualifiedName)
    )

    Column {
        // Contour discret plutôt qu'une ombre : élévation « imprimée ».
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                    selected = item.route != null && currentRoute?.startsWith(item.route) == true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = {
                        navController.navigate(item.screen) {
                            popUpTo(Screen.Home) { inclusive = item.screen == Screen.Home }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
