package com.mibeko.mibeko.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class LegalApiService(private val client: HttpClient) {
    
    // Placeholder URL - change to real endpoint when available
    private val BASE_URL = "http://192.168.1.149:8000/api"


    suspend fun fetchAllData(): SyncResponse {
        return client.get("$BASE_URL/sync").body()
    }
}
