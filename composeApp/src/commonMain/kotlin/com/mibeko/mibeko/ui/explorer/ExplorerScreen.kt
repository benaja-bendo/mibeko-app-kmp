package com.mibeko.mibeko.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.viewmodel.koinViewModel
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import com.mibeko.mibeko.ui.details.DocumentDetailScreen
import com.mibeko.mibeko.ui.home.HomeViewModel

class ExplorerScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<HomeViewModel>()
        val lawCodes by viewModel.lawCodes.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Explorer les Codes") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (lawCodes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Aucun code local. Synchronisez depuis l'accueil.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    } else {
                        items(lawCodes) { code ->
                            ExplorerItem(code) {
                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.DocumentDetail(code.id))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExplorerItem(code: com.mibeko.mibeko.data.LawCodeSpec, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Icon based on type or just generic folder/book
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (code.title.contains("Pénal", ignoreCase = true)) Icons.Default.Gavel else Icons.Default.Balance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = code.title, 
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (code.lastUpdated.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mis à jour: ${code.lastUpdated.take(10)}", // Show date part only (YYYY-MM-DD)
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
