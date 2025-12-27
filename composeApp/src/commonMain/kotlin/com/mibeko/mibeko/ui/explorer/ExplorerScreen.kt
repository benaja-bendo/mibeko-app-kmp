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
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mibeko.mibeko.di.AppModule
import com.mibeko.mibeko.ui.home.HomeViewModel
import com.mibeko.mibeko.ui.home.MibekoBottomBar
import com.mibeko.mibeko.ui.navigation.MibekoNavigator
import com.mibeko.mibeko.ui.navigation.NavDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    navigator: MibekoNavigator,
    viewModel: HomeViewModel = viewModel { HomeViewModel(AppModule.repository) }
) {
    val lawCodes by viewModel.lawCodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explorer les Codes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = { MibekoBottomBar(navigator) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            HorizontalDivider(color = Color(0xFFF0F0F0))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (lawCodes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun code local. Synchronisez depuis l'accueil.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    items(lawCodes) { code ->
                        ExplorerItem(code) {
                            navigator.navigateTo(NavDestination.DocumentDetail(code.id))
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(start = 56.dp))
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
                        color = Color.Gray
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
