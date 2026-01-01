package com.mibeko.mibeko

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getCurrentTimeMillis(): Long