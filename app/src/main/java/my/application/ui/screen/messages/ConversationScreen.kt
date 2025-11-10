package my.application.ui.screen.messages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import my.application.data.messaging.ChatMessage
import kotlin.collections.forEachIndexed

@Composable
fun ConversationScreen(
    navController: NavController,
    contactName: String,
    contactEmail: String,
    viewModel: ConversationViewModel = viewModel(
        factory = ConversationViewModelFactory(contactEmail)
    )
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Log messages for debugging
    LaunchedEffect(messages) {
        Log.d("ConversationScreen", "Messages updated: ${messages.size} messages")
        messages.forEachIndexed { index, msg ->
            Log.d("ConversationScreen", "  [$index] id=${msg.id}, from=${if(msg.isFromMe) "me" else "them"}, text=${msg.text.take(30)}")
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar if there's an error
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            Log.e("ConversationScreen", "Error: $errorMessage")
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = contactName,
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && messages.isEmpty()) {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (messages.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingen meldinger ennå.\nSend den første meldingen!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatBubble(message = message)
                        }
                    }
                }
            }

            // Message input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Skriv en melding...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        maxLines = 4,
                        enabled = !isSending
                    )

                    // Send button
                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                Log.d("ConversationScreen", "Send button clicked with text: $messageText")
                                val textToSend = messageText
                                viewModel.sendMessage(textToSend)
                                messageText = ""
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !isSending && messageText.isNotBlank()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromMe) {
            // Avatar for other person
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble
        Column(
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                ),
                color = if (message.isFromMe) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (message.isFromMe) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isFromMe) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        if (message.isFromMe) {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

