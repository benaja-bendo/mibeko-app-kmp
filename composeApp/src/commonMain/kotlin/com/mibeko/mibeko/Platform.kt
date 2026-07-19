package com.mibeko.mibeko

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getCurrentTimeMillis(): Long

/**
 * Vrai uniquement pour un binaire de développement. Conditionne les
 * comportements réservés au debug (ex : logs réseau Ktor).
 */
expect fun isDebugBuild(): Boolean

expect fun getContentSharer(): com.mibeko.mibeko.util.ContentSharer