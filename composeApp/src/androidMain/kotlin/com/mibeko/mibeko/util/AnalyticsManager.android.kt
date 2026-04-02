package com.mibeko.mibeko.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AndroidAnalyticsManager : AnalyticsManager {
    private val analytics: FirebaseAnalytics?
        get() = ActivityProvider.getActivity()?.let { activity ->
            FirebaseAnalytics.getInstance(activity)
        }

    /**
     * Log un événement spécifique via Firebase Analytics
     */
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
        analytics?.logEvent(name, bundle)
    }

    override fun setUserId(id: String?) {
        analytics?.setUserId(id)
    }

    override fun setUserProperty(name: String, value: String) {
        analytics?.setUserProperty(name, value)
    }
}

actual fun getAnalyticsManager(): AnalyticsManager = AndroidAnalyticsManager()
