package com.mibeko.mibeko.ui.resolver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.ui.components.MibekoErrorState
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Écran-relais des liens publics `mibeko.fr/textes/{slug}` (App Links) — la
 * colle du funnel site → app. Affiche un court chargement puis redirige en se
 * retirant de la pile en cas de succès ; en cas d'échec, reste affiché avec un
 * message honnête (jamais de redirection silencieuse vers l'accueil).
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
            // NotFound / Failed / Resolving : on reste sur cet écran, jamais de
            // redirection silencieuse (règle produit non négociable).
            else -> null
        }
        if (destination != null) {
            navController.navigate(destination) {
                // Retire l'écran-relais de la pile : le retour ne le rejoue pas.
                popUpTo(Screen.TexteResolver(docSlug, articleNumber)) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val goHome = {
        navController.navigate(Screen.Home) {
            popUpTo(Screen.TexteResolver(docSlug, articleNumber)) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val t = target) {
            TexteResolverViewModel.Target.Resolving -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            TexteResolverViewModel.Target.NotFound -> {
                ResolverNotFoundState(onGoHome = goHome)
            }

            is TexteResolverViewModel.Target.Failed -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MibekoErrorState(offline = t.error.offline, onRetry = t.error.retry)
                    TextButton(onClick = goHome) {
                        Text("Retour à l'accueil")
                    }
                }
            }

            // Reader / Document : navigation en cours via le LaunchedEffect ci-dessus.
            else -> Unit
        }
    }
}

/** Lien inconnu du corpus publié — un vrai 404, pas une panne. */
@Composable
private fun ResolverNotFoundState(onGoHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LinkOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ce lien ne correspond à aucun texte",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Il a peut-être expiré ou n'a jamais été publié.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onGoHome) {
            Text("Retour à l'accueil")
        }
    }
}
