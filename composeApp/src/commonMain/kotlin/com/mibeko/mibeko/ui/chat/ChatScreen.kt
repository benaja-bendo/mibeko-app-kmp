package com.mibeko.mibeko.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.data.remote.AgentConversationMessage
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.ui.text.TextStyle
import com.mibeko.mibeko.ui.navigation.LocalNavController
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    initialPrompt: String?
) {
    val navController = LocalNavController.current
    val viewModel: ChatViewModel = koinViewModel()
    val chatState by viewModel.chatState.collectAsState()

    var textState by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(conversationId, initialPrompt) {
        viewModel.initChat(conversationId, initialPrompt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mibeko IA") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = chatState) {
                is ChatState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Erreur lors du chargement de la conversation.")
                    }
                }
                is ChatState.Content -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages) { message ->
                            ChatMessageItem(message)
                        }
                    }
                }
                is ChatState.Idle -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Posez votre question...") },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val message = textState.text
                        if (message.isNotBlank()) {
                            viewModel.sendMessage(message)
                            textState = TextFieldValue("")
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Envoyer",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: AgentConversationMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (!isUser && message.content.isEmpty()) {
                ThinkingDots()
            } else if (isUser) {
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Markdown(
                    content = message.content
                )
            }
        }
    }
}

@Composable
fun ThinkingDots() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha1), RoundedCornerShape(50)))
        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha2), RoundedCornerShape(50)))
        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha3), RoundedCornerShape(50)))
    }
}
