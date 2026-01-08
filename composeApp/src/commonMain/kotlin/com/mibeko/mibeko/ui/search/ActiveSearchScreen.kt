package com.mibeko.mibeko.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full-screen active search interface.
 * Features auto-focus, recent searches, and live suggestions.
 */
class ActiveSearchScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navController = com.mibeko.mibeko.ui.navigation.LocalNavController.current
        val viewModel = koinViewModel<SearchViewModel>()
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        
        var searchText by remember { mutableStateOf("") }
        val recentSearches by viewModel.recentSearches.collectAsState()
        val suggestions by viewModel.suggestions.collectAsState()
        
        // Auto-focus the text field on screen open
        LaunchedEffect(Unit) {
            delay(100) // Small delay to ensure composition is complete
            focusRequester.requestFocus()
        }
        
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        TextField(
                            value = searchText,
                            onValueChange = { 
                                searchText = it
                                viewModel.updateLiveQuery(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    "Rechercher...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchText.isNotBlank()) {
                                        focusManager.clearFocus()
                                        viewModel.saveSearch(searchText)
                                        navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.SearchResults(searchText)) {
                                            popUpTo(com.mibeko.mibeko.ui.navigation.Screen.ActiveSearch) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        )
                        
                        AnimatedVisibility(
                            visible = searchText.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(onClick = { 
                                searchText = ""
                                viewModel.updateLiveQuery("")
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Effacer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Live suggestions (when typing) - Navigate directly to article
                if (searchText.isNotBlank() && suggestions.isNotEmpty()) {
                    item {
                        Text(
                            "Suggestions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(suggestions.take(5)) { suggestion ->
                        ArticleSuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                viewModel.saveSearch("${suggestion.number} - ${suggestion.breadcrumb}")
                                // Navigate directly to article reader
                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.Reader(suggestion.id)) {
                                    popUpTo(com.mibeko.mibeko.ui.navigation.Screen.ActiveSearch) { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
                
                // Recent searches
                if (searchText.isBlank() && recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recherches récentes",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text("Effacer", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    items(recentSearches) { query ->
                        SuggestionItem(
                            text = query,
                            icon = Icons.Filled.History,
                            onClick = {
                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.SearchResults(query)) {
                                    popUpTo(com.mibeko.mibeko.ui.navigation.Screen.ActiveSearch) { inclusive = true }
                                }
                            },
                            onDelete = { viewModel.removeFromHistory(query) }
                        )
                    }
                }
                
                // Empty state
                if (searchText.isBlank() && recentSearches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Recherchez un article, une loi ou un thème",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * New composable for ArticleSuggestion items with source indicators.
 * Shows article number, breadcrumb, and an icon indicating if downloaded.
 */
@Composable
private fun ArticleSuggestionItem(
    suggestion: com.mibeko.mibeko.data.ArticleSuggestion,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source indicator icon
            Icon(
                imageVector = if (suggestion.isDownloaded) 
                    Icons.Filled.CheckCircle 
                else 
                    Icons.Filled.Cloud,
                contentDescription = if (suggestion.isDownloaded) 
                    "Disponible hors-ligne" 
                else 
                    "En ligne",
                tint = if (suggestion.isDownloaded) 
                    MaterialTheme.colorScheme.secondary 
                else 
                    MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Article ${suggestion.number}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = suggestion.breadcrumb,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ouvrir",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SuggestionItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
