package com.mibeko.mibeko.util

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic interface for checking network connectivity.
 * Each platform (Android/iOS) provides its own implementation.
 */
interface NetworkConnectivityChecker {
    /**
     * État réseau à l'instant T.
     *
     * ⚠️ Ne vaut que comme *indice*. Le seul test fiable de joignabilité du
     * backend reste l'appel API lui-même : ne jamais s'en servir pour renoncer
     * à un appel dont l'échec serait annoncé à l'utilisateur comme une absence.
     */
    fun isNetworkAvailable(): Boolean

    /**
     * Même information, mais **observable**. Émet à chaque changement d'état,
     * valeur courante disponible immédiatement via `.value`.
     *
     * Existe parce qu'échantillonner la connectivité une seule fois (dans le
     * `init` d'un ViewModel) enfermait toute la session dans l'état constaté au
     * démarrage : une app lancée pendant que le réseau n'était pas encore
     * joignable n'appelait plus jamais l'API, même une fois la connexion revenue.
     */
    val isOnline: StateFlow<Boolean>
}

/**
 * Expect function to get the platform-specific NetworkConnectivityChecker.
 * Implemented in androidMain and iosMain.
 */
expect fun getNetworkConnectivityChecker(): NetworkConnectivityChecker
