package com.mibeko.mibeko.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import cafe.adriel.voyager.core.screen.Screen
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import com.mibeko.mibeko.ui.components.MibekoBreadcrumb
import com.mibeko.mibeko.ui.components.BreadcrumbSegment

data class ReaderScreen(val articleId: String) : Screen {

    /**
     * Contenu principal de l'écran de lecture d'un article.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<ReaderViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val article by viewModel.article.collectAsState()
        val textSize by viewModel.textSize.collectAsState()
        val isDyslexiaFontEnabled by viewModel.isDyslexiaFontEnabled.collectAsState()
        
        // Reading theme state (Internal for MVP, could be in VM)
        var readerTheme by remember { mutableStateOf("paper") } // "white", "paper", "dark"
        var showSettings by remember { mutableStateOf(false) }

        LaunchedEffect(articleId) {
            viewModel.loadArticle(articleId)
            viewModel.refreshPreferences()
        }

        // Apply colors based on reader theme
        val backgroundColor = when(readerTheme) {
            "paper" -> Color(0xFFF4ECD8)
            "dark" -> Color(0xFF121212)
            else -> Color.White
        }
        
        val textColor = when(readerTheme) {
            "dark" -> Color(0xFFE0E0E0)
            else -> Color(0xFF1A1A1A)
        }

        // Apply text size multiplier
        val fontSizeValue = when(textSize) {
            UserPreferencesRepository.TextSize.SMALL -> 15.sp
            UserPreferencesRepository.TextSize.MEDIUM -> 18.sp
            UserPreferencesRepository.TextSize.LARGE -> 22.sp
        }
        
        val lineSpacingValue = when(textSize) {
            UserPreferencesRepository.TextSize.SMALL -> 26.sp
            UserPreferencesRepository.TextSize.MEDIUM -> 32.sp
            UserPreferencesRepository.TextSize.LARGE -> 38.sp
        }

        // Show loading state
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Chargement de l'article...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
            return
        }

        // Show error state if article not found
        if (uiState.error != null || article == null) {
            Box(modifier = Modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        uiState.error ?: "Article non trouvé",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Retour")
                    }
                }
            }
            return
        }

        val currentArticle = article!!

        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Article ${currentArticle.number}", 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = textColor)
                            }
                        },
                        actions = {
                            // Offline Toggle
                            IconButton(onClick = { viewModel.toggleOffline() }) {
                                Icon(
                                    if (currentArticle.isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                    contentDescription = "Hors-ligne",
                                    tint = if (currentArticle.isDownloaded) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                                )
                            }
                            // Favorite Toggle
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    if (currentArticle.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Favori",
                                    tint = if (currentArticle.isFavorite) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                                )
                            }
                            // Share
                            IconButton(onClick = { 
                                viewModel.shareArticle()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Partager", tint = textColor)
                            }
                            
                            // PDF Export
                            IconButton(onClick = { 
                                viewModel.shareArticle()
                            }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Partager PDF", tint = textColor)
                            }

                            
                            // Settings
                            IconButton(onClick = { showSettings = true }) { 
                                Icon(Icons.Default.FormatSize, contentDescription = "Paramètres de lecture", tint = textColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor
                        )
                    )
                    
                    // Dynamic Breadcrumb for Reader
                    val breadcrumbSegments = listOf(
                        BreadcrumbSegment("Bibliothèque") { navController.popBackStack() },
                        BreadcrumbSegment(uiState.documentType ?: "Norme") { navController.popBackStack() },
                        BreadcrumbSegment(uiState.documentTitle ?: "Document") { navController.popBackStack() },
                        BreadcrumbSegment("Art. ${currentArticle.number}") { }
                    )
                    
                    MibekoBreadcrumb(
                        segments = breadcrumbSegments,
                        modifier = Modifier.background(backgroundColor)
                    )
                    
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                }
            },
            floatingActionButton = {
                 // Removed FAB if all actions are in top bar, or maybe a simple "Share" FAB?
                 // Keeping it clean for now.
            },
            containerColor = backgroundColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Article title in caps
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentArticle.breadcrumb.split(">").last().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
                
                // Article content with optimized typography
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentArticle.content ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSizeValue,
                            lineHeight = lineSpacingValue,
                            letterSpacing = if (isDyslexiaFontEnabled) 0.8.sp else 0.3.sp,
                            fontWeight = if (isDyslexiaFontEnabled) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = textColor
                    )
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }

            if (showSettings) {
                 ModalBottomSheet(
                     onDismissRequest = { showSettings = false },
                     containerColor = MaterialTheme.colorScheme.surface
                 ) {
                     ReaderSettingsSheet(
                        currentTextSize = textSize,
                        onTextSizeChange = { viewModel.setTextSize(it) },
                        currentTheme = readerTheme,
                        onThemeChange = { readerTheme = it },
                        isDyslexiaEnabled = isDyslexiaFontEnabled,
                        onDyslexiaChange = { viewModel.setDyslexiaFontEnabled(it) }
                     )
                 }
            }
        }
    }
}

@Composable
fun ReaderSettingsSheet(
    currentTextSize: UserPreferencesRepository.TextSize,
    onTextSizeChange: (UserPreferencesRepository.TextSize) -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    isDyslexiaEnabled: Boolean,
    onDyslexiaChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Paramètres de lecture", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        
        // Text Size
        Column {
             Text("Taille du texte", fontWeight = FontWeight.Medium)
             Spacer(modifier = Modifier.height(8.dp))
             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
             ) {
                 Text("Petit", fontSize = 14.sp)
                 androidx.compose.material3.Slider(
                     value = when(currentTextSize) {
                         UserPreferencesRepository.TextSize.SMALL -> 0f
                         UserPreferencesRepository.TextSize.MEDIUM -> 0.5f
                         UserPreferencesRepository.TextSize.LARGE -> 1f
                     },
                     onValueChange = { 
                         val newSize = when {
                             it < 0.25f -> UserPreferencesRepository.TextSize.SMALL
                             it > 0.75f -> UserPreferencesRepository.TextSize.LARGE
                             else -> UserPreferencesRepository.TextSize.MEDIUM
                         }
                         onTextSizeChange(newSize)
                     },
                     steps = 1,
                     modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                 )
                 Text("Grand", fontSize = 20.sp)
             }
        }
        
        // Theme
        Column {
            Text("Thème", fontWeight = FontWeight.Medium)
             Spacer(modifier = Modifier.height(8.dp))
             Row(
                 horizontalArrangement = Arrangement.spacedBy(16.dp)
             ) {
                 ThemeOption(
                     name = "Papier",
                     color = Color(0xFFF4ECD8),
                     isSelected = currentTheme == "paper",
                     onClick = { onThemeChange("paper") }
                 )
                 ThemeOption(
                     name = "Clair",
                     color = Color.White,
                     isSelected = currentTheme == "white",
                     onClick = { onThemeChange("white") }
                 )
                 ThemeOption(
                     name = "Sombre",
                     color = Color(0xFF121212),
                     selectionColor = Color.White,
                     isSelected = currentTheme == "dark",
                     onClick = { onThemeChange("dark") }
                 )
             }
        }
        
        // Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Police Dyslexie")
            Switch(
                checked = isDyslexiaEnabled,
                onCheckedChange = onDyslexiaChange
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ThemeOption(
    name: String,
    color: Color,
    isSelected: Boolean,
    selectionColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.background(color).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier.background(color).border(1.dp, Color.Gray, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 12.sp)
    }
}

