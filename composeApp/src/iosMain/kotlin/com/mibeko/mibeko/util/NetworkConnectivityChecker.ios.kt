package com.mibeko.mibeko.util

import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue
import kotlinx.atomicfu.atomic

/**
 * iOS implementation of NetworkConnectivityChecker using NWPathMonitor.
 */
class IosNetworkConnectivityChecker : NetworkConnectivityChecker {
    
    private val isConnected = atomic(true) // Assume connected initially
    
    init {
        startMonitoring()
    }
    
    private fun startMonitoring() {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            isConnected.value = (status == nw_path_status_satisfied)
        }
        nw_path_monitor_start(monitor)
    }
    
    override fun isNetworkAvailable(): Boolean {
        return isConnected.value
    }
}

/**
 * Factory function to get the platform-specific NetworkConnectivityChecker.
 */
actual fun getNetworkConnectivityChecker(): NetworkConnectivityChecker = IosNetworkConnectivityChecker()
