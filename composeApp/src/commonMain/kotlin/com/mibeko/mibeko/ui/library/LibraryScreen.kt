package com.mibeko.mibeko.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.preferences.RecentlyViewedItem
import com.mibeko.mibeko.ui.components.HighlightedText
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val navController = LocalNavController.current
    val libraryViewModel = koinViewModel<LibraryViewModel>()
    
    val libraryState by libraryViewModel.uiState.collectAsState()
    val recentItems by libraryViewModel.recentItems.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }

    var activeFilterSheet by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Header with Search and Filters
            LibraryHeader(
                searchQuery = libraryState.searchQuery,
                onSearchChange = { libraryViewModel.updateSearchQuery(it) },
                currentType = libraryState.selectedType,
                currentInstitution = libraryState.selectedInstitution,
                currentYear = libraryState.selectedYear,
                onOpenFilterSheet = { activeFilterSheet = it },
                onClearFilters = {
                    libraryViewModel.updateTypeFilter(null)
                    libraryViewModel.updateInstitutionFilter(null)
                    libraryViewModel.updateYearFilter(null)
                },
                onNotificationsClick = { navController.navigate(Screen.Notifications) }
            )
            
            // Body Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Only show "Nouveautés" and "Récents" if NO filters are active
                if (libraryState.selectedType == null && libraryState.selectedInstitution == null && libraryState.selectedYear == null && libraryState.searchQuery.isEmpty()) {
                    item {
                        if (libraryState.latestDocuments.isNotEmpty()) {
                            SectionTitle(
                                title = "NOUVEAUTÉS LÉGISLATIVES",
                                badgeText = "NOUVELLES ALERTES",
                                showDot = true
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(libraryState.latestDocuments) { doc ->
                                    NouveauteCard(
                                        title = doc.title,
                                        subtitle = "Mise à jour récente des textes applicables.",
                                        tag = doc.type,
                                        onClick = { navController.navigate(Screen.DocumentDetail(doc.id)) },
                                        onAiSummary = { navController.navigate(Screen.Chat(initialPrompt = "Fais-moi un résumé de: ${doc.title}")) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    item {
                        if (recentItems.isNotEmpty()) {
                            SectionTitle(
                                title = "RÉCEMMENT CONSULTÉS",
                                actionText = "VOIR TOUT",
                                onActionClick = { navController.navigate(Screen.ConversationHistory) } 
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recentItems) { item ->
                                    RecentCard(
                                        item = item,
                                        onClick = { navController.navigate(Screen.Reader(item.id)) } 
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                item {
                    val titleText = if (libraryState.searchQuery.isNotEmpty()) {
                        "RÉSULTATS DE LA RECHERCHE"
                    } else if (libraryState.selectedType != null || libraryState.selectedInstitution != null || libraryState.selectedYear != null) {
                        "RÉSULTATS FILTRÉS"
                    } else {
                        "TOUS LES DOCUMENTS"
                    }
                    SectionTitle(title = titleText)
                }

                items(libraryState.filteredDocuments) { doc ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        DocumentVerticalCard(
                            document = doc,
                            onClick = { navController.navigate(Screen.DocumentDetail(doc.id)) }
                        )
                    }
                }

                if (libraryState.filteredDocuments.isEmpty() && !libraryState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (libraryState.searchQuery.isNotEmpty()) "Aucun document ne correspond à votre recherche" else "Aucun document ne correspond à vos critères", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeFilterSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeFilterSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = when(activeFilterSheet) {
                        "type" -> "Sélectionner un type"
                        "institution" -> "Sélectionner une institution"
                        "year" -> "Sélectionner une année"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                LazyColumn {
                    item {
                        FilterOptionRow(
                            text = "Tous",
                            isSelected = when(activeFilterSheet) {
                                "type" -> libraryState.selectedType == null
                                "institution" -> libraryState.selectedInstitution == null
                                "year" -> libraryState.selectedYear == null
                                else -> false
                            },
                            onClick = {
                                when(activeFilterSheet) {
                                    "type" -> libraryViewModel.updateTypeFilter(null)
                                    "institution" -> libraryViewModel.updateInstitutionFilter(null)
                                    "year" -> libraryViewModel.updateYearFilter(null)
                                }
                                activeFilterSheet = null
                            }
                        )
                    }
                    
                    val options = when(activeFilterSheet) {
                        "type" -> libraryState.documentTypes.map { it.name to it.code }
                        "institution" -> libraryState.institutions.map { it to it }
                        "year" -> libraryState.years.map { it to it }
                        else -> emptyList()
                    }
                    
                    items(options) { (label, value) ->
                        FilterOptionRow(
                            text = label,
                            isSelected = when(activeFilterSheet) {
                                "type" -> libraryState.selectedType == value
                                "institution" -> libraryState.selectedInstitution == value
                                "year" -> libraryState.selectedYear == value
                                else -> false
                            },
                            onClick = {
                                when(activeFilterSheet) {
                                    "type" -> libraryViewModel.updateTypeFilter(value)
                                    "institution" -> libraryViewModel.updateInstitutionFilter(value)
                                    "year" -> libraryViewModel.updateYearFilter(value)
                                }
                                activeFilterSheet = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MibekoBluePrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MibekoBluePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DocumentVerticalCard(
    document: LawCodeSpec,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MibekoBluePrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = document.type,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MibekoBluePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                if (document.isDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Disponible hors-ligne",
                        tint = LegalValid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!document.institutionName.isNullOrEmpty()) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = document.institutionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                
                if (!document.institutionName.isNullOrEmpty() && !document.dateSignature.isNullOrEmpty()) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!document.dateSignature.isNullOrEmpty()) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = document.dateSignature.take(4), // Display year
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currentType: String?,
    currentInstitution: String?,
    currentYear: String?,
    onOpenFilterSheet: (String) -> Unit,
    onClearFilters: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MibekoBlueDark)
            .statusBarsPadding()
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BIBLIOTHÈQUE MIBEKO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                
                IconButton(onClick = onNotificationsClick) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = LegalRepealed)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { 
                    Text("Rechercher une loi, un arrêt, un...", color = Color.White.copy(alpha = 0.5f)) 
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterDropdownChip(
                        label = currentType ?: "Type",
                        isSelected = currentType != null,
                        onClick = { onOpenFilterSheet("type") }
                    )
                }
                item {
                    FilterDropdownChip(
                        label = currentInstitution ?: "Institution",
                        isSelected = currentInstitution != null,
                        onClick = { onOpenFilterSheet("institution") }
                    )
                }
                item {
                    FilterDropdownChip(
                        label = currentYear ?: "Année",
                        isSelected = currentYear != null,
                        onClick = { onOpenFilterSheet("year") }
                    )
                }
                if (currentType != null || currentInstitution != null || currentYear != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.clickable { onClearFilters() }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Effacer filtres",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp).size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MibekoBluePrimary else Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, badgeText: String? = null, showDot: Boolean = false, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MibekoBlueDark,
                letterSpacing = 0.5.sp
            )
            if (showDot) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(6.dp).background(LegalRepealed, CircleShape))
            }
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelSmall,
                color = MibekoGoldDark,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
private fun NouveauteCard(
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    onAiSummary: () -> Unit
) {
    Surface(
        modifier = Modifier.width(300.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column {
            // Top Red Border Indicator
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(LegalRepealed))
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = LegalRepealed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "RÉCENT • $tag",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LegalRepealed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MibekoBlueDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAiSummary,
                        colors = ButtonDefaults.buttonColors(containerColor = MibekoBlueDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RÉSUMÉ IA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { /* Dismiss */ },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IGNORER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCard(item: RecentlyViewedItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(200.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MibekoBluePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.typeCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Consulté récemment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
