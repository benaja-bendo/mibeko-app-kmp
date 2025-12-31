package com.mibeko.mibeko.util

/**
 * Platform-agnostic interface for checking network connectivity.
 * Each platform (Android/iOS) provides its own implementation.
 */
interface NetworkConnectivityChecker {
    /**
     * Check if the device currently has network connectivity.
     * @return true if network is available, false otherwise.
     */
    fun isNetworkAvailable(): Boolean
}

/**
 * Expect function to get the platform-specific NetworkConnectivityChecker.
 * Implemented in androidMain and iosMain.
 */
expect fun getNetworkConnectivityChecker(): NetworkConnectivityChecker
