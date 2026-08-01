package com.mibeko.mibeko.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.preferences.RecentlyViewedItem
import com.mibeko.mibeko.data.remote.LibraryHomeDocument
import com.mibeko.mibeko.data.remote.LibrarySearchItem
import com.mibeko.mibeko.data.remote.LibrarySuggestions
import com.mibeko.mibeko.ui.components.MibekoErrorBanner
import com.mibeko.mibeko.ui.components.MibekoErrorState
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.util.formatRemoteDateForUi
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bibliothèque juridique — déclinaison mobile du poste de travail web :
 * recherche d'abord (plein-texte serveur, autocomplétion), accueil vivant
 * (textes fondamentaux, derniers textes, reprise de lecture, stats du fonds),
 * IA jamais imposée. Hors-ligne : contenus téléchargés + recherche locale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<LibraryViewModel>()

    val state by viewModel.uiState.collectAsState()
    val recentItems by viewModel.recentItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LibraryHeader(
                state = state,
                onSearchChange = viewModel::updateSearchQuery,
                onSearchSubmit = { viewModel.submitSearch() },
                onClearSearch = viewModel::clearSearch,
                onScopeChange = viewModel::updateScope,
                onOpenFilters = { showFilterSheet = true }
            )

            // Autocomplétion : affichée par-dessus le contenu tant qu'on tape.
            state.suggestions?.let { suggestions ->
                SuggestionsPanel(
                    suggestions = suggestions,
                    onOpenDocument = { documentId ->
                        viewModel.dismissSuggestions()
                        navController.navigate(Screen.DocumentDetail(documentId))
                    },
                    onOpenArticle = { articleId ->
                        viewModel.dismissSuggestions()
                        navController.navigate(Screen.Reader(articleId))
                    }
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refresh() },
                state = rememberPullToRefreshState(),
                modifier = Modifier.fillMaxSize()
            ) {
                if (!state.hasSearched) {
                    LibraryHomeContent(
                        state = state,
                        recentItems = recentItems,
                        onSuggestionClick = { viewModel.submitSearch(it) },
                        onOpenDocument = { navController.navigate(Screen.DocumentDetail(it)) },
                        onOpenArticle = { navController.navigate(Screen.Reader(it)) }
                    )
                } else {
                    LibraryResultsContent(
                        state = state,
                        onOpenArticle = { navController.navigate(Screen.Reader(it)) },
                        onOpenDocument = { navController.navigate(Screen.DocumentDetail(it)) },
                        onLoadMore = viewModel::loadNextPage
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LibraryFilterSheet(
                state = state,
                onTypeChange = viewModel::updateTypeFilter,
                onInstitutionChange = viewModel::updateInstitutionFilter,
                onSortChange = viewModel::updateSort,
                onReset = {
                    viewModel.resetFilters()
                    showFilterSheet = false
                },
                onClose = { showFilterSheet = false }
            )
        }
    }
}

// =============================================================================
// En-tête : titre, recherche, périmètre, filtres
// =============================================================================

@Composable
private fun LibraryHeader(
    state: LibraryUiState,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onScopeChange: (LibraryScope) -> Unit,
    onOpenFilters: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bibliothèque",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.isOffline) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hors-ligne",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recherche plein-texte (notion, « article 45 code du travail », titre…)
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher une loi, un article, une notion…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Effacer la recherche",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearchSubmit() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Périmètre juridique (parité web) + accès aux filtres avancés
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LibraryScope.entries.toList()) { scope ->
                        FilterChip(
                            selected = state.scope == scope,
                            onClick = { onScopeChange(scope) },
                            label = { Text(scope.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                BadgedBox(
                    badge = {
                        if (state.activeFilterCount > 0) {
                            Badge { Text("${state.activeFilterCount}") }
                        }
                    }
                ) {
                    IconButton(onClick = onOpenFilters) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filtres",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// =============================================================================
// Autocomplétion (documents, articles, passages)
// =============================================================================

@Composable
private fun SuggestionsPanel(
    suggestions: LibrarySuggestions,
    onOpenDocument: (String) -> Unit,
    onOpenArticle: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            suggestions.documents.take(3).forEach { doc ->
                SuggestionRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = doc.title,
                    subtitle = doc.type_name,
                    onClick = { onOpenDocument(doc.id) }
                )
            }
            suggestions.articles.take(3).forEach { article ->
                SuggestionRow(
                    icon = Icons.Default.Article,
                    title = "Art. ${article.number} — ${article.document_title}",
                    subtitle = null,
                    onClick = { onOpenArticle(article.id) }
                )
            }
            suggestions.passages.take(2).forEach { passage ->
                SuggestionRow(
                    icon = Icons.Default.FormatQuote,
                    title = passage.snippet.replace("[[", "").replace("]]", ""),
                    subtitle = "Art. ${passage.number} — ${passage.document_title}",
                    onClick = { onOpenArticle(passage.id) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =============================================================================
// Accueil vivant (avant recherche)
// =============================================================================

@Composable
private fun LibraryHomeContent(
    state: LibraryUiState,
    recentItems: List<RecentlyViewedItem>,
    onSuggestionClick: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenArticle: (String) -> Unit
) {
    val home = state.home
    // Hors-ligne : l'accueil retombe sur les documents en base locale.
    val essentials: List<LibraryHomeDocument> = home?.essential_documents
        ?: state.localCodes
            .filter { it.type.contains("Code", ignoreCase = true) }
            .map { LibraryHomeDocument(id = it.id, title = it.title, type_name = it.type) }
    val recents: List<LibraryHomeDocument> = home?.recent_documents
        ?: state.localCodes.take(10)
            .map {
                LibraryHomeDocument(
                    id = it.id,
                    title = it.title,
                    type_name = it.type,
                    date_publication = it.dateSignature
                )
            }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Statistiques du fonds : ce que la bibliothèque interroge réellement.
        if (home != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(Icons.Default.Description, "Textes", home.stats.documents, Modifier.weight(1f))
                    StatCard(Icons.Default.Article, "Articles", home.stats.articles, Modifier.weight(1f))
                    StatCard(Icons.Default.AccountBalance, "Institutions", home.stats.institutions, Modifier.weight(1f))
                }
            }
        }

        // Suggestions de recherche cliquables
        if (home != null && home.suggestions.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(home.suggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { onSuggestionClick(suggestion) },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Textes fondamentaux (constitution, codes majeurs)
        if (essentials.isNotEmpty()) {
            item {
                LibrarySectionTitle("Textes fondamentaux")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(essentials) { doc ->
                        EssentialCard(doc = doc, onClick = { onOpenDocument(doc.id) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Reprise de lecture (réelle : derniers contenus consultés)
        if (recentItems.isNotEmpty()) {
            item {
                LibrarySectionTitle("Continuer la lecture")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentItems) { item ->
                        RecentReadingCard(item = item, onClick = { onOpenArticle(item.id) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Derniers textes publiés
        if (recents.isNotEmpty()) {
            item { LibrarySectionTitle("Derniers textes publiés") }
            items(recents) { doc ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    RecentDocumentCard(doc = doc, onClick = { onOpenDocument(doc.id) })
                }
            }
        }

        if (state.isLoading && home == null && state.localCodes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (!state.isLoading && home == null && state.localCodes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucun contenu disponible hors-ligne.\nConnectez-vous pour explorer la base nationale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatCompactCount(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/** 14532 → « 14,5 k » : compteur compact pour les cartes de stats. */
private fun formatCompactCount(value: Int): String {
    return if (value >= 1000) {
        val rounded = (value / 100).toFloat() / 10f
        val text = if (rounded % 1f == 0f) "${rounded.toInt()}" else "$rounded".replace('.', ',')
        "$text k"
    } else {
        "$value"
    }
}

@Composable
private fun LibrarySectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun EssentialCard(doc: LibraryHomeDocument, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            TypeBadge(doc.type_name ?: doc.type_code ?: "Texte")
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = doc.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (doc.articles_count != null && doc.articles_count > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${doc.articles_count} articles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentReadingCard(item: RecentlyViewedItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(240.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
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
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.typeCode.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentDocumentCard(doc: LibraryHomeDocument, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeBadge(doc.type_name ?: doc.type_code ?: "Texte")
                val date = formatRemoteDateForUi(doc.date_publication)
                if (date != null) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = doc.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypeBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// =============================================================================
// Résultats de recherche (granularité article, paginés)
// =============================================================================

@Composable
private fun LibraryResultsContent(
    state: LibraryUiState,
    onOpenArticle: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        state.isSearching -> "Recherche en cours…"
                        state.pagination != null -> {
                            val total = state.pagination.total
                            "$total résultat${if (total > 1) "s" else ""}"
                        }
                        else -> "${state.results.size} résultat${if (state.results.size > 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!state.resultsFromNetwork) {
                    Text(
                        text = "Résultats hors-ligne",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (state.isSearching) {
            items(5) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .height(96.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {}
            }
        }

        // Échec réseau/API sans aucun résultat de repli : jamais l'état vide
        // générique (règle produit — « Je n'ai pas pu vérifier » + Réessayer).
        if (!state.isSearching && state.searchError != null && state.results.isEmpty()) {
            item {
                MibekoErrorState(
                    offline = state.searchError.offline,
                    onRetry = state.searchError.retry
                )
            }
        }

        // Recherche réellement vide (Success avec liste vide) : seul cas où
        // l'état vide est autorisé.
        if (!state.isSearching && state.searchError == null && state.results.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.resultsFromNetwork) {
                            "Aucun texte pour « ${state.submittedQuery} »."
                        } else {
                            "Aucun texte téléchargé ne correspond à « ${state.submittedQuery} »."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.resultsFromNetwork) {
                            "Reformulez ou élargissez les filtres."
                        } else {
                            "Connectez-vous pour chercher dans tout le corpus, ou téléchargez des textes pour la recherche hors-ligne."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Échec réseau/API mais des résultats de repli existent : on les
        // affiche quand même (jamais de faux négatif), avec un bandeau
        // explicite plutôt qu'un écran d'erreur qui les masquerait.
        if (!state.isSearching && state.searchError != null && state.results.isNotEmpty()) {
            item {
                MibekoErrorBanner(
                    offline = state.searchError.offline,
                    onRetry = state.searchError.retry
                )
            }
        }

        if (!state.isSearching) {
            items(state.results, key = { it.id }) { result ->
                Box(modifier = Modifier.padding(vertical = 5.dp)) {
                    SearchResultCard(
                        item = result,
                        onOpenArticle = { onOpenArticle(result.id) },
                        onOpenDocument = {
                            result.document_id?.let(onOpenDocument)
                        }
                    )
                }
            }

            val pagination = state.pagination
            if (pagination != null && pagination.current_page < pagination.last_page) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            OutlinedButton(onClick = onLoadMore) {
                                Text("Afficher plus de résultats")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: LibrarySearchItem,
    onOpenArticle: () -> Unit,
    onOpenDocument: () -> Unit
) {
    Surface(
        onClick = onOpenArticle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(item.document_type ?: "Texte")
                    if (!item.number.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Art. ${item.number}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.document_title ?: "Document",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
            }

            if (item.document_id != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clickable(onClick = onOpenDocument)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ouvrir le texte complet",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// =============================================================================
// Filtres avancés (bottom sheet unique : type, institution, tri)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterSheet(
    state: LibraryUiState,
    onTypeChange: (String?) -> Unit,
    onInstitutionChange: (String?) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filtres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onReset) {
                Text("Réinitialiser")
            }
        }

        // Type de texte
        Text(
            text = "TYPE DE TEXTE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedTypeCode == null,
                onClick = { onTypeChange(null) },
                label = { Text("Tous") }
            )
            state.documentTypes.forEach { type ->
                FilterChip(
                    selected = state.selectedTypeCode == type.code,
                    onClick = { onTypeChange(type.code) },
                    label = { Text(type.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Institution émettrice
        if (state.institutions.isNotEmpty()) {
            Text(
                text = "INSTITUTION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedInstitutionId == null,
                    onClick = { onInstitutionChange(null) },
                    label = { Text("Toutes") }
                )
                state.institutions.forEach { institution ->
                    FilterChip(
                        selected = state.selectedInstitutionId == institution.id,
                        onClick = { onInstitutionChange(institution.id) },
                        label = { Text(institution.acronym ?: institution.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tri
        Text(
            text = "TRI",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibrarySort.entries.forEach { sort ->
                FilterChip(
                    selected = state.sort == sort,
                    onClick = { onSortChange(sort) },
                    label = { Text(sort.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp)
        ) {
            Text("Voir les résultats")
        }
    }
}
