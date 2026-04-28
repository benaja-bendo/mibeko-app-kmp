package com.mibeko.mibeko.ui.officialjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import com.mibeko.mibeko.ui.theme.MibekoBluePrimary
import com.mibeko.mibeko.util.formatIsoDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialJournalListScreen() {
    val navController = LocalNavController.current
    val viewModel = koinViewModel<OfficialJournalViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var filterNumber by remember { mutableStateOf("") }
    var filterYear by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadJournals()
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filtrer les journaux", style = MaterialTheme.typography.titleLarge)
                
                OutlinedTextField(
                    value = filterNumber,
                    onValueChange = { filterNumber = it },
                    label = { Text("Numéro du journal") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = filterYear,
                    onValueChange = { filterYear = it },
                    label = { Text("Année de publication") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        filterNumber = ""
                        filterYear = ""
                        viewModel.loadJournals(page = 1, number = null, year = null)
                        showFilterSheet = false
                    }) {
                        Text("Réinitialiser")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val n = filterNumber.takeIf { it.isNotBlank() }
                        val y = filterYear.toIntOrNull()
                        viewModel.loadJournals(page = 1, number = n, year = y)
                        showFilterSheet = false
                    }) {
                        Text("Appliquer")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal Officiel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading && uiState.journals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MibekoBluePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.journals) { journal ->
                    JournalListItem(
                        title = journal.title,
                        number = journal.number,
                        date = "Publié le ${formatIsoDate(journal.publication_date)}",
                        onClick = { navController.navigate(Screen.OfficialJournalDetail(journal.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalListItem(title: String, number: String?, date: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.background(MibekoBluePrimary.copy(alpha = 0.05f))) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MibekoBluePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!number.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "N° $number",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MibekoBluePrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}