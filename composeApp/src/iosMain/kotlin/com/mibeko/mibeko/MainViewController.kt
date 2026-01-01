package com.mibeko.mibeko

import androidx.compose.ui.window.ComposeUIViewController
import com.mibeko.mibeko.di.initKoin

fun MainViewController() = run {
    initKoin()
    ComposeUIViewController { App() }
}