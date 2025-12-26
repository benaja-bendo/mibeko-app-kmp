package com.mibeko.mibeko.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

sealed class NavDestination {
    object Home : NavDestination()
    data class SearchResults(val query: String) : NavDestination()
    data class Reader(val articleId: String) : NavDestination()
    object Explorer : NavDestination()
    object Favorites : NavDestination()
    object Settings : NavDestination()
}

class MibekoNavigator {
    var currentDestination by mutableStateOf<NavDestination>(NavDestination.Home)
        private set

    fun navigateTo(destination: NavDestination) {
        currentDestination = destination
    }
}

@Composable
fun rememberMibekoNavigator(): MibekoNavigator = remember { MibekoNavigator() }
