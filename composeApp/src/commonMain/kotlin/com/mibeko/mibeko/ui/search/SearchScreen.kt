package com.mibeko.mibeko.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import com.mibeko.mibeko.ui.reader.ReaderScreen
import com.mibeko.mibeko.ui.components.HighlightedText

data class SearchResultsScreen(val query: String) : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<SearchViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(query) {
            viewModel.updateQuery(query)
            viewModel.saveSearch(query)
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
                                "\"$query\"",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        selected = uiState.currentFilter == "Tout",
                        label = "Tout",
                        count = if (uiState.currentFilter == "Tout") uiState.results.size else null,
                        onClick = { viewModel.updateFilter("Tout") }
                    )
                    FilterChipItem(
                        selected = uiState.currentFilter == "Codes",
                        label = "Codes",
                        onClick = { viewModel.updateFilter("Codes") }
                    )
                    FilterChipItem(
                        selected = uiState.currentFilter == "Lois",
                        label = "Lois",
                        onClick = { viewModel.updateFilter("Lois") }
                    )
                }

                // Loading indicator
                AnimatedVisibility(
                    visible = uiState.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Results count
                if (!uiState.isLoading && uiState.results.isNotEmpty()) {
                    Text(
                        text = "${uiState.results.size} résultat${if (uiState.results.size > 1) "s" else ""} trouvé${if (uiState.results.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!uiState.isLoading && uiState.results.isEmpty()) {
                        item {
                            EmptyResultsState(query = query)
                        }
                    } else {
                        items(uiState.results) { article ->
                            SearchResultCard(
                                articleNumber = article.number,
                                source = article.breadcrumb,
                                snippet = article.content,
                                query = query,
                                onClick = { navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Reader(article.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Banner showing network status and any error messages.
 */
@Composable
private fun NetworkStatusBanner(
    isFromNetwork: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    // Show error banner if there's an error
    if (errorMessage != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode hors-ligne: $errorMessage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 2
                    )
                }
                IconButton(onClick = onRetry) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Réessayer",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    } else if (isFromNetwork) {
        // Subtle indicator that results are from network
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Résultats en ligne",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

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

@Composable
private fun SearchResultCard(
    articleNumber: String,
    source: String,
    snippet: String,
    query: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Article title
                Text(
                    text = articleNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Source / breadcrumb
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Highlighted excerpt
                HighlightedText(
                    text = snippet.take(200) + if (snippet.length > 200) "..." else "",
                    query = query,
                    maxLines = 3,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Voir",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun EmptyResultsState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Aucun résultat trouvé",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Essayez d'autres termes comme \"licenciement\" ou \"bail\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
