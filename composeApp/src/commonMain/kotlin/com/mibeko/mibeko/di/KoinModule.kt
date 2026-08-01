package com.mibeko.mibeko.di

import com.mibeko.mibeko.data.auth.SessionEvents
import com.mibeko.mibeko.data.local.AppDatabase
import com.mibeko.mibeko.data.local.getDatabaseBuilder
import com.mibeko.mibeko.data.preferences.RecentlyViewedManager
import com.mibeko.mibeko.data.preferences.SearchHistoryManager
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.preferences.createSecureSettings
import com.mibeko.mibeko.getContentSharer
import com.mibeko.mibeko.isDebugBuild
import com.mibeko.mibeko.util.ContentSharer
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.AiApiService
import com.mibeko.mibeko.data.remote.DossierApiService
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.remote.LibraryApiService
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.NotificationRepository
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import com.mibeko.mibeko.ui.auth.ForgotPasswordViewModel
import com.mibeko.mibeko.ui.auth.LoginViewModel
import com.mibeko.mibeko.ui.auth.RegisterViewModel
import com.mibeko.mibeko.ui.auth.ProfileSetupViewModel
import com.mibeko.mibeko.ui.home.HomeViewModel
import com.mibeko.mibeko.ui.search.SearchViewModel
import com.mibeko.mibeko.ui.reader.ReaderViewModel
import com.mibeko.mibeko.ui.details.DocumentDetailViewModel
import com.mibeko.mibeko.ui.resolver.TexteResolverViewModel
import com.mibeko.mibeko.ui.library.LibraryViewModel
import com.mibeko.mibeko.ui.downloads.DownloadsViewModel
import com.mibeko.mibeko.ui.notifications.NotificationsViewModel
import com.mibeko.mibeko.ui.dossier.DossierDetailViewModel
import com.mibeko.mibeko.ui.dossier.DossierViewModel
import com.mibeko.mibeko.ui.components.DossierSelectionViewModel
import com.mibeko.mibeko.ui.settings.SettingsViewModel
import com.mibeko.mibeko.ui.officialjournal.OfficialJournalViewModel
import com.mibeko.mibeko.ui.chat.ChatViewModel
import com.mibeko.mibeko.ui.chat.ConversationHistoryViewModel
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.NotificationManager
import com.mibeko.mibeko.util.getAnalyticsManager
import com.mibeko.mibeko.util.getDeviceId
import com.mibeko.mibeko.util.getNetworkConnectivityChecker
import com.mibeko.mibeko.util.getNotificationManager
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.header
import io.ktor.client.plugins.sse.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Routes d'authentification publiques dont un 401 fait partie du contrat
 * (identifiants invalides, code de réinitialisation erroné…) : elles ne
 * doivent pas déclencher la déconnexion globale.
 */
private val PUBLIC_AUTH_ROUTES = listOf(
    "/v1/login",
    "/v1/register",
    "/v1/auth/firebase",
    "/v1/forgot-password",
    "/v1/reset-password",
    "/v1/logout"
)

val commonModule = module {
    // User Preferences (stockage sécurisé : EncryptedSharedPreferences / Keychain)
    single { UserPreferencesRepository(createSecureSettings()) }
    single { SearchHistoryManager() }
    single { RecentlyViewedManager() }

    // Événements de session (expiration du jeton observée par l'UI)
    single { SessionEvents() }
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            // Identifiant d'appareil envoyé à chaque requête : côté serveur, les
            // quotas de lecture froide s'y accrochent au lieu de l'adresse IP.
            // Sans cet en-tête, le CGNAT des opérateurs congolais ferait
            // partager un même quota à des centaines d'abonnés, qui se
            // bloqueraient mutuellement pendant leur première synchronisation.
            install(DefaultRequest) {
                header("X-Mibeko-Device", getDeviceId())
            }
            // Sans timeout, une requête peut pendre indéfiniment sur un réseau
            // dégradé (fréquent au Congo) et geler l'écran appelant. Le socket
            // est plus large que la requête : les téléchargements de documents
            // et le premier octet du streaming SSE peuvent être lents.
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 120_000
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val repository = get<UserPreferencesRepository>()
                        val token = repository.getAuthToken()
                        if (token != null) {
                            BearerTokens(token, "")
                        } else {
                            null
                        }
                    }
                }
            }
            install(Logging) {
                // Jamais de logs réseau dans un binaire release : les URLs
                // d'API n'ont rien à faire dans les logs de production.
                level = if (isDebugBuild()) LogLevel.INFO else LogLevel.NONE
            }
            install(SSE)
            // Gestion 401 globale : un jeton révoqué ou expiré côté serveur
            // purge la session locale et notifie l'UI (retour à l'écran de
            // connexion) au lieu de laisser une session zombie.
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == HttpStatusCode.Unauthorized) {
                        val path = response.request.url.encodedPath
                        val isPublicAuthRoute = PUBLIC_AUTH_ROUTES.any { path.endsWith(it) }
                        val preferences = get<UserPreferencesRepository>()
                        if (!isPublicAuthRoute && preferences.isLoggedIn()) {
                            preferences.logout()
                            // On ne purge PAS les dossiers ici : un dossier créé
                            // hors-ligne n'est encore nulle part sur le serveur,
                            // et le jeton étant déjà invalide on ne peut plus le
                            // pousser. La même personne se reconnecte et retrouve
                            // son travail ; le mélange entre comptes est traité
                            // au bon moment par guardAccountSwitch(), à la
                            // première synchronisation d'un compte différent.
                            get<SessionEvents>().notifySessionExpired()
                        }
                    }
                }
            }
        }
    }

    single {
        getDatabaseBuilder().build()
    }

    single { get<AppDatabase>().mibekoDao() }

    single { LegalApiService(get(), get<AppConfig>().baseUrl) }
    single { LibraryApiService(get(), get<AppConfig>().baseUrl) }
    single { AuthApiService(get(), get<AppConfig>().baseUrl) }
    single { AiApiService(get(), get<AppConfig>().baseUrl) }
    single<com.mibeko.mibeko.data.remote.AiChatApi> { get<AiApiService>() }
    single { DossierApiService(get(), get<AppConfig>().baseUrl) }

    // Network connectivity checker (platform-specific implementation)
    single<NetworkConnectivityChecker> { getNetworkConnectivityChecker() }

    // Notification Manager (platform-specific implementation)
    single<NotificationManager> { getNotificationManager() }

    // Analytics : façade unique, gated par le consentement (Réglages)
    single { MibekoAnalytics(getAnalyticsManager(), get()) }

    // Content Sharer (platform-specific implementation)
    single<ContentSharer> { getContentSharer() }

    // Repositories
    single { LocalLegalRepository(get(), get(), get(), get()) }
    single { DossierRepository(get(), get(), get(), get(), get()) }
    single { NotificationRepository(get(), get<AppConfig>().baseUrl) }
    single { PushTokenRegistrar(get(), get()) }

    viewModel { LoginViewModel(get(), get(), get(), get()) }
    viewModel { RegisterViewModel(get(), get(), get(), get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { ProfileSetupViewModel(get(), get(), get()) }

    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get()) }
    viewModel { ReaderViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DocumentDetailViewModel(get(), get(), get()) }
    viewModel { TexteResolverViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { OfficialJournalViewModel(get(), get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DownloadsViewModel(get()) }
    viewModel { NotificationsViewModel(get(), get()) }
    viewModel { DossierViewModel(get(), get()) }
    viewModel { DossierSelectionViewModel(get(), get()) }
    viewModel { params -> DossierDetailViewModel(params.get(), get(), get()) }
    viewModel { ChatViewModel(get(), get()) }
    viewModel { ConversationHistoryViewModel(get()) }
    viewModel { com.mibeko.mibeko.ui.contact.ContactViewModel(get(), get(), get()) }
}

