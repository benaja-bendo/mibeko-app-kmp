package com.mibeko.mibeko.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android implementation of NetworkConnectivityChecker using ConnectivityManager.
 * Uses Koin for Context injection instead of static singleton.
 */
class AndroidNetworkConnectivityChecker : NetworkConnectivityChecker, KoinComponent {
    
    private val context: Context by inject()
    
    override fun isNetworkAvailable(): Boolean {
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
