package com.mibeko.mibeko.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.ui.text.style.TextOverflow
import com.mibeko.mibeko.ui.theme.*
import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import com.mibeko.mibeko.ui.reader.ReaderScreen
import com.mibeko.mibeko.ui.components.HighlightedText
import com.mibeko.mibeko.ui.components.SearchResultsShimmer

data class SearchResultsScreen(val query: String? = null, val tag: String? = null) : Screen {
    
    /**
     * Contenu principal de la page de résultats de recherche.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<SearchViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(query, tag) {
            if (tag != null) {
                viewModel.performSearchByTag(tag)
            } else if (query != null) {
                viewModel.updateQuery(query)
                viewModel.saveSearch(query)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Résultats de recherche",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (tag != null) "Thématique : $tag" else "\"$query\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                // Network status indicator
                NetworkStatusBanner(
                    isFromNetwork = uiState.isFromNetwork,
                    errorMessage = uiState.errorMessage,
                    onRetry = { viewModel.retrySearch() }
                )

                // Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipItem(
                            selected = uiState.currentFilter == "Tout",
                            label = "Tout",
                            count = if (uiState.currentFilter == "Tout") uiState.results.size else null,
                            onClick = { viewModel.updateFilter("Tout") }
                        )
                    }
                    item {
                        FilterChipItem(
                            selected = uiState.currentFilter == "Codes",
                            label = "Codes",
                            onClick = { viewModel.updateFilter("Codes") }
                        )
                    }
                    item {
                        FilterChipItem(
                            selected = uiState.currentFilter == "Lois",
                            label = "Lois",
                            onClick = { viewModel.updateFilter("Lois") }
                        )
                    }
                }

                // Loading with shimmer
                AnimatedVisibility(
                    visible = uiState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SearchResultsShimmer(
                            itemCount = 4,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Results count badge
                if (!uiState.isLoading && uiState.results.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${uiState.results.size} résultat${if (uiState.results.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (!uiState.isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // AI Answer section
                        uiState.aiAnswer?.let { answer ->
                            item {
                                AiAnswerCard(answer = answer)
                            }
                            
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = "SOURCES JURIDIQUES",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }

                        if (uiState.results.isEmpty()) {
                            item {
                                EmptyResultsState(query = query ?: tag ?: "")
                            }
                        } else {
                            items(uiState.results) { article ->
                                SearchResultCard(
                                    articleNumber = article.number,
                                    source = article.breadcrumb,
                                    snippet = article.content ?: "",
                                    query = query ?: "",
                                    isDownloaded = article.isDownloaded,
                                    isFavorite = article.isFavorite,
                                    onClick = { navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Reader(article.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Carte affichant la réponse générée par l'IA (RAG).
 * Design premium avec dégradé subtil et actions rapides.
 */
@Composable
private fun AiAnswerCard(answer: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MibekoBluePrimary.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Column {
            // Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MibekoBluePrimary.copy(alpha = 0.05f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MibekoBluePrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Assistant Juridique",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MibekoBluePrimary
                            )
                            Text(
                                text = "Analyse Mibeko AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MibekoBluePrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    Row {
                        IconButton(onClick = { /* Copy */ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, null, tint = MibekoBluePrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { /* Share */ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, null, tint = MibekoBluePrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Answer Content
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Disclaimer with Icon
                Surface(
                    color = MibekoGold.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MibekoGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vérifiez toujours les sources officielles ci-dessous.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MibekoGoldDark,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bannière indiquant l'état du réseau ou les erreurs de recherche.
 * Design plus discret et moderne.
 */
@Composable
private fun NetworkStatusBanner(
    isFromNetwork: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    if (errorMessage != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mode hors-ligne : $errorMessage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRetry) {
                    Text("RÉESSAYER", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    } else if (isFromNetwork) {
        Surface(
            color = MibekoBluePrimary.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MibekoBluePrimary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = MibekoBluePrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Résultats synchronisés en temps réel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MibekoBluePrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Puce de filtre pour affiner les résultats de recherche.
 */
@Composable
private fun FilterChipItem(
    selected: Boolean,
    label: String,
    count: Int? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                if (count != null && selected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "($count)",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Carte affichant un article trouvé dans les résultats.
 * Design uniformisé avec hiérarchie claire.
 */
@Composable
private fun SearchResultCard(
    articleNumber: String,
    source: String,
    snippet: String,
    query: String,
    isDownloaded: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Context Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val isCode = source.contains("Code", ignoreCase = true)
                    
                    // Type Badge
                    Surface(
                        color = if (isCode) MibekoBluePrimary.copy(alpha = 0.1f) else MibekoGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isCode) "CODE" else "LOI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCode) MibekoBluePrimary else MibekoGoldDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Document Title (Breadcrumb)
                    Text(
                        text = source.split(">").last().trim(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloaded) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Disponible hors-ligne",
                            tint = LegalValid,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Favori",
                        tint = if (isFavorite) MibekoGold else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Article Number and Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Article $articleNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MibekoBluePrimary,
                        letterSpacing = 0.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Highlighted content
                    HighlightedText(
                        text = snippet.trim().take(180) + if (snippet.length > 180) "..." else "",
                        query = query,
                        maxLines = 3,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 12.dp)
                        .size(32.dp)
                        .background(MibekoBluePrimary.copy(alpha = 0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Voir l'article",
                        tint = MibekoBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Vue affichée lorsqu'aucun résultat n'est trouvé.
 * Design épuré avec suggestions pertinentes.
 */
@Composable
private fun EmptyResultsState(query: String) {
    val viewModel = koinViewModel<SearchViewModel>()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mibeko styled illustration
        Surface(
            color = MibekoBluePrimary.copy(alpha = 0.05f),
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.SearchOff,
                    contentDescription = null,
                    tint = MibekoBluePrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Aucun résultat trouvé",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Nous n'avons trouvé aucun texte de loi correspondant à \"$query\" dans la base de données Mibeko.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Essayez une thématique populaire :",
            style = MaterialTheme.typography.labelLarge,
            color = MibekoBluePrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Refined Suggestion chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Constitution", "Code du travail", "Bail commercial", "Droit Civil").forEach { suggestion ->
                SuggestionChip(
                    onClick = { viewModel.performSearch(suggestion) },
                    label = { 
                        Text(
                            text = suggestion,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) 
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MibekoBluePrimary.copy(alpha = 0.05f),
                        labelColor = MibekoBluePrimary
                    ),
                    border = BorderStroke(1.dp, MibekoBluePrimary.copy(alpha = 0.1f))
                )
            }
        }
    }
}

/**
 * Rangée de flux pour les éléments.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
