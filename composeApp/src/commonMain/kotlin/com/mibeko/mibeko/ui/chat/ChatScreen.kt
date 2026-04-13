package com.mibeko.mibeko.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.data.remote.AgentConversationMessage
import com.mibeko.mibeko.data.remote.ArticleSource
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import androidx.compose.ui.text.TextStyle
import com.mibeko.mibeko.ui.navigation.LocalNavController
import com.mibeko.mibeko.ui.navigation.Screen
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
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    placeholder = { Text("Posez une question sur le droit...") },
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
            Text(
                text = "MIBEKO AI PEUT FAIRE DES ERREURS. VÉRIFIEZ LES SOURCES CITÉES.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: AgentConversationMessage) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Profile Header (Icon + Name)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isUser) {
                Text(
                    text = "Vous",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Utilisateur",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Mibeko IA",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mibeko IA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Bubble
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(16.dp)
                .widthIn(max = 320.dp)
        ) {
            if (!isUser && message.content.isEmpty()) {
                ThinkingDots()
            } else if (isUser) {
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val customTypography = markdownTypography(
                    h1 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    h2 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    h3 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    h4 = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    h5 = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    h6 = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    text = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    paragraph = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                )
                
                Markdown(
                    content = message.content,
                    typography = customTypography
                )
            }
        }
        
        // Sources outside the bubble
        if (!isUser && message.getSources() != null && message.getSources()!!.isNotEmpty()) {
            val sources = message.getSources()!!
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Voici les sources juridiques pertinentes pour votre situation :",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sources) { source ->
                    ArticleSourceCard(source)
                }
            }
        }
    }
}

@Composable
fun ArticleSourceCard(source: ArticleSource) {
    val navController = LocalNavController.current
    
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable {
                navController.navigate(Screen.DocumentDetail(source.document_id))
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = source.document_title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val titleText = if (source.number.isNotBlank()) {
                if (source.number.lowercase().startsWith("article")) source.number else "Article ${source.number}"
            } else {
                "Extrait"
            }
            
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "\"${source.content}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Lire la suite >",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
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
