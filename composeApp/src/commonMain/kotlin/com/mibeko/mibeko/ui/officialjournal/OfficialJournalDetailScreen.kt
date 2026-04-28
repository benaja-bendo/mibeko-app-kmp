package com.mibeko.mibeko.ui.officialjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.ui.components.PdfViewer
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.theme.MibekoBluePrimary
import com.mibeko.mibeko.ui.theme.MibekoGold
import com.mibeko.mibeko.util.formatIsoDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialJournalDetailScreen(id: String) {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<OfficialJournalViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    var showDocumentsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        viewModel.loadJournalDetail(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du Journal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (uiState.currentJournal?.pdf_url != null) {
                        if (uiState.isDownloadingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp).size(24.dp),
                                color = MibekoBluePrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { viewModel.sharePdf(id) }) {
                                Icon(Icons.Default.Share, contentDescription = "Partager", tint = MibekoBluePrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading && uiState.currentJournal == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MibekoBluePrimary)
            }
        } else if (uiState.error != null && uiState.currentJournal == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.error ?: "Erreur", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadJournalDetail(id) }) {
                        Text("Réessayer")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                uiState.currentJournal?.let { journal ->
                    // Header Card
                    JournalHeaderCard(
                        title = journal.title,
                        date = formatIsoDate(journal.publication_date),
                        fileSize = journal.file_size_bytes,
                        documentCount = journal.legal_documents.size,
                        onViewDocuments = { showDocumentsSheet = true }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // PDF Viewer
                    if (!journal.pdf_url.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            PdfViewer(
                                url = journal.pdf_url,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Aucun PDF disponible pour ce journal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showDocumentsSheet && uiState.currentJournal != null) {
        ModalBottomSheet(
            onDismissRequest = { showDocumentsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Documents rattachés",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                val docs = uiState.currentJournal!!.legal_documents
                if (docs.isEmpty()) {
                    item { 
                        Text(
                            text = "Aucun document rattaché à ce journal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) 
                    }
                } else {
                    items(docs) { doc ->
                        DocumentListItem(
                            title = doc.title,
                            reference = doc.reference ?: "Sans référence",
                            onClick = {
                                showDocumentsSheet = false
                                navController.navigate(Screen.DocumentDetail(doc.id))
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun JournalHeaderCard(title: String, date: String, fileSize: Long?, documentCount: Int, onViewDocuments: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MibekoBluePrimary,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Publié le $date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                if (fileSize != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        val sizeInMb = fileSize / (1024.0 * 1024.0)
                        val formattedSize = if (sizeInMb >= 1.0) {
                            "${(sizeInMb * 10.0).toInt() / 10.0} Mo"
                        } else {
                            "${fileSize / 1024} Ko"
                        }
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onViewDocuments,
                colors = ButtonDefaults.buttonColors(containerColor = MibekoGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voir les documents rattachés ($documentCount)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun DocumentListItem(title: String, reference: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}