package com.mibeko.mibeko.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.ui.components.OfficialSourcesSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * Contenu principal de l'écran de profil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
    val viewModel = koinViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    val isAuthenticated = uiState.userName.isNotEmpty() || uiState.userEmail.isNotEmpty()
    
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showApparenceDialog by remember { mutableStateOf(false) }
    var showOfflineDataDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.profileUpdateMessage) {
        uiState.profileUpdateMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearProfileUpdateMessage()
        }
    }

    LaunchedEffect(uiState.passwordUpdateMessage) {
        uiState.passwordUpdateMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPasswordUpdateMessage()
            if (it == "Mot de passe mis à jour avec succès") {
                showPasswordDialog = false
            }
        }
    }

    // Erreurs de téléchargement / suppression de documents hors-ligne.
    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncError()
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Informations personnelles") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = uiState.userName,
                        onValueChange = { viewModel.updateProfileField("name", it) },
                        label = { Text("Nom complet") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.updateProfileField("phone", it) },
                        label = { Text("Numéro de téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.profession,
                        onValueChange = { viewModel.updateProfileField("profession", it) },
                        label = { Text("Profession") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.company,
                        onValueChange = { viewModel.updateProfileField("company", it) },
                        label = { Text("Entreprise / Institution") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.saveProfile()
                        showProfileDialog = false 
                    },
                    enabled = !uiState.isUpdatingProfile
                ) {
                    if (uiState.isUpdatingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Enregistrer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Changer le mot de passe") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = uiState.currentPassword,
                        onValueChange = { viewModel.updatePasswordField("currentPassword", it) },
                        label = { Text("Mot de passe actuel") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.newPassword,
                        onValueChange = { viewModel.updatePasswordField("newPassword", it) },
                        label = { Text("Nouveau mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { viewModel.updatePasswordField("confirmPassword", it) },
                        label = { Text("Confirmer le mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.updatePassword()
                    },
                    enabled = !uiState.isUpdatingPassword && uiState.newPassword.isNotBlank()
                ) {
                    if (uiState.isUpdatingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Enregistrer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                            subtitle = "Recevoir les nouveautés", 
                            icon = Icons.Filled.Gavel,
                            checked = uiState.isLegalMonitoringEnabled,
                            onCheckedChange = { viewModel.setLegalMonitoringEnabled(it) }
                        )
                        SettingsSwitch(
                            title = "Alertes Dossiers", 
                            subtitle = "Mises à jour", 
                            icon = Icons.Filled.FolderSpecial,
                            checked = uiState.isDossierAlertsEnabled,
                            onCheckedChange = { viewModel.setDossierAlertsEnabled(it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showApparenceDialog) {
        AlertDialog(
            onDismissRequest = { showApparenceDialog = false },
            title = { Text("Apparence") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Thème", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    val themes = listOf(
                        UserPreferencesRepository.AppTheme.SYSTEM to "Système",
                        UserPreferencesRepository.AppTheme.LIGHT to "Clair",
                        UserPreferencesRepository.AppTheme.DARK to "Sombre"
                    )
                    themes.forEach { (theme, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setTheme(theme) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.currentTheme == theme,
                                onClick = { viewModel.setTheme(theme) }
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Taille du texte", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    val sizes = listOf(
                        UserPreferencesRepository.TextSize.SMALL to "Petit",
                        UserPreferencesRepository.TextSize.MEDIUM to "Moyen",
                        UserPreferencesRepository.TextSize.LARGE to "Grand"
                    )
                    sizes.forEach { (size, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setTextSize(size) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.textSize == size,
                                onClick = { viewModel.setTextSize(size) }
                            )
                            Text(text = label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    SettingsSwitch(
                        title = "Police Dyslexie", 
                        subtitle = "", 
                        icon = Icons.Filled.Abc,
                        checked = uiState.isDyslexiaFontEnabled,
                        onCheckedChange = { viewModel.setDyslexiaFontEnabled(it) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showApparenceDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showOfflineDataDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDataDialog = false },
            title = { Text("Données hors ligne") },
            text = {
                Column {
                    Text("Espace utilisé : ${uiState.diskUsage}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Dernière mise à jour : ${uiState.lastUpdateDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Le corpus évolue (textes corrigés, nouveaux textes
                    // publiés) : sans ce bouton, un téléphone gardait sa copie
                    // du premier jour indéfiniment.
                    Button(
                        onClick = { viewModel.refreshCorpus() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isSyncing) "Mise à jour…" else "Mettre à jour le corpus")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showOfflineDataDialog = false
                            navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Downloads)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gérer le stockage")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOfflineDataDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Langue") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = false }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = true, onClick = { showLanguageDialog = false })
                        Text("Français", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("À propos") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Mibeko - Mobile", fontWeight = FontWeight.Bold)
                    Text("Version: ${uiState.appVersion}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mibeko est une plateforme juridique centralisant les textes de loi de la République du Congo.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Base de données mise à jour le: ${uiState.lastUpdateDate}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Avertissement :", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(
                        "Mibeko est une initiative privée et indépendante. Cette application ne représente aucune entité gouvernementale de la République du Congo. " +
                        "Les informations fournies proviennent de sources officielles publiques (sgg.cg, ohada.org) mais sont fournies à titre informatif uniquement.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showSources) {
        OfficialSourcesSheet(onDismiss = { showSources = false })
    }

    if (showDeleteAccountDialog) {
        var deletePassword by remember { mutableStateOf("") }
        var deleteError by remember { mutableStateOf<String?>(null) }
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteAccountDialog = false },
            title = { Text("Supprimer mon compte") },
            text = {
                Column {
                    Text(
                        "Cette action est définitive : votre compte, vos dossiers synchronisés et votre historique seront supprimés. " +
                            "Confirmez avec votre mot de passe."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("Mot de passe actuel") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        isError = deleteError != null,
                        supportingText = deleteError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        deleteError = null
                        viewModel.deleteAccount(
                            currentPassword = deletePassword,
                            onSuccess = {
                                showDeleteAccountDialog = false
                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Home) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onError = { message ->
                                isDeleting = false
                                deleteError = message
                            }
                        )
                    },
                    enabled = deletePassword.isNotBlank() && !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text("Supprimer définitivement")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }, enabled = !isDeleting) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Déconnexion") },
            text = { Text("Êtes-vous sûr de vouloir vous déconnecter ?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout {
                            navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Home) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Se déconnecter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil", fontWeight = FontWeight.Bold) },
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
            
            // --- EN-TÊTE PROFIL ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isAuthenticated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = if (isAuthenticated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isAuthenticated) Icons.Filled.Edit else Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isAuthenticated) {
                    Text(
                        text = uiState.userName.ifEmpty { "Utilisateur" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = uiState.userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Modifier le profil", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Text(
                        text = "Utilisateur Invité",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Limited access mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = { navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Login()) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Se connecter", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = { navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Register) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Créer un compte", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // --- COMPTE ---
            if (isAuthenticated) {
                SettingsGroup("COMPTE") {
                    SettingsItem(
                        title = "Informations personnelles",
                        icon = Icons.Filled.Person,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { showProfileDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsItem(
                        title = "Sécurité & Mot de passe",
                        icon = Icons.Filled.Security,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { showPasswordDialog = true }
                    )
                }
            }

            // --- PRÉFÉRENCES ---
            SettingsGroup("PRÉFÉRENCES") {
                SettingsItem(
                    title = "Langue",
                    subtitle = "Français",
                    icon = Icons.Filled.Language,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    title = "Notifications",
                    icon = Icons.Filled.Notifications,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { showNotificationsDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    title = "Apparence",
                    icon = Icons.Filled.Palette,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { showApparenceDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    title = "Donnée Hors ligne",
                    icon = Icons.Filled.CloudOff,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { showOfflineDataDialog = true }
                )
            }

            // --- CONFIDENTIALITÉ ---
            SettingsGroup("CONFIDENTIALITÉ") {
                SettingsSwitch(
                    title = "Partage de statistiques anonymes",
                    subtitle = "Aide à améliorer Mibeko. Jamais le contenu de vos recherches.",
                    icon = Icons.Filled.Insights,
                    checked = uiState.isTelemetryEnabled,
                    onCheckedChange = { viewModel.setTelemetryEnabled(it) }
                )
                if (isAuthenticated) {
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    // Droit d'accès annoncé par la politique de confidentialité
                    // publiée sur mibeko.fr (« Profil → Exporter mes données »).
                    SettingsItem(
                        title = "Exporter mes données",
                        subtitle = if (uiState.isExportingData) "Préparation…" else "Recevoir une copie de vos données personnelles",
                        icon = Icons.Filled.Download,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                        onClick = { viewModel.exportPersonalData() }
                    )
                }
            }

            // --- A PROPOS DE MIBEKO ---
            SettingsGroup("A PROPOS DE MIBEKO") {
                SettingsItem(
                    title = "Sources officielles",
                    icon = Icons.Filled.VerifiedUser,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    onClick = { showSources = true }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                // Textes de référence hébergés sur mibeko.fr : une seule source
                // de vérité, alignée sur les déclarations Data Safety des stores.
                SettingsLinkItem(
                    title = "Conditions d'utilisation",
                    onClick = { uriHandler.openUri("https://mibeko.fr/cgu") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsLinkItem(
                    title = "Politique de confidentialité",
                    onClick = { uriHandler.openUri("https://mibeko.fr/confidentialite") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsLinkItem(
                    title = "Nous contacter",
                    onClick = { navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Contact) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsLinkItem(
                    title = "À propos de Mibeko",
                    onClick = { showAbout = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DÉCONNEXION ---
            if (isAuthenticated) {
                Button(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Se déconnecter", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Supprimer mon compte",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Text(
                text = "MIBEKO ${uiState.appVersion.uppercase()}",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String = "",
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        // Le sous-titre vit SOUS le titre, dans la colonne pondérée : posé en
        // frère non pondéré dans la Row, un sous-titre long consommait toute la
        // largeur et écrasait le titre à ~0 (rendu vertical, une lettre par ligne).
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsLinkItem(
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

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
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
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
    
    
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

}
}
