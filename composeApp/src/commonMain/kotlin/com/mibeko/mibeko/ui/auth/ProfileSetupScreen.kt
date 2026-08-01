package com.mibeko.mibeko.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Remplace l'ancien formulaire à 3 champs requis (téléphone/profession/
 * entreprise) : un choix simple, sans friction, qui sert d'abord à mesurer
 * qui utilise réellement Mibeko (citoyen vs professionnel du droit).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    redirectChatPrompt: String? = null,
    redirectToHistory: Boolean = false
) {
    val navController = LocalNavController.current
    val viewModel: ProfileSetupViewModel = koinViewModel()
    val setupState by viewModel.setupState.collectAsState()

    LaunchedEffect(setupState) {
        if (setupState is ProfileSetupState.Success) {
            // Reçoit à son tour ce que Login a transporté (voir LoginScreen).
            val destination = when {
                redirectChatPrompt != null -> Screen.Chat(initialPrompt = redirectChatPrompt)
                redirectToHistory -> Screen.ConversationHistory
                else -> Screen.Home
            }
            navController.navigate(destination) {
                popUpTo(Screen.ProfileSetup()) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Un dernier mot") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vous êtes plutôt…",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pour vous montrer ce qui compte le plus, pas pour vous limiter — vous pourrez préciser plus tard dans les réglages.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (setupState is ProfileSetupState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ProfileTypeOption(
                    title = "Un citoyen",
                    subtitle = "Je consulte le droit pour mes besoins personnels",
                    icon = Icons.Default.Person,
                    onClick = { viewModel.selectProfileType(ProfileType.CITIZEN) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ProfileTypeOption(
                    title = "Un professionnel du droit",
                    subtitle = "Avocat, juriste, notaire, magistrat…",
                    icon = Icons.Default.Gavel,
                    onClick = { viewModel.selectProfileType(ProfileType.PROFESSIONAL) }
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            if (setupState is ProfileSetupState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (setupState as ProfileSetupState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileTypeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
