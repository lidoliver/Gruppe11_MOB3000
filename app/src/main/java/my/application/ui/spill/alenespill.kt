package my.application.ui.spill

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import my.application.data.GameSession
import my.application.data.GameSessionManager
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayDiscGolfScreen(
    navController: NavController,
    courseId: String?,
    sessionId: String? = null,
    isBottomBarVisible: Boolean = false,
    onBottomBarVisibilityChange: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager(context) }

    var courseName by remember { mutableStateOf("Disc Golf Bane") }
    var currentHole by remember { mutableIntStateOf(1) }
    var totalHoles by remember { mutableIntStateOf(18) }
    var holeScores by remember { mutableStateOf(List(18) { 0 }) }
    var showScores by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val isSessionPlay = sessionId != null

    // Load data - check for saved session first, then load new course
    LaunchedEffect(courseId, sessionId) {
        loading = true
        delay(500) // Simulate API call

        // Try to restore saved session if it matches current course
        val savedSession = sessionManager.getGameSession()
        if (savedSession != null &&
            savedSession.courseId == courseId &&
            savedSession.sessionId == sessionId) {
            // Restore saved session
            courseName = savedSession.courseName
            currentHole = savedSession.currentHole
            totalHoles = savedSession.totalHoles
            holeScores = savedSession.holeScores
        } else {
            // Start new session
            // Determine hole count from courseId
            val holes = when {
                courseId?.contains("6-holes") == true -> 6
                courseId?.contains("9-holes") == true -> 9
                courseId?.contains("12-holes") == true -> 12
                courseId?.contains("15-holes") == true -> 15
                courseId?.contains("18-holes") == true -> 18
                else -> 18 // Default to 18
            }

            totalHoles = holes
            holeScores = List(holes) { 0 }

            if (isSessionPlay) {
                courseName = "Multiplayer Game - $sessionId"
            } else {
                courseName = when (holes) {
                    6 -> "Demo 6-Hull Bane"
                    9 -> "Demo 9-Hull Bane"
                    12 -> "Demo 12-Hull Bane"
                    15 -> "Demo 15-Hull Bane"
                    18 -> "Demo 18-Hull Bane"
                    else -> "Course $courseId"
                }
            }

            // Save initial session
            if (courseId != null) {
                sessionManager.saveGameSession(
                    GameSession(
                        courseId = courseId,
                        sessionId = sessionId,
                        courseName = courseName,
                        currentHole = currentHole,
                        totalHoles = totalHoles,
                        holeScores = holeScores
                    )
                )
            }
        }

        loading = false
    }

    // Auto-save when state changes
    LaunchedEffect(currentHole, holeScores) {
        if (!loading && courseId != null) {
            sessionManager.saveGameSession(
                GameSession(
                    courseId = courseId,
                    sessionId = sessionId,
                    courseName = courseName,
                    currentHole = currentHole,
                    totalHoles = totalHoles,
                    holeScores = holeScores
                )
            )
        }
    }

    // Clear session on unmount if game is finished
    DisposableEffect(Unit) {
        onDispose {
            // Don't clear - keep the session saved for restoration
        }
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
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content with scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
            // Progress Section with Map and Score Toggle Icons
            GameProgress(
                currentHole = currentHole,
                totalHoles = totalHoles,
                onMapClick = {
                    // TODO: Open dynamic map with Google Maps API showing all hole locations
                },
                onShowScoresClick = { showScores = !showScores },
                showScores = showScores
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Current Hole Score Input
            CurrentHoleCard(
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
                    // Clear saved session when game is finished
                    sessionManager.clearGameSession()
                    // Save scorecard and navigate back
                    navController.popBackStack()
                }
            )

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

        // Swipe detection zone covering bottom half of screen
        var dragStartY by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) // Bottom half of screen
                .align(Alignment.BottomCenter)
                .pointerInput(isBottomBarVisible) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragStartY = offset.y
                            isDragging = true
                        },
                        onDragEnd = {
                            if (isDragging) {
                                isDragging = false
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            val currentY = change.position.y
                            val totalDrag = currentY - dragStartY

                            // Only trigger on significant drag
                            if (abs(totalDrag) > 120) {
                                if (totalDrag < 0 && !isBottomBarVisible) {
                                    // Swiped up significantly - show bottom bar
                                    onBottomBarVisibilityChange?.invoke(true)
                                    isDragging = false
                                    dragStartY = currentY
                                } else if (totalDrag > 0 && isBottomBarVisible) {
                                    // Swiped down significantly - hide bottom bar
                                    onBottomBarVisibilityChange?.invoke(false)
                                    isDragging = false
                                    dragStartY = currentY
                                }
                            }
                        }
                    )
                }
        )

        // Animated arrow indicator at bottom
        val arrowRotation by animateFloatAsState(
            targetValue = if (isBottomBarVisible) 180f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "arrow rotation"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = if (isBottomBarVisible)
                        1.dp  // Just above navbar (navbar is ~80dp high)
                    else
                        40.dp  // Above Android navigation buttons when hidden
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isBottomBarVisible) "Sveip ned" else "Sveip opp",
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(42.dp)  // Bigger for better visibility
                    .graphicsLayer {
                        rotationZ = arrowRotation
                    }
            )
        }
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
fun GameProgress(
    currentHole: Int,
    totalHoles: Int,
    onMapClick: () -> Unit,
    onShowScoresClick: () -> Unit,
    showScores: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hull $currentHole av $totalHoles",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMapClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Vis kart",
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(
                    onClick = onShowScoresClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (showScores) Icons.Default.ExpandLess else Icons.Default.GridOn,
                        contentDescription = "Vis alle hull",
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            ScoreControls(
                score = score,
                onIncrement = { onScoreChange((score + 1).coerceAtLeast(0)) },
                onDecrement = { onScoreChange((score - 1).coerceAtLeast(0)) }
            )
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
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(16.dp))
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = score > 0,
            modifier = Modifier
                .size(80.dp)
                .background(
                    if (score > 0) Color(0xFF4A90E2) else Color(0xFF95A5A6),
                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(80.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFF4A90E2), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
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
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A90E2),
                disabledContainerColor = Color(0xFF95A5A6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Forrige Hull", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        if (currentHole == totalHoles) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Fullfør Runde", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Neste Hull", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
    // Calculate optimal columns based on number of holes
    val totalHoles = holeScores.size
    val columnCount = when {
        totalHoles <= 6 -> 3  // 6 holes or fewer: 3 columns (bigger cards)
        totalHoles <= 9 -> 3  // 9 holes: 3 columns
        totalHoles <= 12 -> 4 // 12 holes: 4 columns
        totalHoles <= 15 -> 5 // 15 holes: 5 columns
        else -> 6             // 18 holes: 6 columns (standard)
    }

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
                columns = GridCells.Fixed(columnCount),
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

            HorizontalDivider(color = Color(0xFFDDDDDD), thickness = 1.dp)

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

