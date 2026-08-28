package com.mibeko.mibeko.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.mibeko.mibeko.data.remote.RemoteDocument
import com.mibeko.mibeko.ui.components.EmailVerificationBanner
import com.mibeko.mibeko.ui.components.MibekoErrorBanner
import com.mibeko.mibeko.ui.components.NetworkStatusBanner
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.navigation.switchTopLevelTab
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.formatIsoDate
import mibeko.composeapp.generated.resources.Res
import mibeko.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Accueil : l'assistant est intégré ici (pas d'onglet dédié) — champ hero en
 * tête, suggestions au focus, puis l'actualité du Journal Officiel. Un accès
 * secondaire clair mène à la Bibliothèque pour la recherche documentaire
 * (les deux intentions — question en langage naturel / recherche d'un texte —
 * restent séparées, comme sur le web).
 */
@Composable
fun HomeScreen() {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<HomeViewModel>()

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Le corps documentaire de l'accueil a-t-il quelque chose à montrer ?
    // Détermine qui, de la bannière du haut ou du bloc à la place du contenu,
    // porte le message d'échec — jamais les deux, ils diraient la même chose.
    val hasDocumentContent = uiState.popularCodes.isNotEmpty() ||
        uiState.recentlyAdded.isNotEmpty() ||
        uiState.officialJournals.isNotEmpty()

    // Masquage local de la bannière « e-mail non vérifié » pour la session.
    var emailBannerDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Résultat du renvoi de vérification → snackbar, puis on efface le message.
    LaunchedEffect(uiState.verificationResendMessage) {
        uiState.verificationResendMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearVerificationResendMessage()
        }
    }

    // `now` s'actualise tant qu'un cooldown est en cours, pour que le bouton
    // « Renvoyer l'e-mail » se réactive de lui-même à l'expiration (sans ticker,
    // `now` resterait figé à l'instant du dernier envoi → bouton bloqué à vie).
    val cooldownUntil = uiState.verificationResendCooldownUntil
    val now by produceState(initialValue = com.mibeko.mibeko.getCurrentTimeMillis(), cooldownUntil) {
        value = com.mibeko.mibeko.getCurrentTimeMillis()
        while (value < cooldownUntil) {
            delay(1000)
            value = com.mibeko.mibeko.getCurrentTimeMillis()
        }
    }
    val canResendVerification = now >= cooldownUntil
    val analytics = koinInject<MibekoAnalytics>()

    val askAssistant: (String) -> Unit = { prompt ->
        if (prompt.isNotBlank()) {
            if (uiState.isLoggedIn) {
                navController.navigate(Screen.Chat(initialPrompt = prompt))
            } else {
                analytics.logEvent(AnalyticsEvents.LOGIN_WALL_SHOWN, mapOf("context" to "assistant"))
                // La question tapée en invité ne doit pas se perdre au passage
                // par la connexion — Login (et ProfileSetup pour un nouveau
                // compte) la renvoie vers Screen.Chat une fois connecté.
                navController.navigate(Screen.Login(redirectChatPrompt = prompt))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            state = rememberPullToRefreshState(),
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                HomeTopBar(
                    onHistoryClick = {
                        if (uiState.isLoggedIn) {
                            navController.navigate(Screen.ConversationHistory)
                        } else {
                            analytics.logEvent(
                                AnalyticsEvents.LOGIN_WALL_SHOWN,
                                mapOf("context" to "conversation_history")
                            )
                            navController.navigate(Screen.Login(redirectToHistory = true))
                        }
                    },
                    onNotificationsClick = { navController.navigate(Screen.Notifications) }
                )
            }

            item {
                AssistantHero(
                    suggestions = uiState.aiSuggestions,
                    onAsk = askAssistant
                )
            }

            // Posture douce P1.17 : rappel non bloquant de vérification d'e-mail.
            if (uiState.showEmailVerificationBanner && !emailBannerDismissed) {
                item {
                    EmailVerificationBanner(
                        isResending = uiState.isResendingVerification,
                        canResend = canResendVerification,
                        onResend = { viewModel.resendEmailVerification() },
                        onDismiss = { emailBannerDismissed = true }
                    )
                }
            }

            item {
                NetworkStatusBanner(
                    isNetworkAvailable = uiState.isNetworkAvailable,
                    isSyncing = uiState.isSyncing
                )
            }

            // Échec du chargement alors que le réseau était là. N'apparaît que si
            // des textes sont malgré tout affichés : ils sont alors potentiellement
            // périmés, ce que la bannière signale. Sans contenu, c'est le bloc
            // posé à la place des sections qui porte le message.
            uiState.homeDataError?.takeIf { hasDocumentContent }?.let { error ->
                item {
                    MibekoErrorBanner(
                        offline = error.offline,
                        onRetry = error.retry,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Accès secondaire : la recherche documentaire (sans IA) vit dans la Bibliothèque.
            item {
                BrowseLibraryCard(onClick = {
                    // Même chemin que l'onglet Bibliothèque : deux jeux
                    // d'options différents pour une même destination
                    // désynchronisaient la pile sauvegardée.
                    navController.switchTopLevelTab(Screen.Library)
                })
            }

            // Corps documentaire vide : ces sections étant toutes conditionnées par
            // `isNotEmpty()`, un chargement en cours ou échoué les faisait
            // simplement disparaître — l'accueil se réduisait alors au hero et à
            // une carte, séparés par un grand blanc que rien n'expliquait. On
            // occupe la place, soit par des squelettes, soit par un état dit.
            if (!hasDocumentContent) {
                item {
                    if (uiState.isLoading) {
                        HomeDocumentSkeleton()
                    } else {
                        HomeContentUnavailable(
                            offline = !uiState.isNetworkAvailable,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
            }

            // Accueil documentaire : les données sont chargées depuis longtemps mais
            // n'étaient jamais rendues — le corpus (différenciateur réel de l'app)
            // restait invisible derrière le hero IA.
            if (uiState.popularCodes.isNotEmpty()) {
                item {
                    HomeDocumentSection(
                        title = "Codes populaires",
                        documents = uiState.popularCodes,
                        onOpenDocument = { navController.navigate(Screen.DocumentDetail(it)) }
                    )
                }
            }

            if (uiState.recentlyAdded.isNotEmpty()) {
                item {
                    HomeDocumentSection(
                        title = "Récemment ajoutés",
                        documents = uiState.recentlyAdded,
                        onOpenDocument = { navController.navigate(Screen.DocumentDetail(it)) }
                    )
                }
            }

            // Journal Officiel — le flux d'actualité légale.
            if (uiState.officialJournals.isNotEmpty()) {
                item {
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
                                text = "Voir tout",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(uiState.officialJournals.take(6)) { journal ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        JournalRow(
                            title = journal.title,
                            date = formatIsoDate(journal.publication_date),
                            onClick = { navController.navigate(Screen.OfficialJournalDetail(journal.id)) }
                        )
                    }
                }
            }
        }
        }
    }
}

/** En-tête compact : identité discrète + raccourcis historique / notifications. */
@Composable
private fun HomeTopBar(
    onHistoryClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(36.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "Mibeko",
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Mibeko",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "Historique des conversations",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Champ assistant « hero » : une ligne, envoi direct ; au focus, les
 * suggestions s'affichent pour guider la première question. La conversation
 * s'ouvre en plein écran (pas de bottom sheet : clavier + streaming).
 */
@Composable
private fun AssistantHero(
    suggestions: List<String>,
    onAsk: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Bonjour 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Posez votre question juridique à l'assistant IA de Mibeko, spécialisé en droit congolais et OHADA.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 1000) text = it },
                    placeholder = {
                        Text(
                            "Rechercher ou poser une question...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    maxLines = 4,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onAsk(text) })
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { onAsk(text) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(suggestions) {
                    suggestion ->
                    Surface(
                        onClick = { onAsk(suggestion) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.width(200.dp).heightIn(min = 80.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Entrée claire vers la recherche documentaire (sans IA) de la Bibliothèque. */
@Composable
private fun BrowseLibraryCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Parcourir les textes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Codes, lois, décrets — recherche plein-texte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Section documentaire horizontale : « Codes populaires » / « Récemment ajoutés ». */
@Composable
private fun HomeDocumentSkeleton() {
    Column {
        repeat(2) { section ->
            SkeletonBlock(
                width = if (section == 0) 172.dp else 148.dp,
                height = 26.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                repeat(3) {
                    SkeletonBlock(width = 160.dp, height = 108.dp, radius = 16.dp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** Aplat neutre tenant la place d'un contenu encore inconnu. */
@Composable
private fun SkeletonBlock(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/**
 * Corps documentaire indisponible. Ne dit jamais que le corpus est vide — il
 * dit ce qui s'est passé (règle produit n° 1) et propose de réessayer.
 */
@Composable
private fun HomeContentUnavailable(offline: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (offline) Icons.Default.CloudOff else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (offline) {
                "Les textes en avant ne sont pas disponibles hors-ligne."
            } else {
                "Je n'ai pas pu charger les textes en avant."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "La bibliothèque reste accessible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!offline) {
            TextButton(onClick = onRetry) { Text("Réessayer") }
        }
    }
}

@Composable
private fun HomeDocumentSection(
    title: String,
    documents: List<RemoteDocument>,
    onOpenDocument: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(documents, key = { it.id }) { document ->
                HomeDocumentCard(
                    document = document,
                    onClick = { onOpenDocument(document.id) }
                )
            }
        }
    }
}

/** Carte compacte d'un texte, pour les rangées horizontales de l'accueil. */
@Composable
private fun HomeDocumentCard(document: RemoteDocument, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp).heightIn(min = 108.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Objet dérivé du corps de l'acte, SOUS le titre et jamais à sa
                // place : sur un acte en abrégé, le titre ci-dessus ne dit que
                // le type, le numéro et la date.
                document.libelle_descriptif?.let { objet ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = objet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                document.type?.name?.let { typeName ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Une parution du Journal Officiel dans le flux d'accueil. */
@Composable
private fun JournalRow(title: String, date: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Publié le $date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
