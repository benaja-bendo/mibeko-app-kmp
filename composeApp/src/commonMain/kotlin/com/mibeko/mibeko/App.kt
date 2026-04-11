package com.mibeko.mibeko

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.navigation.NavDeepLink
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
import com.mibeko.mibeko.ui.notifications.NotificationsScreen
import com.mibeko.mibeko.ui.auth.LoginScreen
import com.mibeko.mibeko.ui.auth.ProfileSetupScreen
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.theme.MibekoTheme
import org.koin.compose.koinInject

@Composable
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
                composable<Screen.Splash> { SplashScreen() }
                composable<Screen.Login> { LoginScreen() }
                composable<Screen.ProfileSetup> { ProfileSetupScreen() }
                composable<Screen.Disclaimer> { com.mibeko.mibeko.ui.onboarding.DisclaimerScreen() }
                composable<Screen.Onboarding> { com.mibeko.mibeko.ui.onboarding.OnboardingScreen() }
                composable<Screen.Home> { HomeScreen() }
                composable<Screen.Favorites> { FavoritesScreen() }
                composable<Screen.Settings> { SettingsScreen() }
                composable<Screen.Dossiers> { DossierScreen() }
                composable<Screen.Library> { LibraryScreen() }
                composable<Screen.Downloads> { DownloadsScreen() }
                composable<Screen.Notifications> { NotificationsScreen() }
                
                composable<Screen.SearchResults> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.SearchResults>()
                    SearchResultsScreen(query = route.query, tag = route.tag) 
                }
                
                composable<Screen.DocumentDetail>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://mibeko.cg/document/{documentId}" },
                        navDeepLink { uriPattern = "mibeko://document/{documentId}" }
                    )
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.DocumentDetail>()
                    DocumentDetailScreen(route.documentId)
                }
                
                composable<Screen.Reader>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "https://mibeko.cg/article/{articleId}" },
                        navDeepLink { uriPattern = "mibeko://article/{articleId}" }
                    )
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.Reader>()
                    ReaderScreen(route.articleId)
                }
                
                composable<Screen.DossierDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.DossierDetail>()
                    DossierDetailScreen(route.dossierId)
                }
                
                composable<Screen.DocumentList> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.DocumentList>()
                    com.mibeko.mibeko.ui.library.DocumentListScreen(route.typeCode, route.typeName)
                }
                
                composable<Screen.OfficialJournalList> {
                    com.mibeko.mibeko.ui.officialjournal.OfficialJournalListScreen()
                }
                
                composable<Screen.OfficialJournalDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.OfficialJournalDetail>()
                    com.mibeko.mibeko.ui.officialjournal.OfficialJournalDetailScreen(route.id)
                }
            }
        }
    }
    }
}
