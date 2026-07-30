package com.mibeko.mibeko.util

import com.mibeko.mibeko.data.remote.AppConfigResponse

/**
 * Fiches publiées, en repli quand le serveur n'envoie pas d'URL. Sans ce
 * filet, un `MOBILE_STORE_URL_IOS` vidé côté serveur produirait un écran de
 * mise à jour obligatoire SANS bouton, donc sans aucune issue.
 */
object StoreUrls {
    const val PLAY = "https://play.google.com/store/apps/details?id=cg.mibeko.app"
    const val APP_STORE = "https://apps.apple.com/app/id6768865781"
}

/** Décision de mise à jour au démarrage. */
sealed class UpdateState {
    data object UpToDate : UpdateState()

    /** Une version plus récente existe : bannière discrète, ignorable. */
    data class SoftUpdate(val latestVersion: String, val storeUrl: String?) : UpdateState()

    /** Version sous le minimum supporté : écran bloquant vers le store. */
    data class ForceUpdate(val message: String?, val storeUrl: String?) : UpdateState()
}

/**
 * Comparaison de versions et politique de mise à jour.
 *
 * FAIL-OPEN STRICT : toute donnée illisible (config absente, version
 * non numérique) conclut « à jour ». Un bug ici ou une config serveur
 * corrompue ne doit JAMAIS pouvoir bloquer le parc installé.
 */
object VersionGate {

    /**
     * Compare deux versions « 1.2.3 » (tolère un préfixe v et des longueurs
     * différentes : 1.0 == 1.0.0). Retourne < 0, 0 ou > 0 ; null si l'une des
     * deux est illisible.
     */
    fun compare(a: String, b: String): Int? {
        val pa = parse(a) ?: return null
        val pb = parse(b) ?: return null
        val size = maxOf(pa.size, pb.size)
        for (i in 0 until size) {
            val diff = (pa.getOrElse(i) { 0 }).compareTo(pb.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parse(version: String): List<Int>? {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
            .substringBefore('-')
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.').map { it.toIntOrNull() ?: return null }
        return parts.ifEmpty { null }
    }

    fun evaluate(currentVersion: String, config: AppConfigResponse?, platformStoreUrl: String?): UpdateState {
        if (config == null) return UpdateState.UpToDate

        val belowMinimum = compare(currentVersion, config.min_supported_version)
        if (belowMinimum != null && belowMinimum < 0) {
            return UpdateState.ForceUpdate(config.message, platformStoreUrl)
        }

        val belowLatest = compare(currentVersion, config.latest_version)
        if (belowLatest != null && belowLatest < 0) {
            return UpdateState.SoftUpdate(config.latest_version, platformStoreUrl)
        }

        return UpdateState.UpToDate
    }
}
