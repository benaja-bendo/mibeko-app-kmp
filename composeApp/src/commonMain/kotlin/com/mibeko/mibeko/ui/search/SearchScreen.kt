package com.mibeko.mibeko.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.text.style.TextOverflow
import com.mibeko.mibeko.ui.theme.*
import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import com.mibeko.mibeko.ui.reader.ReaderScreen
import com.mibeko.mibeko.ui.components.HighlightedText
import com.mibeko.mibeko.ui.components.SearchResultsShimmer
import com.mibeko.mibeko.util.copyToClipboard
import com.mibeko.mibeko.util.shareText

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

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
        val suggestions by viewModel.suggestions.collectAsState()
        val recentSearches by viewModel.recentSearches.collectAsState()
        
        var searchText by remember { mutableStateOf(query ?: tag ?: "") }
        var isActive by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(query, tag) {
            if (tag != null) {
                viewModel.performSearchByTag(tag)
            } else if (query != null) {
                viewModel.updateQuery(query)
                viewModel.saveSearch(query)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = if (isActive) 0.dp else 16.dp, vertical = if (isActive) 0.dp else 8.dp)
                ) {
                    @Suppress("DEPRECATION")
                    SearchBar(
                        query = searchText,
                        onQueryChange = { 
                            searchText = it
                            viewModel.updateLiveQuery(it)
                        },
                        onSearch = {
                            isActive = false
                            viewModel.performSearch(it)
                            viewModel.saveSearch(it)
                        },
                        active = isActive,
                        onActiveChange = { isActive = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Rechercher un article, une loi...") },
                        leadingIcon = {
                            IconButton(onClick = { 
                                if (isActive) isActive = false else navController.popBackStack() 
                            }) {
                                Icon(
                                    if (isActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Search,
                                    contentDescription = null
                                )
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchText.isNotEmpty() && isActive) {
                                    IconButton(onClick = { searchText = ""; viewModel.updateLiveQuery("") }) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Effacer")
                                    }
                                }
                                
                                // Indicateur de statut réseau intégré
                                if (uiState.errorMessage != null) {
                                    IconButton(onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Mode hors-ligne actif.")
                                        }
                                    }) {
                                        Icon(
                                            Icons.Filled.CloudOff,
                                            contentDescription = "Hors-ligne",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        // Overlay de suggestions et recherches récentes
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            if (searchText.isNotBlank() && suggestions.isNotEmpty()) {
                                item {
                                    Text(
                                        "Suggestions",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                items(suggestions.take(5)) { suggestion ->
                                    ListItem(
                                        headlineContent = { Text("Article ${suggestion.number}") },
                                        supportingContent = { Text(suggestion.breadcrumb) },
                                        leadingContent = { 
                                            Icon(
                                                if (suggestion.isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Cloud,
                                                contentDescription = null,
                                                tint = if (suggestion.isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            searchText = suggestion.number
                                            isActive = false
                                            navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Reader(suggestion.id))
                                        }
                                    )
                                }
                            }

                            if (recentSearches.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Recherches récentes",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                            Text("Effacer", fontSize = 12.sp)
                                        }
                                    }
                                }
                                items(recentSearches) { query ->
                                    ListItem(
                                        headlineContent = { Text(query) },
                                        leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                                        trailingContent = {
                                            IconButton(onClick = { viewModel.removeFromHistory(query) }) {
                                                Icon(Icons.Default.Close, contentDescription = "Supprimer")
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            searchText = query
                                            isActive = false
                                            viewModel.performSearch(query)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                // L'indicateur réseau est maintenant dans la SearchBar
                
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
                            count = uiState.counts["Tout"] ?: 0,
                            onClick = { viewModel.updateFilter("Tout") }
                        )
                    }
                    
                    items(uiState.documentTypes) { type ->
                        val count = uiState.counts[type.code] ?: 0
                        if (count > 0) {
                            FilterChipItem(
                                selected = uiState.currentFilter == type.code,
                                label = type.name,
                                count = count,
                                onClick = { viewModel.updateFilter(type.code) }
                            )
                        }
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
                                EmptyResultsState(query = searchText)
                            }
                        } else {
                            items(uiState.results) { article ->
                                SearchResultCard(
                                    articleId = article.id,
                                    articleNumber = article.number,
                                    source = article.breadcrumb,
                                    snippet = article.content ?: "",
                                    query = searchText,
                                    typeCode = article.typeCode,
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
}

/**
 * Carte affichant la réponse générée par l'IA (RAG).
 * Design premium avec dégradé subtil et actions rapides.
 */
@Composable
private fun AiAnswerCard(answer: String) {
    var isExpanded by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current // Assuming we provide it or use Local

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
                    .clickable { isExpanded = !isExpanded }
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
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            copyToClipboard(answer)
                            // We can't easily show snackbar here without access to scaffold state
                            // but we can try to find it or just perform the action
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, null, tint = MibekoBluePrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { 
                            shareText(answer)
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, null, tint = MibekoBluePrimary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Réduire" else "Développer",
                                tint = MibekoBluePrimary
                            )
                        }
                    }
                }
            }

            // Answer Content
            AnimatedVisibility(visible = isExpanded) {
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cette analyse est générée par IA. Veuillez vérifier avec les textes officiels ci-dessous.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MibekoGold.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
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
    count: Int,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f) 
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                                else MaterialTheme.colorScheme.onSurfaceVariant
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
    articleId: String,
    articleNumber: String,
    source: String,
    snippet: String,
    query: String,
    typeCode: String,
    isDownloaded: Boolean,
    isFavorite: Boolean = false,
    onClick: () -> Unit
) {
    val viewModel = koinViewModel<SearchViewModel>()
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
                    val isCode = typeCode == "CODE" || typeCode == "LOI_ORG"
                    
                    // Type Badge
                    Surface(
                        color = if (isCode) MibekoBluePrimary.copy(alpha = 0.1f) else MibekoGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = typeCode,
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
                    IconButton(
                        onClick = { viewModel.toggleFavorite(articleId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) MibekoGold else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
