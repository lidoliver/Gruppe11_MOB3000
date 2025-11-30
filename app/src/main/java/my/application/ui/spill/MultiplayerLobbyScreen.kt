package my.application.ui.spill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import my.application.ui.components.AppTopBar
import my.application.data.repository.MultiplayerRepository
import my.application.data.model.MultiplayerSession
import my.application.data.model.Player
import my.application.data.auth.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    navController: NavController,
    sessionId: String
) {
    val context = LocalContext.current
    val repository = remember { MultiplayerRepository.getInstance(context) }
    val authManager = remember { AuthManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var session by remember { mutableStateOf<MultiplayerSession?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }

    // Hent nåværende brukers e-post
    LaunchedEffect(Unit) {
        currentUserEmail = authManager.getUserEmail()
    }

    // Observer øktopdateringer
    LaunchedEffect(sessionId) {
        repository.observeSession(sessionId).collect { updatedSession ->
            session = updatedSession

            // Naviger til spill hvis økten er startet
            if (updatedSession?.status == "active") {
                navController.navigate("play_session/${updatedSession.baneId}/$sessionId") {
                    popUpTo("multiplayer_lobby/$sessionId") { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = session?.baneName ?: "Multiplayer Lobby",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Feilmelding
            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = it,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Øktinformasjon
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Spillkode:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = sessionId,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Session Code", sessionId)
                                clipboard.setPrimaryClip(clip)
                                // Show confirmation (optional - can add a snackbar here)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopier kode",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kopier", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${session?.players?.size ?: 0} spillere i lobby",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spillerliste
            Text(
                text = "Players",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                session?.players?.values?.let { playersList ->
                    items(playersList.toList()) { player ->
                        PlayerCard(
                            player = player,
                            isHost = player.email == session?.hostEmail
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Klar-knapp
            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = repository.setPlayerReady(sessionId, !isReady)
                        result.onSuccess {
                            isReady = !isReady
                        }.onFailure {
                            error = it.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReady) Color(0xFF4CAF50) else Color(0xFF4A90E2)
                )
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isReady) "Ready!" else "Mark as Ready")
            }

            // Start spill-knapp (kun for vert)
            if (session?.hostEmail == currentUserEmail) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = repository.startSession(sessionId)
                            result.onFailure {
                                error = it.message
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = session?.players?.values?.all { it.ready } == true,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Game")
                }
            }

            // Forlat-knapp
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        repository.leaveSession(sessionId)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave Lobby")
            }
        }
    }
}

/**
 * Kort som viser en spiller i lobbyen med status
 */
@Composable
fun PlayerCard(player: Player, isHost: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (player.ready) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF4A90E2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = player.email.substringBefore("@"),
                        fontWeight = FontWeight.Bold
                    )
                    if (isHost) {
                        Text(
                            text = "Host",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A90E2)
                        )
                    }
                }
            }

            if (player.ready) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ready",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}
