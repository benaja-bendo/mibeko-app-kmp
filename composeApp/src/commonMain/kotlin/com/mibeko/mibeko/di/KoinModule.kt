package com.mibeko.mibeko.di

import com.mibeko.mibeko.data.local.getDatabaseBuilder
import com.mibeko.mibeko.data.preferences.SearchHistoryManager
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.AiApiService
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.repository.DossierRepository
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.repository.NotificationRepository
import com.mibeko.mibeko.ui.auth.LoginViewModel
import com.mibeko.mibeko.ui.auth.RegisterViewModel
import com.mibeko.mibeko.ui.auth.ProfileSetupViewModel
import com.mibeko.mibeko.ui.home.HomeViewModel
import com.mibeko.mibeko.ui.search.SearchViewModel
import com.mibeko.mibeko.ui.reader.ReaderViewModel
import com.mibeko.mibeko.ui.details.DocumentDetailViewModel
import com.mibeko.mibeko.ui.library.LibraryViewModel
import com.mibeko.mibeko.ui.downloads.DownloadsViewModel
import com.mibeko.mibeko.ui.notifications.NotificationsViewModel
import com.mibeko.mibeko.ui.dossier.DossierDetailViewModel
import com.mibeko.mibeko.ui.dossier.DossierViewModel
import com.mibeko.mibeko.ui.favorites.FavoritesViewModel
import com.mibeko.mibeko.ui.settings.SettingsViewModel
import com.mibeko.mibeko.ui.officialjournal.OfficialJournalViewModel
import com.mibeko.mibeko.ui.chat.ChatViewModel
import com.mibeko.mibeko.ui.chat.ConversationHistoryViewModel
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.NotificationManager
import com.mibeko.mibeko.util.getNetworkConnectivityChecker
import com.mibeko.mibeko.util.getNotificationManager
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    // User Preferences
    single { UserPreferencesRepository() }
    single { SearchHistoryManager() }
    single { com.mibeko.mibeko.data.preferences.RecentlyViewedManager() }
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
                level = LogLevel.INFO
            }
            install(SSE)
        }
    }

    single {  
        getDatabaseBuilder().build()
    }

    single { get<com.mibeko.mibeko.data.local.AppDatabase>().mibekoDao() }

    single { LegalApiService(get(), get<AppConfig>().baseUrl) }
    single { AuthApiService(get(), get<AppConfig>().baseUrl) }
    single { AiApiService(get(), get<AppConfig>().baseUrl) }

    // Network connectivity checker (platform-specific implementation)
    single<NetworkConnectivityChecker> { getNetworkConnectivityChecker() }

    // Notification Manager (platform-specific implementation)
    single<NotificationManager> { getNotificationManager() }

    // Content Sharer (platform-specific implementation)
    single<com.mibeko.mibeko.util.ContentSharer> { com.mibeko.mibeko.getContentSharer() }

    // Repositories
    single { LocalLegalRepository(get(), get(), get(), get()) }
    single { DossierRepository(get(), get()) }
    single { NotificationRepository(get(), get<AppConfig>().baseUrl) }

    viewModel { LoginViewModel(get(), get()) }
    viewModel { RegisterViewModel(get(), get()) }
    viewModel { ProfileSetupViewModel(get(), get()) }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { ReaderViewModel(get(), get(), get(), get(), get()) }
    viewModel { DocumentDetailViewModel(get(), get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { OfficialJournalViewModel(get(), get()) }
    viewModel { LibraryViewModel(get(), get()) }
    viewModel { DownloadsViewModel(get()) }
    viewModel { NotificationsViewModel(get()) }
    viewModel { DossierViewModel(get()) }
    viewModel { params -> DossierDetailViewModel(params.get(), get(), get()) }
    viewModel { ChatViewModel(get()) }
    viewModel { ConversationHistoryViewModel(get()) }
}

