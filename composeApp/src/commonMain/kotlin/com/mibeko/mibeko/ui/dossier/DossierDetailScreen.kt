package com.mibeko.mibeko.ui.dossier

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.mibeko.mibeko.data.local.dao.DossierArticleWithDetails
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen as NavScreen
import androidx.navigation.NavController
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch

class DossierDetailScreen(private val dossierId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        val viewModel = koinViewModel<DossierDetailViewModel> { parametersOf(dossierId) }
        val uiState by viewModel.uiState.collectAsState()
        val showNoteDialog by viewModel.showNoteDialog.collectAsState()
        val showEditDialog by viewModel.showEditDialog.collectAsState()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val dossier = uiState.dossier
        val dossierColor = if (dossier != null) parseColor(dossier.color) else MaterialTheme.colorScheme.primary

        fun shareDossierContent() {
            viewModel.exportPdf(
                onSuccess = { bytes ->
                    scope.launch {
                        // TODO: Implement actual file saving and sharing via platform channel
                        // Platform.sharePdf(bytes, "${dossier?.name ?: "dossier"}.pdf")
                        snackbarHostState.showSnackbar("PDF généré (${bytes.size} bytes). Sauvegarde non implémentée.")
                    }
                },
                onError = { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Erreur: $error")
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = dossierColor
                    ),
                    actions = {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp).padding(end = 16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { /* TODO: More options */ }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                val canEdit = dossier?.tag != DossierTag.FAVORIS

                DossierActionBar(
                    onAddArticle = { 
                        scope.launch {
                             snackbarHostState.showSnackbar("Pour ajouter un article, utilisez le bouton 'Ajouter au dossier' lors de la lecture d'un article.")
                        }
                    },
                    onShare = { shareDossierContent() },
                    onEdit = { viewModel.showEditDialog() },
                    canEdit = canEdit,
                    isExporting = uiState.isExporting
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            ) {
                // Header with dossier info
                DossierHeader(dossier, dossierColor, uiState.articleCount)
                
                // Filter Tabs
                DossierFilterTabs(
                    currentFilter = uiState.filter,
                    onFilterSelected = { viewModel.setFilter(it) },
                    selectedDocument = uiState.documents.find { it.id == viewModel.run { 
                        // Using reflection or accessor would be better, but for now we reset on view change in VM
                        null 
                    }}?.title // Placeholder for selected doc title if needed
                )
                
                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.documents.isEmpty() && uiState.articles.isEmpty()) {
                        EmptyDossierState()
                    } else {
                        when (uiState.filter) {
                            DossierFilterType.DOCUMENTS -> {
                                if (uiState.displayedArticles.isNotEmpty() && uiState.filter == DossierFilterType.DOCUMENTS) {
                                    // This state (doc selected) usually moves us to a specific view, 
                                    // but if we are in Documents mode and have a selection logic in VM:
                                    // Actually VM logic clears selection on filter change.
                                    // Documents View:
                                    DocumentsGrid(
                                        documents = uiState.documents,
                                        onDocumentClick = { docId ->
                                             viewModel.selectDocument(docId)
                                             viewModel.setFilter(DossierFilterType.ALL) // Switch to list view effectively filtered
                                        }
                                    )
                                } else {
                                    DocumentsGrid(
                                        documents = uiState.documents,
                                        onDocumentClick = { docId ->
                                             viewModel.selectDocument(docId)
                                             // We stay in view but show only that doc's articles? 
                                             // Design choice: Switching to "Articles" view filtered by doc is better.
                                             // Or "All" view.
                                        }
                                    )
                                }
                            }
                            DossierFilterType.ARTICLES -> {
                                ArticlesList(
                                    articles = uiState.articles,
                                    navController = navController,
                                    onEditNote = { viewModel.showNoteDialog(it) },
                                    onRemove = { viewModel.removeArticle(it.article.id) }
                                )
                            }
                            DossierFilterType.ALL -> {
                                if (uiState.displayedArticles.isNotEmpty()) {
                                    // Filtered by specific document
                                    Column {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = { viewModel.verifyFilter() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Filtre actif : ${uiState.displayedArticles.firstOrNull()?.document_title ?: "Document"}")
                                                Spacer(Modifier.weight(1f))
                                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        ArticlesList(
                                            articles = uiState.displayedArticles,
                                            navController = navController,
                                            onEditNote = { viewModel.showNoteDialog(it) },
                                            onRemove = { viewModel.removeArticle(it.article.id) }
                                        )
                                    }
                                } else {
                                    // Show all articles
                                    ArticlesList(
                                        articles = uiState.articles,
                                        navController = navController,
                                        onEditNote = { viewModel.showNoteDialog(it) },
                                        onRemove = { viewModel.removeArticle(it.article.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Note dialog
        showNoteDialog?.let { article ->
            NoteDialog(
                currentNote = article.personal_note,
                onDismiss = { viewModel.dismissNoteDialog() },
                onConfirm = { note -> viewModel.updateNote(article.article.id, note) }
            )
        }

        // Edit dialog
        if (showEditDialog && dossier != null) {
            CreateDossierDialog(
                dossier = dossier,
                onDismiss = { viewModel.dismissEditDialog() },
                onConfirm = { name, domain, tag, desc, color ->
                    viewModel.updateDossier(name, domain, tag, desc, color)
                }
            )
        }
    }
}

@Composable
fun DossierHeader(dossier: DossierEntity?, color: Color, count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Folder icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = dossier?.name ?: "Chargement...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                 if (dossier != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = dossier.legal_domain,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "$count articles",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                 }
            }
        }
    }
}

@Composable
fun DossierFilterTabs(
    currentFilter: DossierFilterType,
    onFilterSelected: (DossierFilterType) -> Unit,
    selectedDocument: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == DossierFilterType.ALL,
            onClick = { onFilterSelected(DossierFilterType.ALL) },
            label = { Text("Tout") },
            leadingIcon = { Icon(Icons.Default.Dashboard, null, Modifier.size(16.dp)) }
        )
        FilterChip(
            selected = currentFilter == DossierFilterType.DOCUMENTS,
            onClick = { onFilterSelected(DossierFilterType.DOCUMENTS) },
            label = { Text("Documents") },
            leadingIcon = { Icon(Icons.Default.LibraryBooks, null, Modifier.size(16.dp)) }
        )
        FilterChip(
            selected = currentFilter == DossierFilterType.ARTICLES,
            onClick = { onFilterSelected(DossierFilterType.ARTICLES) },
            label = { Text("Articles") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Article, null, Modifier.size(16.dp)) }
        )
    }
}

@Composable
fun DocumentsGrid(
    documents: List<ClientDossierDocument>,
    onDocumentClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(documents) { doc ->
            Card(
                onClick = { onDocumentClick(doc.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Book, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        doc.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${doc.articleCount} articles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ArticlesList(
    articles: List<DossierArticleWithDetails>,
    navController: androidx.navigation.NavController,
    onEditNote: (DossierArticleWithDetails) -> Unit,
    onRemove: (DossierArticleWithDetails) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(articles) { article ->
            DossierArticleCard(
                article = article,
                onClick = {
                    navController.navigate(NavScreen.Reader(article.article.id))
                },
                onEditNote = { onEditNote(article) },
                onRemove = { onRemove(article) }
            )
        }
    }
}


@Composable
fun DossierArticleCard(
    article: DossierArticleWithDetails,
    onClick: () -> Unit,
    onEditNote: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.document_title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Article ${article.article.number}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = (article.article.content ?: "").take(80) + "...",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Show personal note if exists
                article.personal_note?.let { note ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.StickyNote2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier la note") },
                        onClick = {
                            showMenu = false
                            onEditNote()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Retirer du dossier", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.RemoveCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DossierActionBar(
    onAddArticle: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    canEdit: Boolean,
    isExporting: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (isExporting) {
                Text("Export en cours...", modifier = Modifier.align(Alignment.CenterVertically))
            } else {
                ActionButton(
                    icon = Icons.Filled.Share,
                    label = "Partager",
                    onClick = onShare
                )
                
                if (canEdit) {
                    ActionButton(
                        icon = Icons.Filled.Edit,
                        label = "Modifier",
                        onClick = onEdit
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
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
                .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun NoteDialog(
    currentNote: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var note by remember { mutableStateOf(currentNote ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note personnelle", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Ajouter une note sur cet article...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(note.ifBlank { null }) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EmptyDossierState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun élément",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ajoutez des articles à ce dossier",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}
