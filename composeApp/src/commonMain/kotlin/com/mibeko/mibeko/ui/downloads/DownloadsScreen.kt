package com.mibeko.mibeko.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as NavScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<DownloadsViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        
        val tabs = remember { listOf("Documents", "Articles") }
        val pagerState = rememberPagerState { tabs.size }
        
        var isSearchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }

        // Sync selected tab with pager
        val selectedTabIndex = pagerState.currentPage

        val filteredDocuments = remember(uiState.documents, searchQuery) {
            if (searchQuery.isBlank()) uiState.documents
            else uiState.documents.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

        val filteredArticles = remember(uiState.offlineArticles, searchQuery) {
            val articles = uiState.offlineArticles.filter { it.isDownloaded }
            if (searchQuery.isBlank()) articles
            else articles.filter { 
                it.number.contains(searchQuery, ignoreCase = true) || 
                it.breadcrumb.contains(searchQuery, ignoreCase = true) 
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        title = { 
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Rechercher...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            } else {
                                Text("Gestionnaire Hors-ligne") 
                            }
                        },
                        navigationIcon = {
                            if (isSearchActive) {
                                IconButton(onClick = { 
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                                    }
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Rechercher")
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    @Suppress("DEPRECATION")
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { 
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (uiState.isLoading && uiState.documents.isEmpty() && uiState.offlineArticles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> DocumentList(
                            documents = filteredDocuments,
                            downloadingIds = uiState.downloadingIds,
                            onDownload = { viewModel.downloadDocument(it) },
                            onRemove = { viewModel.removeDownload(it) },
                            onNavigate = { id -> navController.navigate(NavScreen.DocumentDetail(id)) },
                            onNavigateToLibrary = { navController.navigate(NavScreen.Library) }
                        )
                        1 -> ArticleList(
                            articles = filteredArticles,
                            onRemove = { viewModel.removeArticleDownload(it) },
                            onNavigate = { id -> navController.navigate(NavScreen.Reader(id)) },
                            onNavigateToLibrary = { navController.navigate(NavScreen.Library) }
                        )
                    }
                }
            }
        }
    }
 

@Composable
private fun DocumentList(
    documents: List<LawCodeSpec>,
    downloadingIds: Set<String>,
    onDownload: (String) -> Unit,
    onRemove: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    if (documents.isEmpty()) {
        EmptyState("Aucun document trouvé", onNavigateToLibrary)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InfoCard("Documents", "Téléchargez les codes complets pour y accéder sans connexion.") }
            items(documents, key = { it.id }) { doc ->
                DownloadItemCard(
                    title = doc.title,
                    subtitle = if (doc.isDownloaded) "Disponible hors-ligne" else "En ligne uniquement",
                    isDownloaded = doc.isDownloaded,
                    isDownloading = downloadingIds.contains(doc.id),
                    onAction = { if (doc.isDownloaded) onRemove(doc.id) else onDownload(doc.id) },
                    onClick = { onNavigate(doc.id) }
                )
            }
        }
    }
}

@Composable
private fun ArticleList(
    articles: List<ArticleSpec>,
    onRemove: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    if (articles.isEmpty()) {
        EmptyState("Aucun article trouvé", onNavigateToLibrary)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InfoCard("Articles", "Articles que vous avez mis hors-ligne individuellement.") }
            items(articles, key = { it.id }) { article ->
                DownloadItemCard(
                    title = "Article ${article.number}",
                    subtitle = article.breadcrumb,
                    isDownloaded = true,
                    isDownloading = false,
                    onAction = { onRemove(article.id) },
                    onClick = { onNavigate(article.id) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, onNavigateToLibrary: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Vous n'avez pas encore de contenu disponible hors-ligne.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateToLibrary,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Explorer la bibliothèque")
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DownloadItemCard(
    title: String,
    subtitle: String,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onAction: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onAction) {
                    Icon(
                        if (isDownloaded) Icons.Default.Delete else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (isDownloaded) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }


}
