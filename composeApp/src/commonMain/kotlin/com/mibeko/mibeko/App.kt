package com.mibeko.mibeko

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavUri
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.mibeko.mibeko.data.auth.SessionEvents
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import com.mibeko.mibeko.ui.auth.ForgotPasswordScreen
import com.mibeko.mibeko.ui.auth.LoginScreen
import com.mibeko.mibeko.ui.auth.ProfileSetupScreen
import com.mibeko.mibeko.ui.auth.RegisterScreen
import com.mibeko.mibeko.ui.chat.ChatScreen
import com.mibeko.mibeko.ui.chat.ConversationHistoryScreen
import com.mibeko.mibeko.ui.details.DocumentDetailScreen
import com.mibeko.mibeko.ui.dossier.DossierDetailScreen
import com.mibeko.mibeko.ui.dossier.DossierScreen
import com.mibeko.mibeko.ui.downloads.DownloadsScreen
import com.mibeko.mibeko.ui.favorites.FavoritesScreen
import com.mibeko.mibeko.ui.home.HomeScreen
import com.mibeko.mibeko.ui.library.DocumentListScreen
import com.mibeko.mibeko.ui.library.LibraryScreen
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.notifications.NotificationsScreen
import com.mibeko.mibeko.ui.officialjournal.OfficialJournalDetailScreen
import com.mibeko.mibeko.ui.officialjournal.OfficialJournalListScreen
import com.mibeko.mibeko.ui.onboarding.DisclaimerScreen
import com.mibeko.mibeko.ui.onboarding.OnboardingScreen
import com.mibeko.mibeko.ui.reader.ReaderScreen
import com.mibeko.mibeko.ui.resolver.TexteResolverScreen
import com.mibeko.mibeko.ui.search.SearchResultsScreen
import com.mibeko.mibeko.ui.settings.SettingsScreen
import com.mibeko.mibeko.ui.splash.SplashScreen
import com.mibeko.mibeko.ui.theme.MibekoTheme
import com.mibeko.mibeko.util.ExternalUriHandler
import com.mibeko.mibeko.util.addFocusCleaner
import org.koin.compose.koinInject

@Composable
fun App() {
    val userPreferencesRepository = koinInject<UserPreferencesRepository>()
    val focusManager = LocalFocusManager.current

    val theme by userPreferencesRepository.theme.collectAsState()

    val isDarkTheme = when (theme) {
        UserPreferencesRepository.AppTheme.LIGHT -> false
        UserPreferencesRepository.AppTheme.DARK -> true
        UserPreferencesRepository.AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MibekoTheme(darkTheme = isDarkTheme) {
        val navController = rememberNavController()

        // Deep links transmis par la couche native (iOS : onOpenURL).
        DisposableEffect(navController) {
            ExternalUriHandler.listener = { uri ->
                runCatching { navController.navigate(NavUri(uri)) }
            }
            onDispose { ExternalUriHandler.listener = null }
        }

        // Session expirée (401 sur route authentifiée, jeton déjà purgé par le
        // client HTTP) : on invalide le cache bearer et on ramène l'utilisateur
        // à l'écran de connexion.
        val sessionEvents = koinInject<SessionEvents>()
        val authApiService = koinInject<AuthApiService>()
        LaunchedEffect(navController) {
            sessionEvents.sessionExpired.collect {
                authApiService.invalidateTokenCache()
                navController.navigate(Screen.Login) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        // Démarrage connecté : envoie le token push resté en attente
        // (renouvelé hors session ou échec réseau précédent).
        val pushTokenRegistrar = koinInject<PushTokenRegistrar>()
        LaunchedEffect(Unit) {
            pushTokenRegistrar.flushPendingToken()
        }

        CompositionLocalProvider(LocalNavController provides navController) {
            Box(
                modifier = Modifier.fillMaxSize().addFocusCleaner(focusManager)
            ) {
                // Écrans de premier niveau affichant la barre de navigation.
                // Le chat n'en fait pas partie : c'est un écran immersif.
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
                val showBottomBar = bottomBarScreens.any { screen ->
                    screen != null && currentRoute?.startsWith(screen) == true
                }

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
                        composable<Screen.Register> { RegisterScreen() }
                        composable<Screen.ForgotPassword> { ForgotPasswordScreen() }
                        composable<Screen.ProfileSetup> { ProfileSetupScreen() }
                        composable<Screen.Disclaimer> { DisclaimerScreen() }
                        composable<Screen.Onboarding> { OnboardingScreen() }
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

                        // Liens publics du portail citoyen mibeko.fr/textes/{slug}
                        // (et /article-{numero}). Le slug n'est pas un id interne :
                        // l'écran-relais le résout puis redirige vers Reader/Document.
                        // L'ordre compte : le motif « article » (plus spécifique)
                        // est déclaré avant le motif document seul.
                        composable<Screen.TexteResolver>(
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "https://mibeko.fr/textes/{docSlug}/article-{articleNumber}" },
                                navDeepLink { uriPattern = "https://mibeko.fr/textes/{docSlug}" },
                                navDeepLink { uriPattern = "mibeko://textes/{docSlug}/article-{articleNumber}" },
                                navDeepLink { uriPattern = "mibeko://textes/{docSlug}" }
                            )
                        ) { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.TexteResolver>()
                            TexteResolverScreen(docSlug = route.docSlug, articleNumber = route.articleNumber)
                        }

                        composable<Screen.DossierDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.DossierDetail>()
                            DossierDetailScreen(route.dossierId)
                        }

                        composable<Screen.DocumentList> { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.DocumentList>()
                            DocumentListScreen(route.typeCode, route.typeName)
                        }

                        composable<Screen.OfficialJournalList> {
                            OfficialJournalListScreen()
                        }

                        composable<Screen.OfficialJournalDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.OfficialJournalDetail>()
                            OfficialJournalDetailScreen(route.id)
                        }

                        composable<Screen.Chat> { backStackEntry ->
                            val args = backStackEntry.toRoute<Screen.Chat>()
                            ChatScreen(conversationId = args.conversationId, initialPrompt = args.initialPrompt)
                        }

                        composable<Screen.ConversationHistory> {
                            ConversationHistoryScreen()
                        }
                    }
                }
            }
        }
    }
}
