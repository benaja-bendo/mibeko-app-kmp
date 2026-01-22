package com.mibeko.mibeko

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import com.mibeko.mibeko.ui.home.HomeScreen
import com.mibeko.mibeko.ui.favorites.FavoritesScreen
import com.mibeko.mibeko.ui.settings.SettingsScreen
import com.mibeko.mibeko.ui.search.SearchResultsScreen
import com.mibeko.mibeko.ui.details.DocumentDetailScreen
import com.mibeko.mibeko.ui.reader.ReaderScreen
import com.mibeko.mibeko.ui.splash.SplashScreen
import com.mibeko.mibeko.ui.dossier.DossierScreen
import com.mibeko.mibeko.ui.dossier.DossierDetailScreen
import com.mibeko.mibeko.ui.library.LibraryScreen
import com.mibeko.mibeko.ui.downloads.DownloadsScreen
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.theme.MibekoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val userPreferencesRepository = koinInject<UserPreferencesRepository>()
    
    // Collect the theme from the repository reactively
    val theme by userPreferencesRepository.theme.collectAsState()
    
    val isDarkTheme = when (theme) {
        UserPreferencesRepository.AppTheme.LIGHT -> false
        UserPreferencesRepository.AppTheme.DARK -> true
        UserPreferencesRepository.AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MibekoTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()
        
        CompositionLocalProvider(com.mibeko.mibeko.ui.navigation.LocalNavController provides navController) {
            // Screens that should show the bottom bar
        val bottomBarScreens = listOf(
            Screen.Home::class.qualifiedName,
            Screen.Library::class.qualifiedName,
            Screen.Favorites::class.qualifiedName,
            Screen.Dossiers::class.qualifiedName,
            Screen.Settings::class.qualifiedName,
            Screen.SearchResults::class.qualifiedName
        )
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = bottomBarScreens.any { screen -> currentRoute?.contains(screen ?: "") == true }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    MibekoBottomBar(navController)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable<Screen.Splash> { SplashScreen().Content() }
                composable<Screen.Disclaimer> { com.mibeko.mibeko.ui.onboarding.DisclaimerScreen().Content() }
                composable<Screen.Onboarding> { com.mibeko.mibeko.ui.onboarding.OnboardingScreen().Content() }
                composable<Screen.Home> { HomeScreen().Content() }
                composable<Screen.Favorites> { FavoritesScreen().Content() }
                composable<Screen.Settings> { SettingsScreen().Content() }
                composable<Screen.Dossiers> { DossierScreen().Content() }
                composable<Screen.Library> { LibraryScreen().Content() }
                composable<Screen.Downloads> { DownloadsScreen().Content() }
                
                composable<Screen.SearchResults> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.SearchResults>()
                    SearchResultsScreen(query = route.query, tag = route.tag).Content() 
                }
                
                composable<Screen.DocumentDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.DocumentDetail>()
                    DocumentDetailScreen(route.documentId).Content()
                }
                
                composable<Screen.Reader> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.Reader>()
                    ReaderScreen(route.articleId).Content()
                }
                
                composable<Screen.DossierDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.DossierDetail>()
                    DossierDetailScreen(route.dossierId).Content()
                }
                
                composable<Screen.DocumentList> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.DocumentList>()
                    com.mibeko.mibeko.ui.library.DocumentListScreen(route.typeCode, route.typeName).Content()
                }
            }
        }
    }
    }
}