package com.mibeko.mibeko

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.splash.SplashScreen
import com.mibeko.mibeko.ui.theme.MibekoTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val userPreferencesRepository = koinInject<UserPreferencesRepository>()
    
    // Collect the theme from the repository reactively
    val theme by userPreferencesRepository.theme.collectAsState()
    
    val isDarkTheme = when (theme) {
        UserPreferencesRepository.AppTheme.LIGHT -> false
        UserPreferencesRepository.AppTheme.DARK -> true
        UserPreferencesRepository.AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MibekoTheme(darkTheme = isDarkTheme) {
        Navigator(SplashScreen()) { navigator ->
            FadeTransition(navigator)
        }
    }
}