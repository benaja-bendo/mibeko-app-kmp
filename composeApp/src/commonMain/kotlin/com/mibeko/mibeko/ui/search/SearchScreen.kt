package com.mibeko.mibeko.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibeko.mibeko.di.AppModule
import com.mibeko.mibeko.ui.home.MibekoBottomBar
import com.mibeko.mibeko.ui.navigation.MibekoNavigator
import com.mibeko.mibeko.ui.navigation.NavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    query: String, 
    navigator: MibekoNavigator,
    viewModel: SearchViewModel = viewModel { SearchViewModel(AppModule.repository) }
) {
    val results by viewModel.searchResults.collectAsState()

    LaunchedEffect(query) {
        viewModel.updateQuery(query)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Résultats pour : $query", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateTo(NavDestination.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        bottomBar = { MibekoBottomBar(navigator) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = true, label = "Tout", onClick = {})
                FilterChip(selected = false, label = "Codes", onClick = {})
                FilterChip(selected = false, label = "Lois", onClick = {})
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (results.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun résultat trouvé", color = Color.Gray)
                        }
                    }
                } else {
                    items(results) { article ->
                        SearchItemCard(article.number, article.breadcrumb, article.content) {
                            navigator.navigateTo(NavDestination.Reader(article.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        shape = RoundedCornerShape(16.dp),
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = if (selected) Color.White else Color.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SearchItemCard(number: String, breadcrumb: String, snippet: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = number, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "($breadcrumb)", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = snippet,
                maxLines = 3,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
