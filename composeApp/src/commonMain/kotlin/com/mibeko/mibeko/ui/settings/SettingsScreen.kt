package com.mibeko.mibeko.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.mibeko.mibeko.ui.navigation.Screen as AppScreen
import com.mibeko.mibeko.ui.navigation.MibekoBottomBar
import androidx.compose.foundation.clickable
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import org.koin.compose.viewmodel.koinViewModel

class SettingsScreen : cafe.adriel.voyager.core.screen.Screen {

    /**
     * Contenu principal de l'écran des réglages.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<SettingsViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        
        var showThemeDialog by remember { mutableStateOf(false) }
        var showTextSizeDialog by remember { mutableStateOf(false) }
        var showTerms by remember { mutableStateOf(false) }
        var showPrivacy by remember { mutableStateOf(false) }
        var showAbout by remember { mutableStateOf(false) }

        LaunchedEffect(uiState.syncError) {
            uiState.syncError?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSyncError()
            }
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

        if (showTextSizeDialog) {
            AlertDialog(
                onDismissRequest = { showTextSizeDialog = false },
                title = { Text("Taille du texte") },
                text = {
                    Column {
                        val sizes = listOf(
                            UserPreferencesRepository.TextSize.SMALL to "Petit",
                            UserPreferencesRepository.TextSize.MEDIUM to "Moyen",
                            UserPreferencesRepository.TextSize.LARGE to "Grand"
                        )
                        sizes.forEach { pair ->
                            val size = pair.first
                            val label = pair.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.setTextSize(size)
                                        showTextSizeDialog = false 
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.textSize == size,
                                    onClick = { 
                                        viewModel.setTextSize(size)
                                        showTextSizeDialog = false 
                                    }
                                )
                                Text(text = label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTextSizeDialog = false }) {
                        Text("Annuler")
                    }
                }
            )
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("À propos") },
                text = {
                    Column {
                        Text("Mibeko - Mobile", fontWeight = FontWeight.Bold)
                        Text("Version: ${uiState.appVersion}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Mibeko est une plateforme juridique centralisant les textes de loi de la République du Congo.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Base de données mise à jour le: ${uiState.lastUpdateDate}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) {
                        Text("Fermer")
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Réglages", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // --- APPARENCE ---
                SettingsGroup("APPARENCE") {
                    SettingsItem(
                        title = "Thème", 
                        subtitle = when(uiState.currentTheme) {
                            UserPreferencesRepository.AppTheme.SYSTEM -> "Système"
                            UserPreferencesRepository.AppTheme.LIGHT -> "Clair"
                            UserPreferencesRepository.AppTheme.DARK -> "Sombre"
                        }, 
                        icon = Icons.Filled.Contrast,
                        onClick = { showThemeDialog = true }
                    )
                    SettingsItem(
                        title = "Taille du texte", 
                        subtitle = when(uiState.textSize) {
                            UserPreferencesRepository.TextSize.SMALL -> "Petit"
                            UserPreferencesRepository.TextSize.MEDIUM -> "Moyen"
                            UserPreferencesRepository.TextSize.LARGE -> "Grand"
                        }, 
                        icon = Icons.Filled.FormatSize,
                        onClick = { showTextSizeDialog = true }
                    )
                    SettingsSwitch(
                        title = "Police Dyslexie", 
                        subtitle = if (uiState.isDyslexiaFontEnabled) "Activé" else "Désactivé", 
                        icon = Icons.Filled.Abc,
                        checked = uiState.isDyslexiaFontEnabled,
                        onCheckedChange = { viewModel.setDyslexiaFontEnabled(it) }
                    )
                }
                
                // --- NOTIFICATIONS ---
                SettingsGroup("NOTIFICATIONS") {
                    SettingsSwitch(
                        title = "Autoriser les notifications", 
                        subtitle = if (uiState.isNotificationsEnabled) "Activé" else "Désactivé", 
                        icon = Icons.Filled.Notifications,
                        checked = uiState.isNotificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    
                    if (uiState.isNotificationsEnabled) {
                        SettingsSwitch(
                            title = "Veille Juridique", 
                            subtitle = "Recevoir les nouveautés juridiques", 
                            icon = Icons.Filled.Gavel,
                            checked = uiState.isLegalMonitoringEnabled,
                            onCheckedChange = { viewModel.setLegalMonitoringEnabled(it) }
                        )
                        SettingsSwitch(
                            title = "Alertes Dossiers", 
                            subtitle = "Mises à jour de vos dossiers", 
                            icon = Icons.Filled.FolderSpecial,
                            checked = uiState.isDossierAlertsEnabled,
                            onCheckedChange = { viewModel.setDossierAlertsEnabled(it) }
                        )
                    }
                }
                
                // --- DONNÉES ---
                SettingsGroup("DONNÉES") {
                    SettingsItem(
                        title = "Gérer le stockage", 
                        subtitle = uiState.diskUsage, 
                        icon = Icons.Filled.Storage,
                        onClick = { navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Downloads) }
                    )
                }
                
                // --- À PROPOS ---
                SettingsGroup("À PROPOS") {
                    SettingsItem(
                        title = "Conditions d'utilisation", 
                        subtitle = "", 
                        icon = Icons.Filled.Description,
                        onClick = { showTerms = true }
                    )
                    SettingsItem(
                        title = "Confidentialité", 
                        subtitle = "", 
                        icon = Icons.Filled.PrivacyTip,
                        onClick = { showPrivacy = true }
                    )
                    SettingsItem(
                        title = "Version de l'application", 
                        subtitle = uiState.appVersion, 
                        icon = Icons.Filled.Info,
                        onClick = { showAbout = true }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "© 2026 Mibeko - République du Congo",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


/**
 * Groupe de réglages avec un titre.
 */
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

/**
 * Élément de réglage simple avec icône, titre et sous-titre.
 */
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
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

/**
 * Élément de réglage avec un commutateur (switch).
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
 * Élément affichant un document téléchargeable avec actions de téléchargement/suppression.
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
                    "CODE" -> Icons.AutoMirrored.Filled.MenuBook
                    "LOI" -> Icons.Filled.Gavel
                    "DECRET" -> Icons.Filled.Description
                    else -> Icons.AutoMirrored.Filled.Article
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
                        Icons.Filled.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Filled.CloudDownload,
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
