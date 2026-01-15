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

import com.mibeko.mibeko.ui.theme.MibekoGold

import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.reader.ReaderScreen

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextAlign

data class DocumentDetailScreen(val documentId: String) : Screen {

    /**
     * Contenu principal de l'écran de détail d'un document.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<DocumentDetailViewModel>()
        val structure by viewModel.structure.collectAsState()
        val document by viewModel.document.collectAsState()
        
        val backgroundColor = Color(0xFFF9F6F0) // Parchment white
        val textColor = Color(0xFF1A1A1A)

        LaunchedEffect(documentId) {
            viewModel.loadStructure(documentId)
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { 
                            Text(
                                text = document?.title ?: "Détails", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = textColor)
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* Internal search */ }) {
                                Icon(Icons.Filled.Search, contentDescription = "Rechercher", tint = textColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
                    )
                    
                    // Breadcrumb
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(textColor.copy(alpha = 0.05f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bibliothèque Juridique", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.6f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(12.dp), tint = textColor.copy(alpha = 0.3f))
                        Text(document?.title ?: "", style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1)
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = backgroundColor,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { /* Download PDF */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Télécharger PDF", style = MaterialTheme.typography.labelLarge)
                        }
                        
                        OutlinedButton(
                            onClick = { /* Share */ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Partager", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            },
            containerColor = backgroundColor
        ) { padding ->
            if (structure.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = document?.title?.uppercase() ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 2.dp, color = textColor.copy(alpha = 0.2f))
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "RECHERCHER une loi...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 2.dp, color = textColor.copy(alpha = 0.2f))
                        }
                    }

                    val sortedNodes = structure.keys.sortedBy { it.sort_order }
                    sortedNodes.forEach { node ->
                        item {
                            Surface(
                                color = textColor.copy(alpha = 0.03f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                        
                        val articles = structure[node]?.sortedBy { it.number.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 } ?: emptyList()
                        items(articles) { article ->
                            ArticleItem(article, textColor) {
                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Reader(article.id))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Article ${article.number}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp), tint = textColor.copy(alpha = 0.3f))
        }
        
        if (!article.content.isNullOrBlank()) {
            Text(
                text = article.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.05f))
    }
}
