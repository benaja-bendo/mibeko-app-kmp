package com.mibeko.mibeko.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.components.MibekoErrorState
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.util.formatRelativeTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<NotificationsViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Une panne au rafraîchissement, alors que des notifications sont déjà
        // affichées avec succès, ne doit pas les remplacer par un écran
        // d'erreur — un snackbar suffit. Liste vide : voir MibekoErrorState
        // plus bas, qui reste affiché (pas de disparition automatique).
        LaunchedEffect(uiState.error, uiState.notifications.isNotEmpty()) {
            if (uiState.error != null && uiState.notifications.isNotEmpty()) {
                snackbarHostState.showSnackbar("Je n'ai pas pu vérifier — liste non actualisée")
                viewModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    actions = {
                        if (uiState.notifications.any { !it.isRead }) {
                            TextButton(onClick = { viewModel.markAllAsRead() }) {
                                Text("Tout lire", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            val isLoggedIn = koinInject<UserPreferencesRepository>().isLoggedIn()
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (!isLoggedIn) {
                    // État invité honnête : les alertes sont liées au compte,
                    // l'ancien « Vous êtes à jour ! » était mensonger.
                    GuestNotificationsView(onLogin = { navController.navigate(Screen.Login()) })
                } else {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadNotifications() },
                        state = rememberPullToRefreshState(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            // Premier chargement : l'indicateur de PullToRefreshBox
                            // suffit, pas de « Aucune notification » qui clignote.
                            uiState.isLoading && uiState.notifications.isEmpty() -> {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                            // Panne réseau/API : jamais confondue avec une boîte
                            // vide (règle produit non négociable).
                            uiState.error != null && uiState.notifications.isEmpty() -> {
                                MibekoErrorState(
                                    offline = uiState.error!!.offline,
                                    onRetry = uiState.error!!.retry,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            uiState.notifications.isEmpty() -> {
                                EmptyNotificationsView()
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uiState.notifications) { notification ->
                                        NotificationItem(
                                            notification = notification,
                                            onClick = {
                                                viewModel.markAsRead(notification.id)
                                                // Une alerte de veille désigne un texte :
                                                // la marquer lue sans y conduire laissait
                                                // l'utilisateur le chercher lui-même.
                                                notification.targetSlug()?.let { slug ->
                                                    navController.navigate(
                                                        Screen.TexteResolver(
                                                            docSlug = slug,
                                                            articleNumber = notification.data?.get("article")
                                                        )
                                                    )
                                                }
                                            },
                                            onDelete = { viewModel.deleteNotification(notification.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NotificationItem(
        notification: NotificationUiModel,
        onClick: () -> Unit,
        onDelete: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            shadowElevation = if (notification.isRead) 1.dp else 4.dp,
            border = if (notification.isRead) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon based on type
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (notification.type) {
                                "legal_update" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                "dossier_alert" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (notification.type) {
                            "legal_update" -> Icons.Default.Gavel
                            "dossier_alert" -> Icons.Default.FolderSpecial
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (notification.type) {
                            "legal_update" -> MaterialTheme.colorScheme.secondary
                            "dossier_alert" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!notification.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = formatRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun GuestNotificationsView(onLogin: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Connectez-vous pour recevoir des alertes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Les alertes de veille juridique et de vos dossiers sont liées à votre compte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onLogin, shape = RoundedCornerShape(8.dp)) {
                Text("Se connecter")
            }
        }
    }

    @Composable
    private fun EmptyNotificationsView() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Aucune notification",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                "Vous êtes à jour !",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            )
        }
    }
