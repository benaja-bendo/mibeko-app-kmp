package com.mibeko.mibeko.ui.officialjournal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.remote.RemoteDocument
import com.mibeko.mibeko.ui.components.OfficialSourceAttribution
import com.mibeko.mibeko.ui.components.OfficialSourcesSheet
import com.mibeko.mibeko.ui.components.PdfViewer
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.theme.MibekoCream
import com.mibeko.mibeko.ui.theme.MibekoForest
import com.mibeko.mibeko.util.formatIsoDateLong
import org.koin.compose.viewmodel.koinViewModel

/**
 * Détail d'un Journal Officiel, pensé comme un numéro que l'on feuillette :
 * les textes publiés sont la matière première (affichés directement), le PDF
 * est une action secondaire, et un swipe horizontal passe au numéro
 * précédent / suivant — même geste que le lecteur d'articles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialJournalDetailScreen(id: String) {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<OfficialJournalViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    var displayedId by remember { mutableStateOf(id) }
    var showPdf by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }

    LaunchedEffect(displayedId) {
        viewModel.loadJournalDetail(displayedId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal Officiel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (uiState.journals.size > 1 && uiState.currentIndex >= 0) {
                JournalNavigationBar(
                    currentIndex = uiState.currentIndex,
                    total = uiState.journals.size,
                    hasPrevious = uiState.previousJournalId != null,
                    hasNext = uiState.nextJournalId != null,
                    onPrevious = { uiState.previousJournalId?.let { displayedId = it } },
                    onNext = { uiState.nextJournalId?.let { displayedId = it } }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading && uiState.currentJournal == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.error != null && uiState.currentJournal == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.error ?: "Erreur", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadJournalDetail(displayedId) }) {
                            Text("Réessayer")
                        }
                    }
                }
            }

            else -> {
                val journal = uiState.currentJournal ?: return@Scaffold

                // Swipe gauche = numéro plus ancien, droite = plus récent.
                var dragTotal by remember { mutableStateOf(0f) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pointerInput(uiState.currentIndex, uiState.journals.size) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onDragEnd = {
                                    when {
                                        dragTotal < -150f -> uiState.nextJournalId?.let { displayedId = it }
                                        dragTotal > 150f -> uiState.previousJournalId?.let { displayedId = it }
                                    }
                                    dragTotal = 0f
                                },
                                onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount }
                            )
                        },
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        JournalHeroHeader(
                            title = journal.number?.let { "JO n° " + it } ?: journal.title,
                            subtitle = "Publié le " + formatIsoDateLong(journal.publication_date),
                            fileSize = journal.file_size_bytes,
                            hasPdf = !journal.pdf_url.isNullOrEmpty(),
                            isDownloadingPdf = uiState.isDownloadingPdf,
                            onOpenPdf = { showPdf = true },
                            onShare = { viewModel.sharePdf(displayedId) }
                        )
                    }

                    item {
                        val count = journal.legal_documents.size
                        Text(
                            text = if (count == 0) {
                                "DANS CE NUMÉRO"
                            } else {
                                "DANS CE NUMÉRO — " + count + " TEXTE" + (if (count > 1) "S" else "")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    if (journal.legal_documents.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Les textes de ce numéro n'ont pas encore été transcrits.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!journal.pdf_url.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Consultez le PDF original en attendant.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(journal.legal_documents, key = { it.id }) { doc ->
                            JournalTextItem(
                                document = doc,
                                onClick = { navController.navigate(Screen.DocumentDetail(doc.id)) }
                            )
                        }
                    }

                    item {
                        OfficialSourceAttribution(
                            onClick = { showSources = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }

    // Le PDF n'est plus l'écran principal : il s'ouvre à la demande,
    // en plein écran, par-dessus le flux des textes.
    if (showPdf) {
        val pdfUrl = uiState.currentJournal?.pdf_url
        if (!pdfUrl.isNullOrEmpty()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                uiState.currentJournal?.number?.let { "JO n° " + it + " — PDF" } ?: "PDF",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showPdf = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer le PDF")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    PdfViewer(
                        url = pdfUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            showPdf = false
        }
    }

    if (showSources) {
        OfficialSourcesSheet(onDismiss = { showSources = false })
    }
}

@Composable
private fun JournalHeroHeader(
    title: String,
    subtitle: String,
    fileSize: Long?,
    hasPdf: Boolean,
    isDownloadingPdf: Boolean,
    onOpenPdf: () -> Unit,
    onShare: () -> Unit
) {
    Surface(color = MibekoForest) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MibekoCream
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle + (fileSize?.let { " · " + formatFileSize(it) } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MibekoCream.copy(alpha = 0.75f)
            )

            if (hasPdf) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenPdf,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ouvrir le PDF")
                    }
                    OutlinedButton(
                        onClick = onShare,
                        enabled = !isDownloadingPdf,
                        border = BorderStroke(1.dp, MibekoCream.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MibekoCream),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (isDownloadingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MibekoCream
                            )
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Le PDF intégral est téléchargé avant l'envoi : le
                        // libellé doit annoncer que c'est le fichier qui part.
                        Text("Partager le PDF")
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalTextItem(document: RemoteDocument, onClick: () -> Unit) {
    val typeCode = document.type?.code ?: document.type_code ?: "TEXTE"
    val accent = typeAccentColor(typeCode)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(accent)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = document.type?.name ?: typeCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                document.articles_count?.takeIf { it > 0 }?.let { count ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "" + count + " article" + (if (count > 1) "s" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(end = 10.dp).size(20.dp)
            )
        }
    }
}

@Composable
private fun JournalNavigationBar(
    currentIndex: Int,
    total: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrevious, enabled = hasPrevious) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Plus récent", style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = "" + (currentIndex + 1) + " / " + total,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNext, enabled = hasNext) {
                    Text("Plus ancien", style = MaterialTheme.typography.labelLarge)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Couleur d'accent par type de texte (bordure + libellé). */
@Composable
private fun typeAccentColor(typeCode: String): androidx.compose.ui.graphics.Color {
    return when (typeCode.uppercase()) {
        "LOI", "LOI_ORG", "CONST" -> MaterialTheme.colorScheme.primary
        "ARR", "CIRC" -> MaterialTheme.colorScheme.secondary
        "DEC", "ORD" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) "" + ((mb * 10).toInt() / 10.0) + " Mo" else "" + (bytes / 1024) + " Ko"
}
