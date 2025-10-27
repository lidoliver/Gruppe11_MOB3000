package My.Application.ui.spill

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import My.Application.ui.components.AppTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayDiscGolfScreen(
    navController: NavController,
    courseId: String?,
    sessionId: String? = null
) {
    var courseName by remember { mutableStateOf("Disc Golf Bane") }
    var currentHole by remember { mutableIntStateOf(1) }
    var totalHoles by remember { mutableIntStateOf(18) }
    var holeScores by remember { mutableStateOf(List(18) { 0 }) }
    var showScores by remember { mutableStateOf(false) }
    var isInviteModalOpen by remember { mutableStateOf(false) }
    var inviteLink by remember { mutableStateOf("") }
    var linkCopied by remember { mutableStateOf(false) }
    var players by remember { mutableStateOf(listOf<Player>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val isSessionPlay = sessionId != null
    val scope = rememberCoroutineScope()

    // Simulate data loading
    LaunchedEffect(courseId, sessionId) {
        loading = true
        delay(500) // Simulate API call

        if (isSessionPlay) {
            // Load session data
            courseName = "Multiplayer Game - $sessionId"
            inviteLink = "https://yourapp.com/join-game/$sessionId"
            players = listOf(Player("host@example.com", true, List(totalHoles) { 0 }))
        } else {
            courseName = "Course $courseId"
        }

        loading = false
    }

    if (loading) {
        LoadingScreen()
        return
    }

    if (error != null) {
        ErrorScreen(error = error!!, onBack = { navController.popBackStack() })
        return
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = courseName,
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Invite Friends Button
            InviteFriendsButton(
                onClick = {
                    isInviteModalOpen = true
                    // Generate session if not already in session play
                    if (!isSessionPlay) {
                        val newSessionId = "session-${System.currentTimeMillis()}"
                        inviteLink = "https://yourapp.com/join-game/$newSessionId"
                        players = listOf(Player("you@example.com", true, List(totalHoles) { 0 }))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Section
            GameProgress(
                currentHole = currentHole,
                totalHoles = totalHoles
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current Hole Score Input
            CurrentHoleCard(
                holeNumber = currentHole,
                score = holeScores[currentHole - 1],
                onScoreChange = { newScore ->
                    holeScores = holeScores.toMutableList().apply {
                        this[currentHole - 1] = newScore
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Buttons
            NavigationButtons(
                currentHole = currentHole,
                totalHoles = totalHoles,
                onPrevious = { if (currentHole > 1) currentHole-- },
                onNext = { if (currentHole < totalHoles) currentHole++ },
                onFinish = {
                    // Save scorecard and navigate back
                    navController.popBackStack()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle Score Summary Button
            Button(
                onClick = { showScores = !showScores },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Text(if (showScores) "Skjul Resultater" else "Vis Alle Resultater")
                Icon(
                    imageVector = if (showScores) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Summary
            AnimatedVisibility(
                visible = showScores,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ScoreSummary(
                    holeScores = holeScores,
                    currentHole = currentHole,
                    onHoleClick = { holeIndex -> currentHole = holeIndex + 1 }
                )
            }
        }

        // Invite Modal
        if (isInviteModalOpen) {
            InviteModal(
                inviteLink = inviteLink,
                players = players,
                linkCopied = linkCopied,
                onCopyLink = {
                    scope.launch {
                        linkCopied = true
                        delay(2000)
                        linkCopied = false
                    }
                },
                onDismiss = { isInviteModalOpen = false },
                onStartGame = { isInviteModalOpen = false }
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF4A90E2))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading course...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ErrorScreen(error: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = error,
                color = Color(0xFFE74C3C),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

@Composable
fun InviteFriendsButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6366F1)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Spill med venner",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GameProgress(currentHole: Int, totalHoles: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Hull $currentHole av $totalHoles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A90E2)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { currentHole.toFloat() / totalHoles.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = Color(0xFF4A90E2),
            trackColor = Color(0xFFEEEEEE)
        )
    }
}

@Composable
fun CurrentHoleCard(
    holeNumber: Int,
    score: Int,
    onScoreChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Hull $holeNumber",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF4A90E2),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ditt resultat:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444)
                )

                ScoreControls(
                    score = score,
                    onIncrement = { onScoreChange((score + 1).coerceAtLeast(0)) },
                    onDecrement = { onScoreChange((score - 1).coerceAtLeast(1)) }
                )
            }
        }
    }
}

@Composable
fun ScoreControls(
    score: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = score > 1,
            modifier = Modifier
                .size(50.dp)
                .background(
                    if (score > 1) Color(0xFF4A90E2) else Color(0xFF95A5A6),
                    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(70.dp)
                .height(50.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFF4A90E2), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun NavigationButtons(
    currentHole: Int,
    totalHoles: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onPrevious,
            enabled = currentHole > 1,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A90E2),
                disabledContainerColor = Color(0xFF95A5A6)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Forrige Hull", fontSize = 16.sp)
        }

        if (currentHole == totalHoles) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fullfør Runde", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Neste Hull", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ScoreSummary(
    holeScores: List<Int>,
    currentHole: Int,
    onHoleClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Poeng Så Langt",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF4A90E2),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                itemsIndexed(holeScores) { index, score ->
                    HoleSummaryItem(
                        holeNumber = index + 1,
                        score = score,
                        isCurrent = currentHole - 1 == index,
                        onClick = { onHoleClick(index) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total Score: ${holeScores.sum()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun HoleSummaryItem(
    holeNumber: Int,
    score: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFE3F2FD) else Color.White
        ),
        border = if (isCurrent)
            BorderStroke(2.dp, Color(0xFF4A90E2))
        else
            BorderStroke(1.dp, Color(0xFFDDDDDD))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hull $holeNumber",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = if (score > 0) score.toString() else "-",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun InviteModal(
    inviteLink: String,
    players: List<Player>,
    linkCopied: Boolean,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit,
    onStartGame: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Inviter venner til å spille",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Del denne lenken med venner for å spille sammen:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inviteLink,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = onCopyLink,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        )
                    ) {
                        Text(if (linkCopied) "Kopiert!" else "Kopier")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spillere (${players.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        players.forEach { player ->
                            PlayerItem(player)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "Venter på at andre skal bli med...",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color(0xFF718096),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lukk")
                    }

                    Button(
                        onClick = onStartGame,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2)
                        )
                    ) {
                        Text("Start spill (${players.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerItem(player: Player) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (player.isHost) Color(0xFF4A90E2) else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "${player.email} ${if (player.isHost) "(Vert)" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (player.isHost) FontWeight.SemiBold else FontWeight.Normal,
            color = if (player.isHost) Color(0xFF4A90E2) else Color(0xFF333333)
        )
    }
}

data class Player(
    val email: String,
    val isHost: Boolean,
    val scores: List<Int>
)
