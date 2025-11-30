package my.application.ui.screen.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import my.application.data.messaging.ChatMessage
import my.application.service.CurrentConversationTracker
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Send message helper - keeps keyboard open by NOT clearing focus or hiding keyboard
    val sendMessage: () -> Unit = {
        if (messageText.isNotBlank() && !isSending) {
            Log.d("ConversationScreen", "Sending message: $messageText")
            viewModel.sendMessage(messageText)
            messageText = "" // Clear text but DON'T clear focus
            // Immediately re-request focus to prevent any focus loss
            focusRequester.requestFocus()
            // Do NOT call keyboardController?.hide()
            // Do NOT call clearFocus()
        }
    }

    // Track when user enters/leaves this conversation
    DisposableEffect(contactEmail) {
        Log.d("ConversationScreen", "Entering conversation with $contactEmail")
        CurrentConversationTracker.setCurrentConversation(contactEmail)
        
        onDispose {
            Log.d("ConversationScreen", "Leaving conversation with $contactEmail")
            CurrentConversationTracker.clearCurrentConversation()
        }
    }

    // Automatically focus text field when screen loads (opens keyboard)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100) // Small delay to ensure UI is ready
        focusRequester.requestFocus()
    }

    // Keep focus on text field even after recompositions
    LaunchedEffect(messageText) {
        // After text changes (including clearing), ensure focus is maintained
        if (messageText.isEmpty()) {
            kotlinx.coroutines.delay(10) // Tiny delay for state update
            focusRequester.requestFocus()
        }
    }

    // Log messages for debugging
    LaunchedEffect(messages) {
        Log.d("ConversationScreen", "Messages updated: ${messages.size} messages")
        messages.forEachIndexed { index, msg ->
            Log.d("ConversationScreen", "  [$index] id=${msg.id}, from=${if(msg.isFromMe) "me" else "them"}, text=${msg.text.take(30)}")
        }
    }

    // Auto-scroll to latest message (bottom of the list in reverse layout)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                        reverseLayout = true
                    ) {
                        items(messages.reversed(), key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                navController = navController,
                                onCopyMessage = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("message", text)
                                    clipboard.setPrimaryClip(clip)
                                    
                                    // Show snackbar confirmation
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Melding kopiert",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Meldingsinntastingslinje
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Skriv en melding...") },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                // Send the message (mimics the XML pattern)
                                sendMessage()
                                // Explicitly request focus to keep keyboard open
                                // (equivalent to v.requestFocus() in XML)
                                focusRequester.requestFocus()
                                // By handling this action, we prevent default behavior
                                // (equivalent to returning true in setOnEditorActionListener)
                            }
                        ),
                        maxLines = 4,
                        enabled = !isSending
                    )

                    // Send-knapp
                    FilledIconButton(
                        onClick = sendMessage,
                        modifier = Modifier.size(56.dp),
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

// Helper function to detect session codes and create annotated string
fun createAnnotatedMessageText(text: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    // UUID pattern for session codes (e.g., 44a9d6b4-6bb1-4603-b9d6-55f74b071fc4)
    val sessionCodePattern = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".toRegex()
    
    return buildAnnotatedString {
        var lastIndex = 0
        sessionCodePattern.findAll(text).forEach { matchResult ->
            // Add text before the match
            append(text.substring(lastIndex, matchResult.range.first))
            
            // Add the clickable session code
            pushStringAnnotation(
                tag = "SESSION_CODE",
                annotation = matchResult.value
            )
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(matchResult.value)
            }
            pop()
            
            lastIndex = matchResult.range.last + 1
        }
        
        // Add remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    navController: NavController,
    onCopyMessage: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(
            16.dp,
            if (message.isFromMe) Alignment.End else Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconSize = 48.dp

        // Show avatar for received messages on the left
        if (!message.isFromMe) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(iconSize))
        }

        // Message bubble with better styling
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (message.isFromMe) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    val textColor = if (message.isFromMe) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    
                    val linkColor = if (message.isFromMe) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    
                    val annotatedText = createAnnotatedMessageText(message.text, linkColor)
                    
                    ClickableText(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                        modifier = Modifier.weight(1f),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = "SESSION_CODE",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { annotation ->
                                // Navigate to join session screen with the code
                                navController.navigate("join_multiplayer_session?code=${annotation.item}")
                            }
                        }
                    )
                    IconButton(
                        onClick = { onCopyMessage(message.text) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Kopier melding",
                            tint = if (message.isFromMe) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isFromMe) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

