package com.mibeko.mibeko.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android implementation of NetworkConnectivityChecker using ConnectivityManager.
 * Uses Koin for Context injection instead of static singleton.
 *
 * Enregistré en `single` Koin : la surveillance démarre une fois et vit aussi
 * longtemps que le processus, il n'y a donc rien à désinscrire.
 */
class AndroidNetworkConnectivityChecker : NetworkConnectivityChecker, KoinComponent {

    private val context: Context by inject()

    private val connectivityManager: ConnectivityManager
        get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Optimiste si la mesure initiale échoue (Context pas encore résolu par
    // Koin) : mieux vaut tenter l'appel API que déclarer l'appareil hors-ligne
    // à tort — et surtout, ne jamais faire tomber la construction du singleton.
    private val _isOnline = MutableStateFlow(runCatching { queryNow() }.getOrDefault(true))
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Fail-open : si l'enregistrement échoue (SecurityException sur certains
        // ROM, quota de callbacks atteint), on garde la mesure ponctuelle plutôt
        // que de faire tomber le démarrage.
        runCatching {
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = queryNow()
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = queryNow()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities
                    ) {
                        _isOnline.value = queryNow()
                    }
                }
            )
        }
    }

    /**
     * `NET_CAPABILITY_VALIDATED` n'est **pas** exigée.
     *
     * Cette capacité n'est posée qu'une fois la sonde de validation d'Android
     * revenue : elle manque derrière un portail captif, quand la sonde est
     * bloquée, et pendant les premières secondes de toute connexion lente —
     * situations ordinaires sur une connexion contrainte. L'exiger revenait à
     * déclarer l'appareil hors-ligne alors qu'il joignait parfaitement l'API,
     * et à sauter la synchronisation initiale du corpus.
     */
    private fun queryNow(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun isNetworkAvailable(): Boolean = queryNow()
}

/**
 * Factory function to get the platform-specific NetworkConnectivityChecker.
 */
actual fun getNetworkConnectivityChecker(): NetworkConnectivityChecker = AndroidNetworkConnectivityChecker()
