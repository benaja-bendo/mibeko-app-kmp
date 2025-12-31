package com.mibeko.mibeko.di

import com.mibeko.mibeko.data.local.getDatabaseBuilder
import com.mibeko.mibeko.data.preferences.SearchHistoryManager
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.ui.home.HomeViewModel
import com.mibeko.mibeko.ui.search.SearchViewModel
import com.mibeko.mibeko.ui.reader.ReaderViewModel
import com.mibeko.mibeko.ui.details.DocumentDetailViewModel
import com.mibeko.mibeko.ui.favorites.FavoritesViewModel
import com.mibeko.mibeko.ui.settings.SettingsViewModel
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.getNetworkConnectivityChecker
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    // User Preferences
    single { UserPreferencesRepository() }
    single { SearchHistoryManager() }
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
        }
    }

    single { 
        getDatabaseBuilder().build()
    }

    single { get<com.mibeko.mibeko.data.local.AppDatabase>().mibekoDao() }

    single { LegalApiService(get(), get<AppConfig>().baseUrl) }

    // Network connectivity checker (platform-specific implementation)
    single<NetworkConnectivityChecker> { getNetworkConnectivityChecker() }

    // Repository with all dependencies for hybrid search
    single { LocalLegalRepository(get(), get(), get(), get()) }

    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get(), get()) }
    viewModel { ReaderViewModel(get()) }
    viewModel { DocumentDetailViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}

