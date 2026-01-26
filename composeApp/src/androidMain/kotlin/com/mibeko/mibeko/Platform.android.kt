package com.mibeko.mibeko

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun getContentSharer(): com.mibeko.mibeko.util.ContentSharer = 
    com.mibeko.mibeko.util.AndroidContentSharer(MibekoApp.INSTANCE)