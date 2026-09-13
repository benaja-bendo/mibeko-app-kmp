package com.mibeko.mibeko.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmailVerificationScreen(
    onVerified: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: EmailVerificationViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.checkVerification() }
    LaunchedEffect(state.verified) { if (state.verified) onVerified() }
    LaunchedEffect(state.loggedOut) { if (state.loggedOut) onLoggedOut() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.MarkEmailUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Vérifiez votre adresse e-mail",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Nous vous avons envoyé un lien. Ouvrez-le, puis revenez ici pour continuer la création de votre compte.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            state.message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = viewModel::checkVerification,
                enabled = !state.checking && !state.resending,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("J’ai vérifié mon e-mail")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = viewModel::resend,
                enabled = !state.checking && !state.resending,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.resending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Renvoyer l’e-mail")
            }
            TextButton(onClick = viewModel::logout) { Text("Utiliser une autre adresse") }
        }
    }
}
