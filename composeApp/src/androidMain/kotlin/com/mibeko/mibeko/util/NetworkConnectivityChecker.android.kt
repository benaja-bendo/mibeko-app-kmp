package com.mibeko.mibeko.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.mibeko.mibeko.MibekoApp

/**
 * Android implementation of NetworkConnectivityChecker using ConnectivityManager.
 */
class AndroidNetworkConnectivityChecker : NetworkConnectivityChecker {
    
    override fun isNetworkAvailable(): Boolean {
        val context = MibekoApp.INSTANCE
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/**
 * Factory function to get the platform-specific NetworkConnectivityChecker.
 */
actual fun getNetworkConnectivityChecker(): NetworkConnectivityChecker = AndroidNetworkConnectivityChecker()
