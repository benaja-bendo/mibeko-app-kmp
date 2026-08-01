package com.mibeko.mibeko.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Contexte applicatif injecté par Koin (cf. `Platform.android.kt`). */
private object AnalyticsContextProvider : KoinComponent {
    val context: Context by inject()
}

class AndroidAnalyticsManager : AnalyticsManager {
    // Contexte applicatif : l'ancienne résolution via ActivityProvider rendait
    // le SDK nul dès qu'aucune Activity n'était au premier plan (service FCM,
    // démarrage) et perdait silencieusement les événements.
    private val analytics: FirebaseAnalytics
        get() = FirebaseAnalytics.getInstance(AnalyticsContextProvider.context)

    override fun logEvent(name: String, params: Map<String, Any>?) {
        val bundle = params?.let {
            Bundle().apply {
                it.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Double -> putDouble(key, value)
                        is Float -> putFloat(key, value)
                        is Boolean -> putBoolean(key, value)
                        else -> putString(key, value.toString())
                    }
                }
            }
        }
        analytics.logEvent(name, bundle)
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }
}

actual fun getAnalyticsManager(): AnalyticsManager = AndroidAnalyticsManager()
