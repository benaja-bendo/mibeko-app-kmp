package com.mibeko.mibeko.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of NetworkConnectivityChecker using NWPathMonitor.
 *
 * Enregistré en `single` Koin : la surveillance démarre une fois et vit aussi
 * longtemps que le processus.
 */
class IosNetworkConnectivityChecker : NetworkConnectivityChecker {

    // Optimiste au démarrage : tant que NWPathMonitor n'a pas répondu, mieux
    // vaut tenter l'appel API que déclarer l'appareil hors-ligne à tort.
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Référence conservée : un moniteur seulement local à la fonction de
    // démarrage peut être libéré, et cesse alors silencieusement d'émettre.
    private val monitor: nw_path_monitor_t = nw_path_monitor_create()

    init {
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            _isOnline.value = (nw_path_get_status(path) == nw_path_status_satisfied)
        }
        nw_path_monitor_start(monitor)
    }

    override fun isNetworkAvailable(): Boolean = _isOnline.value
}

/**
 * Factory function to get the platform-specific NetworkConnectivityChecker.
 */
actual fun getNetworkConnectivityChecker(): NetworkConnectivityChecker = IosNetworkConnectivityChecker()
