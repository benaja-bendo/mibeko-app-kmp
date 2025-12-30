package com.mibeko.mibeko.di

import com.mibeko.mibeko.data.local.getDatabaseBuilder
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.ui.home.HomeViewModel
import com.mibeko.mibeko.ui.search.SearchViewModel
import com.mibeko.mibeko.ui.reader.ReaderViewModel
import com.mibeko.mibeko.ui.details.DocumentDetailViewModel
import com.mibeko.mibeko.ui.favorites.FavoritesViewModel
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
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

    single { LocalLegalRepository(get(), get()) }

    viewModel { HomeViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { ReaderViewModel(get()) }
    viewModel { DocumentDetailViewModel(get()) }
    viewModel { FavoritesViewModel(get()) }
}
