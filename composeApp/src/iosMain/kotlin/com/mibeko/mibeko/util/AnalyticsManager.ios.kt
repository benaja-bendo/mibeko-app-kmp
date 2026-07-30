package com.mibeko.mibeko.util

/**
 * Analytics iOS : délègue aux closures Firebase posées par iOSApp.swift dans
 * [FirebaseIosBridge]. Sans configuration Firebase (plist absent), no-op.
 */
class IosAnalyticsManager : AnalyticsManager {
    override fun logEvent(name: String, params: Map<String, Any>?) {
        FirebaseIosBridge.logEvent?.invoke(name, params)
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        FirebaseIosBridge.setCollectionEnabled?.invoke(enabled)
    }
}

actual fun getAnalyticsManager(): AnalyticsManager = IosAnalyticsManager()
