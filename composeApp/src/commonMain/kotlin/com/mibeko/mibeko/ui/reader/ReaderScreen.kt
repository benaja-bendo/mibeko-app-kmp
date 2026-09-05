package com.mibeko.mibeko.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Color
import com.mibeko.mibeko.ui.components.DossierSelectionSheet
import com.mibeko.mibeko.ui.components.LegalTableBlock
import com.mibeko.mibeko.ui.components.MibekoBreadcrumb
import com.mibeko.mibeko.ui.components.BreadcrumbSegment
import com.mibeko.mibeko.ui.components.ShareOptionsSheet
import com.mibeko.mibeko.ui.components.ReportErrorDialog
import com.mibeko.mibeko.ui.components.OfficialSourcesSheet
import com.mibeko.mibeko.util.ContentSegment
import com.mibeko.mibeko.util.articleLeafLabel
import com.mibeko.mibeko.util.articleSegments
import com.mibeko.mibeko.util.formatIsoDateToFrench
import com.mibeko.mibeko.util.readableText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(articleId: String) {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<ReaderViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val article = uiState.article
        val textSize by viewModel.textSize.collectAsState()
        val isDyslexiaFontEnabled by viewModel.isDyslexiaFontEnabled.collectAsState()
        val isSharing by viewModel.isSharing.collectAsState()
        val snackbarMessage by viewModel.snackbarMessage.collectAsState()
        
        val snackbarHostState = remember { SnackbarHostState() }

        // Thème de lecture persisté (préférences), indépendant du thème de l'app.
        val readerThemePref by viewModel.readerTheme.collectAsState()
        val readerTheme = when (readerThemePref) {
            UserPreferencesRepository.ReaderTheme.PAPER -> "paper"
            UserPreferencesRepository.ReaderTheme.LIGHT -> "white"
            UserPreferencesRepository.ReaderTheme.DARK -> "dark"
        }
        var showSettings by remember { mutableStateOf(false) }
        var showShareSheet by remember { mutableStateOf(false) }
        var showReportDialog by remember { mutableStateOf(false) }
        var showDossierSelection by remember { mutableStateOf(false) }
        var showToc by remember { mutableStateOf(false) }
        var showSources by remember { mutableStateOf(false) }

        val listState = rememberLazyListState()

        // Show snackbar when message changes
        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSnackbar()
            }
        }

        LaunchedEffect(articleId) {
            viewModel.loadArticle(articleId)
            viewModel.refreshPreferences()
        }

        // Revenir en haut quand on passe à un autre article (suivant/précédent).
        LaunchedEffect(uiState.article?.id) {
            listState.scrollToItem(0)
        }

        // Apply colors based on reader theme
        val backgroundColor = when(readerTheme) {
            "paper" -> Color(0xFFF4ECD8)
            "dark" -> Color(0xFF121212)
            else -> Color.White
        }
        
        val textColor = when(readerTheme) {
            "dark" -> Color(0xFFE0E0E0)
            else -> Color(0xFF1A1A1A)
        }

        // Apply text size multiplier
        val fontSizeValue = when(textSize) {
            UserPreferencesRepository.TextSize.SMALL -> 15.sp
            UserPreferencesRepository.TextSize.MEDIUM -> 18.sp
            UserPreferencesRepository.TextSize.LARGE -> 22.sp
        }
        
        val lineSpacingValue = when(textSize) {
            UserPreferencesRepository.TextSize.SMALL -> 26.sp
            UserPreferencesRepository.TextSize.MEDIUM -> 32.sp
            UserPreferencesRepository.TextSize.LARGE -> 38.sp
        }

        // Show loading state
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Chargement de l'article...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
            return
        }

        // Show error state if article not found
        if (uiState.error != null || article == null) {
            Box(modifier = Modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        uiState.error ?: "Article non trouvé",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Retour")
                    }
                }
            }
            return
        }

        val currentArticle = article

        // Deux origines possibles pour un tableau : la structure synchronisée
        // depuis l'API (cas normal), ou — pour un corpus téléchargé avant la
        // normalisation — du HTML MinerU resté dans le texte. La segmentation
        // absorbe les deux, et aucune balise n'arrive à l'écran.
        val segments = remember(currentArticle.id, currentArticle.content, currentArticle.tables) {
            articleSegments(currentArticle.content, currentArticle.tables)
        }

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier.background(backgroundColor)
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    // Certaines feuilles portent un numéro
                                    // technique (« TABLEAU_1 », « PREAMBULE ») :
                                    // « Article TABLEAU_1 » n'est pas un titre.
                                    articleLeafLabel(currentArticle.number),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = textColor)
                            }
                        },
                        actions = {
                            // Offline Toggle
                            IconButton(onClick = { viewModel.toggleOffline() }) {
                                Icon(
                                    if (currentArticle.isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                    contentDescription = "Hors-ligne",
                                    tint = if (currentArticle.isDownloaded) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                                )
                            }
                            // Favorite Toggle
                            IconButton(onClick = { showDossierSelection = true }) {
                                Icon(
                                    if (currentArticle.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Ajouter à un dossier",
                                    tint = if (currentArticle.isFavorite) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                                )
                            }
                            // Share - Single button that opens share options sheet
                            IconButton(
                                onClick = { showShareSheet = true },
                                enabled = !isSharing
                            ) {
                                if (isSharing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = textColor)
                                }
                            }
                            
                            // Settings
                            IconButton(onClick = { showSettings = true }) { 
                                Icon(Icons.Default.FormatSize, contentDescription = "Paramètres de lecture", tint = textColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    
                    // Simplified breadcrumb with document info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Document type badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when(readerTheme) {
                                "dark" -> Color(0xFF2D2D2D)
                                else -> textColor.copy(alpha = 0.08f)
                            }
                        ) {
                            Text(
                                text = uiState.documentType?.uppercase() ?: "LOI",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Document title
                        Text(
                            text = uiState.documentTitle ?: "Document",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    HorizontalDivider(color = textColor.copy(alpha = 0.08f))
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = backgroundColor,
            bottomBar = {
                // Navigation séquentielle : précédent / position (ouvre le
                // sommaire) / suivant — même logique que le lecteur web.
                if (uiState.articleSequence.size > 1) {
                    ArticleNavigationBar(
                        currentIndex = uiState.currentIndex,
                        total = uiState.articleSequence.size,
                        textColor = textColor,
                        backgroundColor = backgroundColor,
                        onPrevious = { uiState.previousArticleId?.let { viewModel.loadArticle(it) } },
                        onNext = { uiState.nextArticleId?.let { viewModel.loadArticle(it) } },
                        onOpenToc = { showToc = true },
                        hasPrevious = uiState.previousArticleId != null,
                        hasNext = uiState.nextArticleId != null
                    )
                }
            }
        ) { padding ->
            // Swipe horizontal : passer à l'article suivant / précédent.
            var dragTotal by remember { mutableStateOf(0f) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(uiState.currentIndex, uiState.articleSequence.size) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onDragEnd = {
                                when {
                                    dragTotal < -150f -> uiState.nextArticleId?.let { viewModel.loadArticle(it) }
                                    dragTotal > 150f -> uiState.previousArticleId?.let { viewModel.loadArticle(it) }
                                }
                                dragTotal = 0f
                            },
                            onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount }
                        )
                    },
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Article title in caps
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            // Le fil d'Ariane porte le titre du nœud parent. Il
                            // est vide sur les actes courts, dont les articles
                            // pendent d'un nœud racine synthétique : le libellé
                            // de la feuille prend alors le relais.
                            text = currentArticle.breadcrumb.split(">").last().trim()
                                .ifBlank { articleLeafLabel(currentArticle.number) }
                                .uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                }
                
                // Article content with optimized typography
                segments.forEachIndexed { index, segment ->
                    item(key = "segment_${currentArticle.id}_$index") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            when (segment) {
                                // SelectionContainer : citer un article suppose de
                                // pouvoir en sélectionner le texte (mibeko-app-kmp#9).
                                is ContentSegment.Text -> SelectionContainer {
                                    Text(
                                        text = readableText(segment.text),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = fontSizeValue,
                                            lineHeight = lineSpacingValue,
                                            letterSpacing = if (isDyslexiaFontEnabled) 0.8.sp else 0.3.sp,
                                            fontWeight = if (isDyslexiaFontEnabled) FontWeight.Medium else FontWeight.Normal
                                        ),
                                        color = textColor
                                    )
                                }

                                is ContentSegment.Table -> LegalTableBlock(
                                    table = segment.table,
                                    textColor = textColor,
                                    fontSize = fontSizeValue,
                                    lineHeight = lineSpacingValue,
                                    dyslexiaFriendly = isDyslexiaFontEnabled
                                )
                            }
                        }
                    }
                }

                // Provenance : d'où vient ce texte et de quand date la copie
                // consultée. Sur un corpus hors-ligne, l'utilisateur doit
                // pouvoir juger la fraîcheur de ce qu'il lit.
                item {
                    ProvenanceBanner(
                        consolidationAsOf = uiState.consolidationAsOf,
                        localVersionDate = uiState.localVersionDate,
                        textColor = textColor
                    )
                }

                item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 48.dp),
                            color = textColor.copy(alpha = 0.1f)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Pont vers l'assistant : l'article est épinglé en
                            // référence, la question reste celle de
                            // l'utilisateur (posture assistive, pas de prompt
                            // induit).
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(
                                        com.mibeko.mibeko.ui.navigation.Screen.Chat(
                                            pinnedDocumentId = currentArticle.codeId,
                                            pinnedLabel = uiState.documentTitle
                                        )
                                    )
                                },
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyser avec l'assistant")
                            }
                            OutlinedButton(
                                onClick = { showSources = true },
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Source officielle")
                            }
                            OutlinedButton(
                                onClick = { showReportDialog = true },
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signaler une erreur")
                            }
                        }
                    }
                }
            }

        if (showToc) {
            ModalBottomSheet(
                onDismissRequest = { showToc = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReaderTocSheet(
                    entries = uiState.articleSequence,
                    currentArticleId = uiState.article?.id,
                    onSelect = { id ->
                        showToc = false
                        viewModel.loadArticle(id)
                    }
                )
            }
        }

        if (showReportDialog) {
            ReportErrorDialog(
                onDismiss = { showReportDialog = false },
                onSubmit = { type, desc ->
                    viewModel.reportError(type, desc)
                }
            )
        }

        if (showSources) {
            OfficialSourcesSheet(onDismiss = { showSources = false })
        }

        if (showDossierSelection) {
            DossierSelectionSheet(
                articleId = currentArticle.id,
                onDismiss = { 
                    showDossierSelection = false
                    viewModel.loadArticle(currentArticle.id) // Reload to get updated isFavorite status
                }
            )
        }

        if (showSettings) {
             ModalBottomSheet(
                     onDismissRequest = { showSettings = false },
                     containerColor = MaterialTheme.colorScheme.surface
                 ) {
                     ReaderSettingsSheet(
                        currentTextSize = textSize,
                        onTextSizeChange = { viewModel.setTextSize(it) },
                        currentTheme = readerTheme,
                        onThemeChange = { selected ->
                            viewModel.setReaderTheme(
                                when (selected) {
                                    "white" -> UserPreferencesRepository.ReaderTheme.LIGHT
                                    "dark" -> UserPreferencesRepository.ReaderTheme.DARK
                                    else -> UserPreferencesRepository.ReaderTheme.PAPER
                                }
                            )
                        },
                        isDyslexiaEnabled = isDyslexiaFontEnabled,
                        onDyslexiaChange = { viewModel.setDyslexiaFontEnabled(it) }
                     )
                 }
            }

            if (showShareSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showShareSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                ShareOptionsSheet(
                    title = "Partager l'article",
                    subtitle = "${articleLeafLabel(currentArticle.number)} - ${uiState.documentTitle}",
                    onSharePdf = {
                        viewModel.shareAsPdf()
                        showShareSheet = false
                    },
                    onShareText = {
                        viewModel.shareAsText()
                        showShareSheet = false
                    },
                    // Sans slug public, le lien retomberait sur l'accueil
                    // mibeko.fr : on masque l'option plutôt que de mentir.
                    onShareLink = if (uiState.documentSlug != null) {
                        {
                            viewModel.shareAsLink()
                            showShareSheet = false
                        }
                    } else null,
                    onCopy = {
                        viewModel.copyArticleText()
                        showShareSheet = false
                    },
                    isLoading = isSharing
                )
                }
            }
        }
 

/**
 * Barre de navigation séquentielle du lecteur : article précédent / position
 * dans le document (ouvre le sommaire) / article suivant.
 */
@Composable
private fun ArticleNavigationBar(
    currentIndex: Int,
    total: Int,
    textColor: Color,
    backgroundColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenToc: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean
) {
    Surface(color = backgroundColor) {
        Column {
            HorizontalDivider(color = textColor.copy(alpha = 0.08f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrevious, enabled = hasPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = if (hasPrevious) textColor else textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Précédent",
                        color = if (hasPrevious) textColor else textColor.copy(alpha = 0.3f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Position courante : un appui ouvre le sommaire du document.
                Surface(
                    onClick = onOpenToc,
                    shape = RoundedCornerShape(8.dp),
                    color = textColor.copy(alpha = 0.06f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Sommaire",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${currentIndex + 1} / $total",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }

                TextButton(onClick = onNext, enabled = hasNext) {
                    Text(
                        "Suivant",
                        color = if (hasNext) textColor else textColor.copy(alpha = 0.3f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (hasNext) textColor else textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Sommaire du document : articles groupés par section, article courant marqué. */
@Composable
private fun ReaderTocSheet(
    entries: List<ReaderTocEntry>,
    currentArticleId: String?,
    onSelect: (String) -> Unit
) {
    val rows = remember(entries) { buildTocRows(entries) }
    // Ouvrir sur l'article courant plutôt qu'en haut (mibeko-app-kmp#9) :
    // à l'article 187/700, il fallait sinon défiler depuis le début.
    val currentIndex = remember(rows, currentArticleId) {
        rows.indexOfFirst { it is TocRow.ArticleRow && it.entry.id == currentArticleId }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = "Sommaire",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        LazyColumn(state = listState, modifier = Modifier.heightIn(max = 480.dp)) {
            items(
                rows,
                key = { row ->
                    when (row) {
                        is TocRow.Header -> "node_${row.nodeTitle}_${row.firstEntryId}"
                        is TocRow.ArticleRow -> row.entry.id
                    }
                }
            ) { row ->
                when (row) {
                    is TocRow.Header -> Text(
                        text = row.nodeTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    is TocRow.ArticleRow -> {
                        val entry = row.entry
                        val isCurrent = entry.id == currentArticleId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(entry.id) }
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 32.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = articleLeafLabel(entry.number),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Article affiché",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderSettingsSheet(
    currentTextSize: UserPreferencesRepository.TextSize,
    onTextSizeChange: (UserPreferencesRepository.TextSize) -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    isDyslexiaEnabled: Boolean,
    onDyslexiaChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Paramètres de lecture", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        
        // Text Size
        Column {
             Text("Taille du texte", fontWeight = FontWeight.Medium)
             Spacer(modifier = Modifier.height(8.dp))
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Text("Petit", fontSize = 14.sp)
                 androidx.compose.material3.Slider(
                     value = when(currentTextSize) {
                         UserPreferencesRepository.TextSize.SMALL -> 0f
                         UserPreferencesRepository.TextSize.MEDIUM -> 0.5f
                         UserPreferencesRepository.TextSize.LARGE -> 1f
                     },
                     onValueChange = { 
                         val newSize = when {
                             it < 0.25f -> UserPreferencesRepository.TextSize.SMALL
                             it > 0.75f -> UserPreferencesRepository.TextSize.LARGE
                             else -> UserPreferencesRepository.TextSize.MEDIUM
                         }
                         onTextSizeChange(newSize)
                     },
                     steps = 1,
                     modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                 )
                 Text("Grand", fontSize = 20.sp)
             }
        }
        
        // Theme
        Column {
            Text("Thème", fontWeight = FontWeight.Medium)
             Spacer(modifier = Modifier.height(8.dp))
             Row(
                 horizontalArrangement = Arrangement.spacedBy(16.dp)
             ) {
                 ThemeOption(
                     name = "Papier",
                     color = Color(0xFFF4ECD8),
                     isSelected = currentTheme == "paper",
                     onClick = { onThemeChange("paper") }
                 )
                 ThemeOption(
                     name = "Clair",
                     color = Color.White,
                     isSelected = currentTheme == "white",
                     onClick = { onThemeChange("white") }
                 )
                 ThemeOption(
                     name = "Sombre",
                     color = Color(0xFF121212),
                     selectionColor = Color.White,
                     isSelected = currentTheme == "dark",
                     onClick = { onThemeChange("dark") }
                 )
             }
        }
        
        // Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Police Dyslexie")
            Switch(
                checked = isDyslexiaEnabled,
                onCheckedChange = onDyslexiaChange
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ThemeOption(
    name: String,
    color: Color,
    isSelected: Boolean,
    selectionColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.background(color).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier.background(color).border(1.dp, Color.Gray, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 12.sp)
    }


}

/**
 * Bandeau de provenance affiché sous le texte.
 *
 * Un corpus consulté hors-ligne peut dater : afficher côte à côte le « à jour
 * au » officiel et la date de la copie locale permet à l'utilisateur — un
 * avocat, souvent — de juger lui-même s'il peut s'appuyer sur ce qu'il lit.
 */
@Composable
private fun ProvenanceBanner(
    consolidationAsOf: String?,
    localVersionDate: String?,
    textColor: Color
) {
    if (consolidationAsOf == null && localVersionDate == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        HorizontalDivider(color = textColor.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(12.dp))
        if (consolidationAsOf != null) {
            Text(
                text = "Texte consolidé à jour au ${formatIsoDateToFrench(consolidationAsOf)}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
        if (localVersionDate != null) {
            Text(
                text = "Copie locale du $localVersionDate",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.45f)
            )
        }
    }
}
