package com.mibeko.mibeko.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun initKoin() {
    // Guard against double initialization
    try {
        KoinPlatform.getKoin()
        return // Already initialized
    } catch (_: Exception) {
        // Not initialized yet, continue
    }
    
    startKoin {
        modules(commonModule, module {
            single<AppConfig> {
                object : AppConfig {
                    override val baseUrl = "http://192.168.1.149:8000/api"
                }
            }
        })
    }
}
