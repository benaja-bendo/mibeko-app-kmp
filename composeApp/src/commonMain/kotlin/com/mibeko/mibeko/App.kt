package com.mibeko.mibeko

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import com.mibeko.mibeko.ui.auth.ForgotPasswordScreen
import com.mibeko.mibeko.ui.auth.LoginScreen
import com.mibeko.mibeko.ui.auth.ProfileSetupScreen
import com.mibeko.mibeko.ui.auth.RegisterScreen
import com.mibeko.mibeko.ui.chat.ChatScreen
import com.mibeko.mibeko.ui.chat.ConversationHistoryScreen
import com.mibeko.mibeko.ui.contact.ContactScreen
import com.mibeko.mibeko.ui.details.DocumentDetailScreen
import com.mibeko.mibeko.ui.dossier.DossierDetailScreen
import com.mibeko.mibeko.ui.dossier.DossierScreen
import com.mibeko.mibeko.ui.downloads.DownloadsScreen
import com.mibeko.mibeko.ui.home.HomeScreen
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
import com.mibeko.mibeko.ui.settings.SettingsScreen
import com.mibeko.mibeko.ui.splash.SplashScreen
import com.mibeko.mibeko.ui.theme.MibekoTheme
import com.mibeko.mibeko.ui.update.ForceUpdateScreen
import com.mibeko.mibeko.ui.update.UpdateBanner
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.ExternalUriHandler
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.StoreUrls
import com.mibeko.mibeko.util.UpdateState
import com.mibeko.mibeko.util.VersionGate
import com.mibeko.mibeko.util.addFocusCleaner
import com.mibeko.mibeko.util.getAppVersionName
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
                navController.navigate(Screen.Login()) {
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

        // Applique le consentement télémétrie persisté au SDK Analytics
        // (toggle « Partage de statistiques anonymes » des Réglages).
        val analytics = koinInject<MibekoAnalytics>()
        LaunchedEffect(Unit) {
            analytics.applyStoredConsent()
        }

        // Force-update / bannière de mise à jour. FAIL-OPEN STRICT : tout
        // échec (réseau, parse, config) laisse l'app démarrer normalement.
        val legalApiService = koinInject<LegalApiService>()
        var updateState by remember { mutableStateOf<UpdateState>(UpdateState.UpToDate) }
        LaunchedEffect(Unit) {
            val config = runCatching { legalApiService.fetchAppConfig() }.getOrNull()
            val isAndroid = getPlatform().name.lowercase().contains("android")
            val storeUrl = (if (isAndroid) config?.store_urls?.android else config?.store_urls?.ios)
                ?.takeIf { it.isNotBlank() }
                ?: if (isAndroid) StoreUrls.PLAY else StoreUrls.APP_STORE
            val evaluated = VersionGate.evaluate(getAppVersionName(), config, storeUrl)
            updateState = when (evaluated) {
                is UpdateState.SoftUpdate ->
                    if (userPreferencesRepository.isUpdateBannerDismissed(evaluated.latestVersion)) {
                        UpdateState.UpToDate
                    } else {
                        evaluated
                    }
                else -> evaluated
            }
            when (updateState) {
                is UpdateState.ForceUpdate -> analytics.logEvent(AnalyticsEvents.APP_UPDATE_FORCED)
                is UpdateState.SoftUpdate -> analytics.logEvent(AnalyticsEvents.APP_UPDATE_PROMPTED)
                else -> Unit
            }
        }

        val forceUpdate = updateState as? UpdateState.ForceUpdate
        if (forceUpdate != null) {
            ForceUpdateScreen(forceUpdate)
            return@MibekoTheme
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
                    Screen.Dossiers::class.qualifiedName,
                    Screen.Settings::class.qualifiedName
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
                    // Le slot `content` du Scaffold ne mesure que son PREMIER
                    // enfant : bannière et NavHost doivent vivre dans un même
                    // conteneur, sinon afficher la bannière escamote toute
                    // l'application.
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Bannière de mise à jour douce, épinglée au-dessus du
                        // contenu (ignorable, mémorisée par version proposée).
                        (updateState as? UpdateState.SoftUpdate)?.let { soft ->
                            UpdateBanner(
                                state = soft,
                                modifier = Modifier.statusBarsPadding()
                            ) {
                                userPreferencesRepository.dismissUpdateBanner(soft.latestVersion)
                                updateState = UpdateState.UpToDate
                            }
                        }
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                        composable<Screen.Splash> { SplashScreen() }
                        composable<Screen.Login> { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.Login>()
                            LoginScreen(
                                redirectChatPrompt = route.redirectChatPrompt,
                                redirectToHistory = route.redirectToHistory
                            )
                        }
                        composable<Screen.Register> { RegisterScreen() }
                        composable<Screen.ForgotPassword> { ForgotPasswordScreen() }
                        composable<Screen.ProfileSetup> { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.ProfileSetup>()
                            ProfileSetupScreen(
                                redirectChatPrompt = route.redirectChatPrompt,
                                redirectToHistory = route.redirectToHistory
                            )
                        }
                        composable<Screen.Disclaimer> { DisclaimerScreen() }
                        composable<Screen.Onboarding> { OnboardingScreen() }
                        composable<Screen.Home> { HomeScreen() }
                        composable<Screen.Settings> { SettingsScreen() }
                        composable<Screen.Dossiers> { DossierScreen() }
                        composable<Screen.Library> { LibraryScreen() }
                        composable<Screen.Downloads> { DownloadsScreen() }
                        composable<Screen.Notifications> { NotificationsScreen() }
                        composable<Screen.Contact> { ContactScreen() }

                        // Le domaine mibeko.cg n'a jamais été le nôtre : seuls
                        // le scheme mibeko:// et mibeko.fr (TexteResolver plus
                        // bas) sont des entrées légitimes.
                        composable<Screen.DocumentDetail>(
                            deepLinks = listOf(
                                navDeepLink { uriPattern = "mibeko://document/{documentId}" }
                            )
                        ) { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.DocumentDetail>()
                            DocumentDetailScreen(route.documentId)
                        }

                        composable<Screen.Reader>(
                            deepLinks = listOf(
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


                        composable<Screen.OfficialJournalList> {
                            OfficialJournalListScreen()
                        }

                        composable<Screen.OfficialJournalDetail> { backStackEntry ->
                            val route = backStackEntry.toRoute<Screen.OfficialJournalDetail>()
                            OfficialJournalDetailScreen(route.id)
                        }

                        composable<Screen.Chat> { backStackEntry ->
                            val args = backStackEntry.toRoute<Screen.Chat>()
                            ChatScreen(
                                conversationId = args.conversationId,
                                initialPrompt = args.initialPrompt,
                                pinnedDocumentId = args.pinnedDocumentId,
                                pinnedLabel = args.pinnedLabel
                            )
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
}
