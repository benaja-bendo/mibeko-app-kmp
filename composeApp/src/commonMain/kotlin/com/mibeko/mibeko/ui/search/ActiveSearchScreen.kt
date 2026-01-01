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
                                    Icons.Default.Close,
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
                // Live suggestions (when typing)
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
                        SuggestionItem(
                            text = suggestion,
                            icon = Icons.AutoMirrored.Filled.Article,
                            onClick = {
                                viewModel.saveSearch(suggestion)
                                navController.navigate(com.mibeko.mibeko.ui.navigation.Screen.SearchResults(suggestion)) {
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
                            icon = Icons.Default.History,
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
                                    Icons.Default.Search,
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
                        Icons.Default.Close,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
