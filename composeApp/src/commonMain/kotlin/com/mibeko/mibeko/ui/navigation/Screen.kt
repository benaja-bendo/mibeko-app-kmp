package com.mibeko.mibeko.ui.navigation

import kotlinx.serialization.Serializable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

@Serializable
sealed class Screen {
    @Serializable
    data object Splash : Screen()

    @Serializable
    data object Disclaimer : Screen()
    
    @Serializable
    data object Home : Screen()
    
    @Serializable
    data object Explorer : Screen()
    
    @Serializable
    data object Favorites : Screen()
    
    @Serializable
    data object Settings : Screen()
    
    @Serializable
    data class SearchResults(val query: String? = null, val tag: String? = null) : Screen()
    
    @Serializable
    data object Onboarding : Screen()
    
    @Serializable
    data class ActiveSearch(val tag: String? = null) : Screen()
    
    @Serializable
    data class DocumentDetail(val documentId: String) : Screen()
    
    @Serializable
    data class Reader(val articleId: String) : Screen()
    
    @Serializable
    data object Dossiers : Screen()
    
    @Serializable
    data class DossierDetail(val dossierId: String) : Screen()
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}
