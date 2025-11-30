package my.application.ui.screen.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import my.application.data.friends.Friend
import my.application.data.friends.FriendStatus
import my.application.data.repository.MultiplayerRepository
import my.application.data.repository.MessagesRepository
import my.application.data.repository.UserRepository

@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = viewModel()
) {
    val friendRequestsCount by viewModel.friendRequestsCount.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Reload friends and friend request count when screen is displayed or resumed
    // Also poll periodically (every 30 seconds) to update automatically
    LaunchedEffect(isAuthenticated) {
        viewModel.refreshAuthenticationState() // Check auth status

        if (isAuthenticated) {
            viewModel.loadFriends() // Initial load with loading indicator
            viewModel.loadFriendRequestsCount()

            // Poll every 30 seconds for updates while on this screen
            // Use showLoading=false to avoid jarring UI resets
            while (isAuthenticated) {
                kotlinx.coroutines.delay(30_000) // 30 seconds
                if (isAuthenticated) {
                    viewModel.loadFriends(showLoading = false) // Silent refresh
                    viewModel.loadFriendRequestsCount()
                }
            }
        }
    }

    // Reload friend request count when navigating back to this screen
    DisposableEffect(navController) {
        val callback = NavController.OnDestinationChangedListener { controller, destination, _ ->
            if (destination.route == "friends") {
                // Reload the count when returning to friends screen
                // Use silent refresh to avoid jarring UI reset
                viewModel.refreshAuthenticationState() // Re-check auth status
                if (isAuthenticated) {
                    viewModel.loadFriendRequestsCount()
                    viewModel.loadFriends(showLoading = false) // Silent refresh when returning
                }
            }
        }
        navController.addOnDestinationChangedListener(callback)

        onDispose {
            navController.removeOnDestinationChangedListener(callback)
        }
    }

    // Filter friends based on search query
    val filteredFriends = remember(searchQuery, friends) {
        if (searchQuery.isBlank()) {
            friends
        } else {
            friends.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Show error snackbar if there's an error
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // You can show a snackbar here if needed
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Venner",
                canNavigateBack = false,
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Only show add friend button when authenticated
            if (isAuthenticated) {
                FloatingActionButton(
                    onClick = { navController.navigate("friend_search") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Søk etter venner",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Only show search bar when authenticated
            if (isAuthenticated) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Søk etter venner...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Søk"
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Friend Requests Card - only show when authenticated
            if (isAuthenticated && friendRequestsCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { navController.navigate("friend_requests") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Venneforespørsler",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$friendRequestsCount ${if (friendRequestsCount == 1) "ny forespørsel" else "nye forespørsler"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Se forespørsler",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }


            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Not authenticated state - show login prompt
            else if (!isAuthenticated) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Logg inn for å se venner",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Du må logge inn for å se vennelisten og legge til venner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("login") },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Logg inn")
                    }
                }
            }
            // Empty state
            else if (filteredFriends.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Ingen venner ennå" else "Ingen venner funnet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank())
                            "Legg til venner for å spille sammen"
                            else "Prøv et annet søk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFriends) { friend ->
                        FriendItem(
                            friend = friend,
                            onProfileClick = {
                                navController.navigate("friend_profile/${friend.email}/${friend.name}")
                            },
                            onMessageClick = {
                                navController.navigate("conversation/${friend.email}/${friend.name}")
                            },
                            onInviteClick = {
                                // Create lobby and send invite code
                                coroutineScope.launch {
                                    try {
                                        // Create a multiplayer session with default course
                                        val multiplayerRepo = MultiplayerRepository.getInstance(context)
                                        val result = multiplayerRepo.createSession(
                                            courseId = "default",
                                            courseName = "Multiplayer Session",
                                            totalHoles = 18
                                        )
                                        
                                        result.onSuccess { sessionId ->
                                            // Send the session code as a message
                                            val userRepo = UserRepository.getInstance()
                                            val messagesRepo = MessagesRepository()
                                            val token = userRepo.getAuthToken()
                                            
                                            if (token != null) {
                                                // Remove "Bearer " prefix if present (MessagesRepository adds it)
                                                val cleanToken = token.removePrefix("Bearer ")
                                                
                                                val inviteMessage = "Bli med i mitt disc golf-spill! Klikk på koden for å bli med: $sessionId"
                                                messagesRepo.sendMessage(
                                                    receiverEmail = friend.email,
                                                    content = inviteMessage,
                                                    token = cleanToken
                                                ).onSuccess {
                                                    // Navigate to the lobby
                                                    navController.navigate("multiplayer_lobby/$sessionId")
                                                }.onFailure { error ->
                                                    snackbarHostState.showSnackbar(
                                                        message = "Kunne ikke sende invitasjon: ${error.message}",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    message = "Ikke autentisert",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }.onFailure { error ->
                                            snackbarHostState.showSnackbar(
                                                message = "Kunne ikke opprette spill: ${error.message}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            message = "Feil: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onHistoryClick = {
                                navController.navigate("friend_history/${friend.email}/${friend.name}")
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    friend: Friend,
    onProfileClick: () -> Unit,
    onMessageClick: () -> Unit,
    onInviteClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    var showActionMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = { showActionMenu = !showActionMenu },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with status indicator - clickable to view profile
                Box(
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(friend.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when (friend.status) {
                                    FriendStatus.ONLINE -> Color(0xFF4CAF50)
                                    FriendStatus.IN_GAME -> Color(0xFFFFC107)
                                    FriendStatus.OFFLINE -> Color(0xFF9E9E9E)
                                }
                            )
                            .align(Alignment.BottomEnd)
                    )

                    // Unread message indicator
                    if (friend.hasUnreadMessages) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Friend info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = friend.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (friend.status) {
                                FriendStatus.ONLINE -> "Pålogget"
                                FriendStatus.IN_GAME -> "Spiller nå"
                                FriendStatus.OFFLINE -> "Frakoblet"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (friend.status) {
                                FriendStatus.ONLINE -> Color(0xFF4CAF50)
                                FriendStatus.IN_GAME -> Color(0xFFFFC107)
                                FriendStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${friend.gamesPlayed} spill",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (friend.lastPlayed != null) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = friend.lastPlayed,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Message button - always visible
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Send melding",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Action buttons - shown when item is clicked
            if (showActionMenu) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // Invite to play button
                    Button(
                        onClick = onInviteClick,
                        modifier = Modifier.weight(1f),
                        enabled = friend.status != FriendStatus.OFFLINE,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Inviter",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Vis historikk-knapp
                    OutlinedButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Historikk",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

