package com.mibeko.mibeko.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import com.mibeko.mibeko.ApiConstants

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
                    override val baseUrl = ApiConstants.BASE_URL
                }
            }
        })
    }
}
