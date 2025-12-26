package com.mibeko.mibeko.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.ui.home.MibekoBottomBar
import com.mibeko.mibeko.ui.navigation.MibekoNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navigator: MibekoNavigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { MibekoBottomBar(navigator) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsGroup("Application") {
                SettingsItem("Langue", "Français", Icons.Default.Language)
                SettingsItem("Thème", "Clair", Icons.Default.Brightness4)
                SettingsItem("Notifications", "Activées", Icons.Default.Notifications)
            }
            
            SettingsGroup("Données") {
                SettingsItem("Mise à jour de la base", "Dernière vérification : Aujourd'hui", Icons.Default.Update)
                SettingsItem("Espace disque utilisé", "12.4 MB", Icons.Default.Storage)
            }
            
            SettingsGroup("À propos") {
                SettingsItem("Version", "1.0.0 (Beta)", Icons.Default.Info)
                SettingsItem("Contactez-nous", "", Icons.Default.Email)
                SettingsItem("Conditions d'utilisation", "", Icons.Default.Description)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "© 2024 Mibeko - République du Congo",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = Color.Gray, fontSize = 14.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}
