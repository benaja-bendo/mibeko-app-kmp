package com.mibeko.mibeko.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow

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
                                currentArticle.number, 
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
                            IconButton(onClick = { /* Open search */ }) { 
                                Icon(Icons.Filled.Search, contentDescription = "Rechercher", tint = textColor)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = backgroundColor
                        )
                    )
                    
                    // Functional Breadcrumb
                    BreadcrumbBar(
                        breadcrumb = currentArticle.breadcrumb,
                        textColor = textColor,
                        onNavigate = { /* TODO: Implement back navigation to levels */ }
                    )
                }
            },
            bottomBar = {
                Column {
                    // Reader Controls Bar (Aa, TT, theme, slider)
                    ReaderControlsBar(
                        backgroundColor = backgroundColor,
                        textColor = textColor,
                        currentTextSize = textSize,
                        onTextSizeChange = { viewModel.setTextSize(it) },
                        currentTheme = readerTheme,
                        onThemeChange = { readerTheme = it }
                    )
                    
                    // Main Action Bar
                    Surface(
                        color = backgroundColor,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { /* Navigate previous */ },
                                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Précédent", color = textColor, style = MaterialTheme.typography.labelMedium)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { /* Source file */ }) {
                                    Icon(Icons.Default.Description, contentDescription = "Source", tint = textColor)
                                }
                                IconButton(onClick = { /* Share */ }) {
                                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = textColor)
                                }
                                IconButton(onClick = { /* Favorite */ }) {
                                    Icon(
                                        if (currentArticle.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Favori",
                                        tint = if (currentArticle.isFavorite) MaterialTheme.colorScheme.primary else textColor
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { /* Navigate next */ },
                                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Suivant", color = textColor, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
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
        }
    }
}

/**
 * Functional Breadcrumb Bar
 */
@Composable
private fun BreadcrumbBar(
    breadcrumb: String,
    textColor: Color,
    onNavigate: (String) -> Unit
) {
    val levels = breadcrumb.split(">")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(textColor.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.forEachIndexed { index, level ->
            Text(
                text = level.trim(),
                style = MaterialTheme.typography.labelSmall,
                color = if (index == levels.lastIndex) textColor else textColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onNavigate(level) }
            )
            if (index < levels.lastIndex) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = textColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Reader Controls Bar (Aa, TT, theme, slider)
 */
@Composable
private fun ReaderControlsBar(
    backgroundColor: Color,
    textColor: Color,
    currentTextSize: UserPreferencesRepository.TextSize,
    onTextSizeChange: (UserPreferencesRepository.TextSize) -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress Slider (Simulated)
                Slider(
                    value = 0.3f,
                    onValueChange = {},
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = textColor.copy(alpha = 0.8f),
                        activeTrackColor = textColor.copy(alpha = 0.5f),
                        inactiveTrackColor = textColor.copy(alpha = 0.1f)
                    )
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text Size (TT)
                IconButton(onClick = { 
                    val nextSize = when(currentTextSize) {
                        UserPreferencesRepository.TextSize.SMALL -> UserPreferencesRepository.TextSize.MEDIUM
                        UserPreferencesRepository.TextSize.MEDIUM -> UserPreferencesRepository.TextSize.LARGE
                        UserPreferencesRepository.TextSize.LARGE -> UserPreferencesRepository.TextSize.SMALL
                    }
                    onTextSizeChange(nextSize)
                }) {
                    Icon(Icons.Default.TextFields, contentDescription = "Taille", tint = textColor)
                }
                
                // Theme (Aa)
                IconButton(onClick = { 
                    val nextTheme = when(currentTheme) {
                        "white" -> "paper"
                        "paper" -> "dark"
                        else -> "white"
                    }
                    onThemeChange(nextTheme)
                }) {
                    Icon(Icons.Default.FormatSize, contentDescription = "Thème", tint = textColor)
                }
                
                // Dyslexia switch (simplified)
                Switch(
                    checked = currentTheme == "paper",
                    onCheckedChange = { onThemeChange(if(it) "paper" else "white") },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Bouton d'action pour le lecteur d'article (Précédent, Favori, Partager, Suivant).
 */
@Composable
private fun ReaderActionButton(
    icon: ImageVector,
    label: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) 
            MaterialTheme.colorScheme.secondaryContainer
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200)
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isHighlighted) 
            MaterialTheme.colorScheme.secondary
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200)
    )
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = backgroundColor,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon, 
                    contentDescription = label, 
                    modifier = Modifier.size(22.dp), 
                    tint = iconColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

