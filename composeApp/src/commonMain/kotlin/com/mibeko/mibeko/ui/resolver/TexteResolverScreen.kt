package com.mibeko.mibeko.ui.resolver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Écran-relais des liens publics `mibeko.fr/textes/{slug}` (App Links). Affiche
 * un court chargement, résout le slug en identifiants internes, puis redirige
 * en se retirant de la pile (l'utilisateur ne revient jamais sur cet écran).
 */
@Composable
fun TexteResolverScreen(docSlug: String, articleNumber: String?) {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<TexteResolverViewModel>()
    val target by viewModel.target.collectAsState()

    LaunchedEffect(docSlug, articleNumber) {
        viewModel.resolve(docSlug, articleNumber)
    }

    LaunchedEffect(target) {
        val destination = when (val t = target) {
            is TexteResolverViewModel.Target.Reader -> Screen.Reader(t.articleId)
            is TexteResolverViewModel.Target.Document -> Screen.DocumentDetail(t.documentId)
            TexteResolverViewModel.Target.Home -> Screen.Home
            TexteResolverViewModel.Target.Resolving -> null
        }
        if (destination != null) {
            navController.navigate(destination) {
                // Retire l'écran-relais de la pile : le retour ne le rejoue pas.
                popUpTo(Screen.TexteResolver(docSlug, articleNumber)) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
