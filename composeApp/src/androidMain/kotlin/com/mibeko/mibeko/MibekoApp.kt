package com.mibeko.mibeko

import android.app.Application
import android.content.Context

class MibekoApp : Application() {
    companion object {
        lateinit var INSTANCE: MibekoApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}
