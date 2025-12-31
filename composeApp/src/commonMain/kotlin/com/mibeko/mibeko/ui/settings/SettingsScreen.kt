package com.mibeko.mibeko.ui.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import androidx.compose.foundation.clickable
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import org.koin.compose.viewmodel.koinViewModel

class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<SettingsViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        var showDisclaimer by remember { mutableStateOf(false) }
        var showThemeDialog by remember { mutableStateOf(false) }
        var showTerms by remember { mutableStateOf(false) }
        var showPrivacy by remember { mutableStateOf(false) }

        if (showDisclaimer) {
            AlertDialog(
                onDismissRequest = { showDisclaimer = false },
                title = { Text("Mentions Légales") },
                text = {
                    Text(
                        "Mibeko est une application à but informatif recensant les textes de lois de la République du Congo.\n\n" +
                        "Bien que nous nous efforcions de maintenir les données à jour, Mibeko ne peut être tenu responsable d'erreurs ou d'omissions. " +
                        "Les textes officiels publiés au Journal Officiel font foi.\n\n" +
                        "Cette application n'est pas affiliée au gouvernement congolais."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDisclaimer = false }) {
                        Text("Compris")
                    }
                }
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choisir le thème") },
                text = {
                    Column {
                        val themes = listOf(
                            UserPreferencesRepository.AppTheme.SYSTEM to "Système",
                            UserPreferencesRepository.AppTheme.LIGHT to "Clair",
                            UserPreferencesRepository.AppTheme.DARK to "Sombre"
                        )
                        themes.forEach { pair ->
                            val theme = pair.first
                            val label = pair.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false 
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.currentTheme == theme,
                                    onClick = { 
                                        viewModel.setTheme(theme)
                                        showThemeDialog = false 
                                    }
                                )
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }

        if (showTerms) {
            AlertDialog(
                onDismissRequest = { showTerms = false },
                title = { Text("Conditions d'Utilisation") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "En utilisant Mibeko, vous acceptez les conditions suivantes :\n\n" +
                            "1. Utilisation du service : Mibeko est fourni 'en l'état'. L'accès peut être suspendu pour maintenance.\n\n" +
                            "2. Responsabilité : Les informations fournies sont à titre indicatif. Seuls les textes officiels font foi.\n\n" +
                            "3. Propriété intellectuelle : Le contenu de l'application est protégé par les lois sur la propriété intellectuelle."
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTerms = false }) {
                        Text("Fermer")
                    }
                }
            )
        }

        if (showPrivacy) {
            AlertDialog(
                onDismissRequest = { showPrivacy = false },
                title = { Text("Politique de Confidentialité") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Votre vie privée est importante pour nous.\n\n" +
                            "1. Collecte de données : Mibeko ne collecte aucune donnée personnelle identifiable sans votre consentement.\n\n" +
                            "2. Utilisation : Les préférences (thème, langue) sont stockées localement sur votre appareil.\n\n" +
                            "3. Partage : Aucune donnée n'est partagée avec des tiers à des fins commerciales."
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacy = false }) {
                        Text("Fermer")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Réglages") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = { MibekoBottomBar(navigator) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // --- MODE HORS-LIGNE SECTION ---
                SettingsGroup("Mode Hors-ligne") {
                    SettingsSwitch(
                        title = "Mode Hors-ligne uniquement",
                        subtitle = "N'utilise pas le réseau pour les recherches",
                        icon = Icons.Default.WifiOff,
                        checked = uiState.isOfflineModeEnabled,
                        onCheckedChange = { viewModel.setOfflineMode(it) }
                    )
                }
                
                // --- DOWNLOAD MANAGER SECTION ---
                SettingsGroup("Téléchargements") {
                    if (uiState.isLoadingDocuments) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.documents.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Aucun document disponible",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Synchronisez d'abord pour voir les documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        uiState.documents.forEach { doc ->
                            DownloadableDocumentItem(
                                title = doc.title,
                                typeCode = doc.typeCode,
                                isDownloaded = doc.isDownloaded,
                                isDownloading = doc.isDownloading,
                                downloadProgress = doc.downloadProgress,
                                onDownload = { viewModel.downloadDocument(doc.id) },
                                onDelete = { viewModel.deleteDocument(doc.id) }
                            )
                        }
                    }
                }
                
                // --- APPLICATION SETTINGS ---
                SettingsGroup("Application") {
                    SettingsItem("Langue", "Français", Icons.Default.Language)
                    SettingsItem(
                        title = "Thème", 
                        subtitle = when(uiState.currentTheme) {
                            UserPreferencesRepository.AppTheme.SYSTEM -> "Système"
                            UserPreferencesRepository.AppTheme.LIGHT -> "Clair"
                            UserPreferencesRepository.AppTheme.DARK -> "Sombre"
                        }, 
                        icon = Icons.Default.Brightness4,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsSwitch(
                        title = "Notifications", 
                        subtitle = if (uiState.isNotificationsEnabled) "Activées" else "Désactivées", 
                        icon = Icons.Default.Notifications,
                        checked = uiState.isNotificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
                
                // --- DATA SECTION ---
                SettingsGroup("Données") {
                    SettingsItem(
                        title = "Mise à jour de la base", 
                        subtitle = "Dernière vérification : ${uiState.lastUpdateDate}", 
                        icon = Icons.Default.Update
                    )
                    SettingsItem(
                        title = "Espace disque utilisé", 
                        subtitle = uiState.diskUsage, 
                        icon = Icons.Default.Storage,
                        onClick = { viewModel.refreshDiskUsage() }
                    )
                }
                
                // --- ABOUT SECTION ---
                SettingsGroup("À propos") {
                    SettingsItem("Version", "1.0.2 (Production)", Icons.Default.Info)
                    SettingsItem("Contactez-nous", "contact@mibeko.cg", Icons.Default.Email)
                    SettingsItem(
                        title = "Conditions d'utilisation", 
                        subtitle = "Lire les conditions", 
                        icon = Icons.Default.Description,
                        onClick = { showTerms = true }
                    )
                    SettingsItem(
                        title = "Politique de confidentialité", 
                        subtitle = "Données personnelles", 
                        icon = Icons.Default.PrivacyTip,
                        onClick = { showPrivacy = true }
                    )
                    SettingsItem(
                        title = "Mentions Légales", 
                        subtitle = "Clause de non-responsabilité", 
                        icon = Icons.Default.Gavel,
                        onClick = { showDisclaimer = true }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "© 2025 Mibeko - République du Congo",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

/**
 * Settings item with a switch toggle.
 */
@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Item showing a downloadable document with download/delete action.
 */
@Composable
fun DownloadableDocumentItem(
    title: String,
    typeCode: String,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document icon based on type
            Icon(
                when (typeCode.uppercase()) {
                    "CODE" -> Icons.Default.MenuBook
                    "LOI" -> Icons.Default.Gavel
                    "DECRET" -> Icons.Default.Description
                    else -> Icons.Default.Article
                },
                contentDescription = null,
                tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = if (isDownloaded) "Disponible hors-ligne" else "Non téléchargé",
                    fontSize = 12.sp,
                    color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Action button
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else if (isDownloaded) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = "Télécharger",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Download progress indicator
        AnimatedVisibility(visible = isDownloading) {
            LinearProgressIndicator(
                progress = { downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
