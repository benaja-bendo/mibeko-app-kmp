package com.mibeko.mibeko.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel

data class ReaderScreen(val articleId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<ReaderViewModel>()
        val article by viewModel.article.collectAsState()

        LaunchedEffect(articleId) {
            viewModel.loadArticle(articleId)
        }

        if (article == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        val currentArticle = article!!

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(currentArticle.number, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(currentArticle.breadcrumb, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) { 
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        Spacer(modifier = Modifier.width(16.dp)) 
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ReaderNavAction("Précédent", Icons.AutoMirrored.Filled.KeyboardArrowLeft) { }
                        ReaderNavAction("Suivant", Icons.AutoMirrored.Filled.KeyboardArrowRight) { }
                        ReaderNavAction("Favori", if (currentArticle.isFavorite) Icons.Default.Star else Icons.Default.StarBorder) { }
                        ReaderNavAction("Partager", Icons.Default.Share) { }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = currentArticle.number,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currentArticle.breadcrumb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = currentArticle.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ReaderNavAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
