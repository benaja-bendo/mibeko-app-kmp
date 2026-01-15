package com.mibeko.mibeko.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as NavScreen
import org.koin.compose.viewmodel.koinViewModel

class DownloadsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<DownloadsViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Documents", "Articles")

        LaunchedEffect(uiState.error) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("Gestionnaire Hors-ligne") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
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
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (selectedTabIndex) {
                        0 -> DocumentList(
                            documents = uiState.documents,
                            downloadingIds = uiState.downloadingIds,
                            onDownload = { viewModel.downloadDocument(it) },
                            onRemove = { viewModel.removeDownload(it) },
                            onNavigate = { id -> navController.navigate(NavScreen.DocumentDetail(id)) },
                            onNavigateToLibrary = { navController.navigate(NavScreen.Library) }
                        )
                        1 -> ArticleList(
                            articles = uiState.offlineArticles,
                            onRemove = { viewModel.removeArticleDownload(it) },
                            onNavigate = { id -> navController.navigate(NavScreen.Reader(id)) },
                            onNavigateToLibrary = { navController.navigate(NavScreen.Library) }
                        )
                    }
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
        EmptyState("Aucun document téléchargé", onNavigateToLibrary)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InfoCard("Documents", "Téléchargez les codes complets pour y accéder sans connexion.") }
            items(documents) { doc ->
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
    val downloadedArticles = articles.filter { it.isDownloaded }
    if (downloadedArticles.isEmpty()) {
        EmptyState("Aucun article hors-ligne", onNavigateToLibrary)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InfoCard("Articles", "Articles que vous avez mis hors-ligne individuellement.") }
            items(downloadedArticles) { article ->
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
                shape = RoundedCornerShape(20.dp),
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
