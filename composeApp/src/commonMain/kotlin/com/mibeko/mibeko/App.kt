package com.mibeko.mibeko

import androidx.compose.runtime.Composable
import com.mibeko.mibeko.ui.home.HomeScreen
import com.mibeko.mibeko.ui.search.SearchResultsScreen
import com.mibeko.mibeko.ui.reader.ReaderScreen
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
        when (val dest = navigator.currentDestination) {
            is NavDestination.Home -> HomeScreen(navigator)
            is NavDestination.SearchResults -> SearchResultsScreen(dest.query, navigator)
            is NavDestination.Reader -> ReaderScreen(dest.articleId, navigator)
            is NavDestination.Explorer -> ExplorerScreen(navigator)
            is NavDestination.Favorites -> FavoritesScreen(navigator)
            is NavDestination.Settings -> SettingsScreen(navigator)
        }
    }
}