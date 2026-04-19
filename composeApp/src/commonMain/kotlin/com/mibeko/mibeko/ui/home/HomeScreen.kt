package com.mibeko.mibeko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mibeko.composeapp.generated.resources.Res
import mibeko.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.components.NetworkStatusBanner
import com.mibeko.mibeko.ui.theme.MibekoGold
import com.mibeko.mibeko.ui.theme.MibekoGoldDark
import com.mibeko.mibeko.ui.theme.MibekoBluePrimary
import com.mibeko.mibeko.ui.theme.MibekoBlueDark
import com.mibeko.mibeko.util.formatIsoDate

@Composable
fun HomeScreen() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<HomeViewModel>()
        
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
        var showContent by remember { mutableStateOf(false) }
        
        // Refresh data on resume
        LaunchedEffect(Unit) {
            viewModel.refreshNetworkStatus()
            delay(100)
            showContent = true
        }

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Blue Header with Logo
                item {
                    HomeHeader(
                        onNotificationsClick = {
                            navController.navigate(Screen.Notifications)
                        },
                        onHistoryClick = {
                            if (uiState.isLoggedIn) {
                                navController.navigate(Screen.ConversationHistory)
                            } else {
                                navController.navigate(Screen.Login)
                            }
                        }
                    )
                }
                
                // Search Trigger Button (navigates to Chat or Login)
                item {
                    AiChatInputCard(
                        onSend = { query ->
                            if (query.isNotBlank()) {
                                if (uiState.isLoggedIn) {
                                    navController.navigate(Screen.Chat(initialPrompt = query))
                                } else {
                                    navController.navigate(Screen.Login)
                                }
                            }
                        }
                    )
                }

                // Network Status Banner (FR3)
                item {
                    NetworkStatusBanner(
                        isNetworkAvailable = uiState.isNetworkAvailable,
                        isSyncing = uiState.isSyncing
                    )
                }
                
                // Zone 1: Journal Officiel (Horizontal List)
                item {
                    AnimatedVisibility(
                        visible = !uiState.isLoading && uiState.officialJournals.isNotEmpty(),
                        enter = fadeIn() + slideInVertically { 30 }
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Journal Officiel",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(onClick = { navController.navigate(Screen.OfficialJournalList) }) {
                                    Text(
                                        text = "Voir plus",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MibekoBluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.officialJournals.take(5)) { journal ->
                                    JournalOfficielCard(
                                        title = journal.title,
                                        date = "Publié le ${formatIsoDate(journal.publication_date)}",
                                        excerpt = "Consulter le journal officiel et ses documents annexes...",
                                        onClick = { navController.navigate(Screen.OfficialJournalDetail(journal.id)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty state or Loading
                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
 

/**
 * En-tête avec dégradé bleu institutionnel, logo Mibeko et titre.
 */
@Composable
private fun HomeHeader(
    onNotificationsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MibekoBluePrimary, MibekoBlueDark)
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 48.dp), // Extra padding at bottom for overlap
        contentAlignment = Alignment.Center
    ) {
        // History Icon (Top Left)
        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 0.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Historique",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
        }

        // Notification Icon (Top Right)
        IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 0.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo inside white rounded square
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.size(72.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "Mibeko Logo",
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Mibeko",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                text = "Mobile",
                style = MaterialTheme.typography.labelSmall,
                color = MibekoGoldDark,
                fontWeight = FontWeight.Bold
            )
            }
        }
    }
}

/**
 * Carte de saisie pour discuter avec l'IA.
 * Remplace l'ancien bouton de recherche.
 */
@Composable
private fun AiChatInputCard(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-30).dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "IA",
                    tint = MibekoBluePrimary,
                    modifier = Modifier.padding(top = 8.dp).size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            text = "Discutez avec l'IA juridique pour vos recherches...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 4
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* TODO: Attach file */ }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Joindre un fichier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { /* TODO: Voice input */ }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Saisie vocale",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                IconButton(
                    onClick = { onSend(text) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MibekoBlueDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).offset(x = 2.dp) // slight offset for send icon
                    )
                }
            }
        }
    }
}

/**
 * Carte pour afficher un document du Journal Officiel.
 * Design basé sur la maquette.
 */
@Composable
private fun JournalOfficielCard(title: String, date: String, excerpt: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.background(MibekoBluePrimary.copy(alpha = 0.05f))) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MibekoBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CONSULTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MibekoBluePrimary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
