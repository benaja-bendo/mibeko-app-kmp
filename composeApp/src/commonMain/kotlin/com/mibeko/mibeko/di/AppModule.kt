package com.mibeko.mibeko.di

import com.mibeko.mibeko.data.local.AppDatabase
import com.mibeko.mibeko.data.local.getDatabaseBuilder
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object AppModule {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private val database: AppDatabase by lazy {
        getDatabaseBuilder().build()
    }

    private val mibekoDao by lazy { database.mibekoDao() }
    
    private val apiService by lazy { LegalApiService(client) }

    val repository by lazy { 
        LocalLegalRepository(mibekoDao, apiService) 
    }
}
