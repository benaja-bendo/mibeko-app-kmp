package com.mibeko.mibeko

import androidx.compose.runtime.Composable
import com.mibeko.mibeko.ui.home.HomeScreen
import com.mibeko.mibeko.ui.search.SearchResultsScreen
import com.mibeko.mibeko.ui.reader.ReaderScreen
import com.mibeko.mibeko.ui.details.DocumentDetailScreen
import com.mibeko.mibeko.ui.explorer.ExplorerScreen
import com.mibeko.mibeko.ui.favorites.FavoritesScreen
import com.mibeko.mibeko.ui.settings.SettingsScreen
import com.mibeko.mibeko.ui.navigation.NavDestination
import com.mibeko.mibeko.ui.navigation.rememberMibekoNavigator
import com.mibeko.mibeko.ui.theme.MibekoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val navigator = rememberMibekoNavigator()

    MibekoTheme {
        androidx.compose.animation.Crossfade(targetState = navigator.currentDestination) { destination ->
            when (destination) {
                is NavDestination.Home -> HomeScreen(navigator)
                is NavDestination.SearchResults -> SearchResultsScreen(destination.query, navigator)
                is NavDestination.Reader -> ReaderScreen(destination.articleId, navigator)
                is NavDestination.DocumentDetail -> DocumentDetailScreen(destination.documentId, navigator)
                is NavDestination.Explorer -> ExplorerScreen(navigator)
                is NavDestination.Favorites -> FavoritesScreen(navigator)
                is NavDestination.Settings -> SettingsScreen(navigator)
                is NavDestination.Notifications -> com.mibeko.mibeko.ui.notifications.NotificationScreen()
            }
        }
    }
}