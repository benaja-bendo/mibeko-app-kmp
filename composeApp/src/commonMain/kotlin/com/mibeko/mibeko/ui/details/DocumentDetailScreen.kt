package com.mibeko.mibeko.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.di.AppModule
import com.mibeko.mibeko.ui.navigation.MibekoNavigator
import com.mibeko.mibeko.ui.navigation.NavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    navigator: MibekoNavigator,
    viewModel: DocumentDetailViewModel = viewModel { DocumentDetailViewModel(AppModule.repository) }
) {
    val structure by viewModel.structure.collectAsState()

    LaunchedEffect(documentId) {
        viewModel.loadStructure(documentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Table des Matières", fontSize = 18.sp) }, // Ideally show Document Title
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateTo(NavDestination.Explorer) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (structure.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chargement...", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Determine sort order
                val sortedNodes = structure.keys.sortedBy { it.sort_order }

                sortedNodes.forEach { node ->
                    item {
                        Text(
                            text = "${node.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    
                    val articles = structure[node]?.sortedBy { it.number.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 } ?: emptyList()
                    
                    items(articles) { article ->
                        ArticleItem(article) {
                            navigator.navigateTo(NavDestination.Reader(article.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleItem(article: ArticleEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Article ${article.number}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = article.content.take(60) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
