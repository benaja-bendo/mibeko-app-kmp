package com.mibeko.mibeko.ui.navigation

import kotlinx.serialization.Serializable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

@Serializable
sealed class Screen {
    @Serializable
    data object Splash : Screen()

    /**
     * [redirectChatPrompt]/[redirectToHistory] transportent l'intention d'un
     * invité qui heurte le mur de connexion (question à l'assistant tapée sans
     * compte, ou historique des conversations) — sans eux, la connexion
     * réussie renvoyait toujours vers l'accueil et la question était perdue.
     */
    @Serializable
    data class Login(
        val redirectChatPrompt: String? = null,
        val redirectToHistory: Boolean = false
    ) : Screen()

    @Serializable
    data object Register : Screen()

    @Serializable
    data object ForgotPassword : Screen()

    /** Voir [Login] : mêmes champs, propagés depuis Login pour un tout nouveau compte. */
    @Serializable
    data class ProfileSetup(
        val redirectChatPrompt: String? = null,
        val redirectToHistory: Boolean = false
    ) : Screen()

    @Serializable
    data object Disclaimer : Screen()
    
    @Serializable
    data object Home : Screen()

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

    /**
     * Destination-relais des liens publics `mibeko.fr/textes/{slug}` (et
     * `/article-{numero}`). Le slug n'est pas un identifiant interne : cet écran
     * le résout en document (et éventuellement en article), puis redirige vers
     * [Reader] ou [DocumentDetail]. C'est la cible des App Links (host mibeko.fr,
     * pathPrefix /textes) — cf. AndroidManifest et App.kt.
     */
    @Serializable
    data class TexteResolver(val docSlug: String, val articleNumber: String? = null) : Screen()
    
    @Serializable
    data object Dossiers : Screen()

    @Serializable
    data object Library : Screen()

    @Serializable
    data object Downloads : Screen()
    
    @Serializable
    data object Notifications : Screen()

    @Serializable
    data object Contact : Screen()
    
    @Serializable
    data class DossierDetail(val dossierId: String) : Screen()
    
    @Serializable
    data object OfficialJournalList : Screen()
    
    @Serializable
    data class OfficialJournalDetail(val id: String) : Screen()
    
    @Serializable
    data class Chat(
        val conversationId: String? = null,
        val initialPrompt: String? = null,
        /**
         * Document épinglé d'emblée comme référence (« Analyser avec
         * l'assistant » depuis le lecteur). On épingle plutôt que de
         * pré-remplir une question : la formulation reste celle de
         * l'utilisateur, l'assistant se contente de restreindre son périmètre.
         */
        val pinnedDocumentId: String? = null,
        val pinnedLabel: String? = null
    ) : Screen()
    
    @Serializable
    data object ConversationHistory : Screen()
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}
