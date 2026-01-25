package com.mibeko.mibeko.util

import com.mibeko.mibeko.ui.navigation.Screen
import androidx.navigation.NavController

/**
 * Gère la redirection suite à un clic sur une notification push.
 */
object PushRedirectHandler {
    /**
     * Analyse les données de la notification et redirige l'utilisateur.
     */
    fun handleRedirect(data: Map<String, String>, navController: NavController) {
        val type = data["type"] ?: return
        val id = data["article_id"] ?: data["document_id"] ?: return

        when (type) {
            "reader", "article" -> {
                navController.navigate(Screen.Reader(id))
            }
            "document" -> {
                navController.navigate(Screen.DocumentDetail(id))
            }
            "dossier" -> {
                navController.navigate(Screen.DossierDetail(id))
            }
        }
    }
}
