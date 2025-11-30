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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import my.application.data.repository.MultiplayerRepository
import my.application.data.auth.AuthManager
import my.application.ui.components.AppTopBar

/**
 * Waiting screen shown after a player finishes a multiplayer game
 * Waits for all other players to finish before showing final results
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerWaitingScreen(
    navController: NavController,
    sessionId: String
) {
    val context = LocalContext.current
    val repository = remember { MultiplayerRepository.getInstance(context) }
    val authManager = remember { AuthManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var allPlayersFinished by remember { mutableStateOf(false) }
    var finishedPlayers by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalPlayers by remember { mutableStateOf(0) }
    var courseName by remember { mutableStateOf("") }
    var totalHoles by remember { mutableStateOf(18) }

    // Get current user email
    LaunchedEffect(Unit) {
        currentUserEmail = authManager.getUserEmail()
    }

    // Observe session for when all players finish
    LaunchedEffect(sessionId) {
        repository.observeSession(sessionId).collect { session ->
            if (session != null) {
                try {
                    courseName = session.baneName
                    totalPlayers = session.players.size
                    
                    // Get list of finished players
                    val finished = session.players.values.filter { it.finished }.map { it.email }
                    finishedPlayers = finished
                    
                    // Check if all players are finished
                    val allFinished = session.players.values.all { it.finished }
                    
                    if (allFinished && !allPlayersFinished) {
                        allPlayersFinished = true
                        
                        // Collect all player scores
                        val playerScores = mutableMapOf<String, List<Int>>()
                        var maxHoles = 0
                        
                        session.players.values.forEach { player ->
                            val scores = mutableListOf<Int>()
                            val sortedHoles = player.scores.keys.mapNotNull { it.toIntOrNull() }.sorted()
                            
                            if (sortedHoles.isNotEmpty()) {
                                maxHoles = maxOf(maxHoles, sortedHoles.maxOrNull() ?: 0)
                                
                                for (holeNum in 1..sortedHoles.maxOrNull()!!) {
                                    val scoreEntry = player.scores[holeNum.toString()]
                                    scores.add(scoreEntry?.score ?: 0)
                                }
                            }
                            
                            playerScores[player.email] = scores
                        }
                        
                        totalHoles = maxHoles
                        
                        // Store game results
                        val gameResults = my.application.data.GameResults(
                            courseName = courseName,
                            totalHoles = totalHoles,
                            playerScores = playerScores,
                            isSinglePlayer = false
                        )
                        my.application.data.GameResultsManager.setGameResults(gameResults)
                        
                        // Clear active session
                        val activeSessionManager = my.application.data.ActiveSessionManager.getInstance(context)
                        activeSessionManager.clearActiveSession()
                        
                        // Navigate to post-game screen
                        val encodedCourseName = java.net.URLEncoder.encode(courseName, "UTF-8")
                        navController.navigate("post_game/$encodedCourseName/$totalHoles/false") {
                            popUpTo("multiplayer_waiting/$sessionId") { inclusive = true }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MultiplayerWaitingScreen", "Error processing session data", e)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Venter på spillere",
                canNavigateBack = false,
                navController = navController
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Trophy icon
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main message
            Text(
                text = "Du er ferdig!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Venter på at andre spillere skal fullføre...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Player status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spillere ferdig",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${finishedPlayers.size}/$totalPlayers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { if (totalPlayers > 0) finishedPlayers.size.toFloat() / totalPlayers.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // List of finished players
                    if (finishedPlayers.isNotEmpty()) {
                        Text(
                            text = "Ferdige spillere:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        finishedPlayers.forEach { email ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = email.substringBefore("@"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Resultatene vises når alle har fullført runden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
