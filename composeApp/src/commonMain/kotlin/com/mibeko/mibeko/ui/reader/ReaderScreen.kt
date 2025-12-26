package com.mibeko.mibeko.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.MOCK_ARTICLES
import com.mibeko.mibeko.ui.navigation.MibekoNavigator
import com.mibeko.mibeko.ui.navigation.NavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(articleId: String, navigator: MibekoNavigator) {
    val article = MOCK_ARTICLES.find { it.id == articleId } ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(article.number, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(article.breadcrumb, color = Color.Gray, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateTo(NavDestination.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { }) { 
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    Spacer(modifier = Modifier.width(16.dp)) // To center title better
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ReaderNavAction("Préc.", Icons.Default.KeyboardArrowLeft) { }
                    ReaderNavAction("Suiv.", Icons.Default.KeyboardArrowRight) { }
                    ReaderNavAction("Favori", Icons.Default.StarBorder) { }
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
                text = article.number,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = article.breadcrumb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = article.content,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 28.sp,
                color = Color(0xFF212121)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
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
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Black)
    }
}
