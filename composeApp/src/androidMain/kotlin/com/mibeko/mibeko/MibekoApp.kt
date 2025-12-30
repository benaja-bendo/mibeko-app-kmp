package com.mibeko.mibeko

import android.app.Application
import android.content.Context
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.mibeko.mibeko.di.commonModule

class MibekoApp : Application() {
    companion object {
        lateinit var INSTANCE: MibekoApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        
        startKoin {
            androidContext(this@MibekoApp)
            modules(commonModule, module {
                single<com.mibeko.mibeko.di.AppConfig> {
                    object : com.mibeko.mibeko.di.AppConfig {
                        override val baseUrl = BuildConfig.BASE_URL
                    }
                }
            })
        }
    }
}
