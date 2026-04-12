package com.mibeko.mibeko.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.data.remote.AgentConversation
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatIsoDate(isoString: String): String {
    try {
        val instant = Instant.parse(isoString)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val year = dateTime.year
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        return "$day/$month/$year à $hour:$minute"
    } catch (e: Exception) {
        return isoString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen() {
    val navController = LocalNavController.current
    val viewModel: ConversationHistoryViewModel = koinViewModel()
    val state by viewModel.historyState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des conversations") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val currentState = state) {
                is HistoryState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HistoryState.Error -> {
                    Text(
                        "Erreur de chargement de l'historique.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is HistoryState.Content -> {
                    if (currentState.conversations.isEmpty()) {
                        Text(
                            "Aucune conversation trouvée.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentState.conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    onClick = {
                                        navController.navigate(Screen.Chat(conversationId = conversation.id))
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteConversation(conversation.id)
                                    }
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
fun ConversationItem(
    conversation: AgentConversation,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Supprimer la conversation") },
            text = { Text("Êtes-vous sûr de vouloir supprimer cette conversation ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatIsoDate(conversation.created_at),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer la conversation",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
