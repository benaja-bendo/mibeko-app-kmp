package com.mibeko.mibeko.util

class IosAnalyticsManager : AnalyticsManager {
    override fun logEvent(name: String, params: Map<String, Any>?) {
        // TODO: Implémenter avec Firebase Analytics iOS via Cocoapods ou SPM
    }

    override fun setUserId(id: String?) {
        // TODO: Implémenter avec Firebase Analytics iOS
    }

    override fun setUserProperty(name: String, value: String) {
        // TODO: Implémenter avec Firebase Analytics iOS
    }
}

actual fun getAnalyticsManager(): AnalyticsManager = IosAnalyticsManager()
