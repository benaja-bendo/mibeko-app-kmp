package com.mibeko.mibeko.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.ui.components.SyncStatusIndicator
import com.mibeko.mibeko.ui.components.SyncState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.mibeko.mibeko.ui.components.MibekoBreadcrumb
import com.mibeko.mibeko.ui.components.BreadcrumbSegment

import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.reader.ReaderScreen

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

data class DocumentDetailScreen(val documentId: String) : Screen {

    /**
     * Contenu principal de l'écran de détail d'un document.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<DocumentDetailViewModel>()
        
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
        val dossier = uiState.document
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        val scope = rememberCoroutineScope()
        
        var isSearchActive by remember { mutableStateOf(false) }

        LaunchedEffect(documentId) {
            viewModel.loadStructure(documentId)
        }

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
                    CenterAlignedTopAppBar(
                        title = { 
                            Text(
                                text = uiState.document?.title ?: "Détails", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = !isSearchActive }) {
                                Icon(
                                    if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search, 
                                    contentDescription = "Rechercher", 
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    if (isSearchActive) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = { Text("Rechercher dans ce document...") },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(Icons.Filled.Close, null)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                    
                    // Dynamic Breadcrumb
                    val breadcrumbSegments = listOf(
                        BreadcrumbSegment("Bibliothèque") { navController.popBackStack() },
                        BreadcrumbSegment(uiState.document?.type ?: "Norme") { },
                        BreadcrumbSegment(uiState.document?.title ?: "") { }
                    )
                    
                    MibekoBreadcrumb(
                        segments = breadcrumbSegments,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleOffline() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.document?.isDownloaded == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isDownloading
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    if (uiState.document?.isDownloaded == true) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (uiState.document?.isDownloaded == true) "Hors-ligne" else "Télécharger",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                val shareText = viewModel.getShareText()
                                com.mibeko.mibeko.util.shareText(shareText)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Partager", style = MaterialTheme.typography.labelLarge)
                        }

                        // PDF Export Button
                        IconButton(
                            onClick = { 
                                val url = viewModel.exportPdf()
                                if (url.isNotEmpty()) {
                                    uriHandler.openUri(url)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("URL d'export non disponible") }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Filled.PictureAsPdf, 
                                contentDescription = "PDF", 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Chargement du document...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                uiState.error != null && uiState.structure.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(40.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    Icons.Default.Gavel, 
                                    null, 
                                    modifier = Modifier.padding(20.dp), 
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Oups !",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "Une erreur est survenue lors de la récupération du document.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { viewModel.loadStructure(documentId) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
                uiState.structure.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Aucun contenu disponible pour ce document.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = uiState.document?.type ?: "DOC",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = uiState.document?.title ?: "",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 28.sp
                                    )
                                }
                            }
                        }

                        val sortedNodes = uiState.filteredStructure.keys.sortedBy { it.sort_order }
                        
                        if (sortedNodes.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Aucun article trouvé pour \"${uiState.searchQuery}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        sortedNodes.forEach { node ->
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = node.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            
                            val articles = uiState.filteredStructure[node]?.sortedBy { it.number.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 } ?: emptyList()
                            items(articles) { article ->
                                ArticleItem(article, MaterialTheme.colorScheme.onSurface) {
                                    navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Reader(article.id))
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
 * Élément de liste affichant un article dans la table des matières.
 */
@Composable
fun ArticleItem(article: ArticleEntity, textColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Art. ${article.number}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    null, 
                    modifier = Modifier.size(18.dp), 
                    tint = textColor.copy(alpha = 0.3f)
                )
            }
            
            if (!article.content.isNullOrBlank()) {
                Text(
                    text = article.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                    lineHeight = 22.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    }
}
