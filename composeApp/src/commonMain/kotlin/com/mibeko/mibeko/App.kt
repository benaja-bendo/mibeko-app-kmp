package com.mibeko.mibeko

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.mibeko.mibeko.ui.splash.SplashScreen
import com.mibeko.mibeko.ui.theme.MibekoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MibekoTheme {
        Navigator(SplashScreen()) { navigator ->
            FadeTransition(navigator)
        }
    }
}