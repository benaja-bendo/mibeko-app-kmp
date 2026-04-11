package com.mibeko.mibeko.ui.navigation

import kotlinx.serialization.Serializable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

@Serializable
sealed class Screen {
    @Serializable
    data object Splash : Screen()

    @Serializable
    data object Login : Screen()

    @Serializable
    data object ProfileSetup : Screen()

    @Serializable
    data object Disclaimer : Screen()
    
    @Serializable
    data object Home : Screen()
    
    @Serializable
    data object Favorites : Screen()
    
    @Serializable
    data object Settings : Screen()
    
    @Serializable
    data class SearchResults(val query: String? = null, val tag: String? = null) : Screen()
    
    @Serializable
    data object Onboarding : Screen()
    
    @Serializable
    data class DocumentDetail(val documentId: String) : Screen()
    
    @Serializable
    data class Reader(val articleId: String) : Screen()
    
    @Serializable
    data object Dossiers : Screen()

    @Serializable
    data object Library : Screen()

    @Serializable
    data object Downloads : Screen()
    
    @Serializable
    data object Notifications : Screen()
    
    @Serializable
    data class DossierDetail(val dossierId: String) : Screen()
    
    @Serializable
    data class DocumentList(val typeCode: String, val typeName: String) : Screen()
    
    @Serializable
    data object OfficialJournalList : Screen()
    
    @Serializable
    data class OfficialJournalDetail(val id: String) : Screen()
    
    @Serializable
    data class Chat(val conversationId: String? = null, val initialPrompt: String? = null) : Screen()
    
    @Serializable
    data object ConversationHistory : Screen()
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}
