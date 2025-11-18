package my.application.ui.spill

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import my.application.data.GameSession
import my.application.data.GameSessionManager
import my.application.data.repository.TrackRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import my.application.data.remote.dto.HoleData

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
    val trackRepository = remember { TrackRepository(context) }

    var courseName by remember { mutableStateOf("Disc Golf Bane") }
    var currentHole by remember { mutableIntStateOf(1) }
    var totalHoles by remember { mutableIntStateOf(18) }
    var holeScores by remember { mutableStateOf(List(18) { 0 }) }
    var showScores by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var holesData by remember { mutableStateOf<List<HoleData>>(emptyList()) }

    val scrollState = rememberScrollState()
    val isSessionPlay = sessionId != null

    // Load data - check for saved session first, then load course from database
    LaunchedEffect(courseId, sessionId) {
        loading = true
        error = null

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
            loading = false
        } else if (courseId != null) {
            // Fetch course from backend database
            try {
                android.util.Log.d("AleneSpill", "========= FETCHING COURSE DATA =========")
                android.util.Log.d("AleneSpill", "Course ID: $courseId")

                val result = trackRepository.getTrackById(courseId)
                result.onSuccess { track ->
                    android.util.Log.d("AleneSpill", "✅ API Response received!")
                    android.util.Log.d("AleneSpill", "Track DTO raw data: $track")

                    // Use data from database
                    courseName = track.name
                    totalHoles = track.holes
                    holeScores = List(track.holes) { 0 }

                    // Load hole data with GPS coordinates
                    holesData = track.holesData ?: emptyList()

                    // Debug logging
                    android.util.Log.d("AleneSpill", "========= COURSE DATA LOADED =========")
                    android.util.Log.d("AleneSpill", "Course name: ${track.name}")
                    android.util.Log.d("AleneSpill", "Course ID (baneId): ${track.baneId}")
                    android.util.Log.d("AleneSpill", "Total holes (track.holes): ${track.holes}")
                    android.util.Log.d("AleneSpill", "Holes data list size: ${holesData.size}")
                    android.util.Log.d("AleneSpill", "Holes data is empty: ${holesData.isEmpty()}")
                    android.util.Log.d("AleneSpill", "track.holesData is null: ${track.holesData == null}")

                    if (holesData.isEmpty()) {
                        android.util.Log.e("AleneSpill", "⚠️⚠️⚠️ WARNING: holesData is EMPTY! ⚠️⚠️⚠️")
                        android.util.Log.e("AleneSpill", "Track.holesData field value: ${track.holesData}")
                        android.util.Log.e("AleneSpill", "This means the API response doesn't include 'holes' array")
                        android.util.Log.e("AleneSpill", "Or the GPS data hasn't been added to the database yet")
                    } else {
                        android.util.Log.d("AleneSpill", "✅ Found ${holesData.size} holes with data!")
                    }

                    holesData.forEachIndexed { index, hole ->
                        val hasTeeCoords = hole.teeLatitude != null && hole.teeLongitude != null
                        val hasBasketCoords = hole.basketLatitude != null && hole.basketLongitude != null
                        val logPrefix = if (hasTeeCoords && hasBasketCoords) "✅" else "❌"

                        android.util.Log.d("AleneSpill", "$logPrefix Hole ${hole.holeNumber} [index=$index]: " +
                            "tee=(${hole.teeLatitude}, ${hole.teeLongitude}), " +
                            "basket=(${hole.basketLatitude}, ${hole.basketLongitude}), " +
                            "par=${hole.par}, " +
                            "hasTee=$hasTeeCoords, hasBasket=$hasBasketCoords")
                    }
                    android.util.Log.d("AleneSpill", "========================================")

                    if (isSessionPlay) {
                        courseName = "${track.name} - Multiplayer"
                    }

                    // Save initial session
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
                    loading = false
                }.onFailure { exception ->
                    error = "Failed to load course: ${exception.message}"
                    loading = false
                }
            } catch (e: Exception) {
                error = "Error loading course data: ${e.message}"
                loading = false
            }
        } else {
            // Fallback to demo data if no courseId
            totalHoles = 18
            holeScores = List(18) { 0 }
            courseName = "Demo Course"
            loading = false
        }
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
                    // Navigate to course map screen
                    courseId?.let {
                        navController.navigate("course_map/$it")
                    }
                },
                onShowScoresClick = { showScores = !showScores },
                showScores = showScores
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Map showing current hole
            val currentHoleData = holesData.find { it.holeNumber == currentHole }

            // Debug logging
            LaunchedEffect(currentHole, holesData.size) {
                android.util.Log.d("AleneSpill", "========= HOLE MAP DEBUG =========")
                android.util.Log.d("AleneSpill", "Current hole: $currentHole")
                android.util.Log.d("AleneSpill", "Total holes data: ${holesData.size}")
                android.util.Log.d("AleneSpill", "Holes data list: ${holesData.map { it.holeNumber }}")
                android.util.Log.d("AleneSpill", "Current hole data found: ${currentHoleData != null}")
                currentHoleData?.let { hole ->
                    android.util.Log.d("AleneSpill", "Hole ${hole.holeNumber} GPS: " +
                        "tee=(${hole.teeLatitude}, ${hole.teeLongitude}), " +
                        "basket=(${hole.basketLatitude}, ${hole.basketLongitude})")
                    android.util.Log.d("AleneSpill", "Has tee coords: ${hole.teeLatitude != null && hole.teeLongitude != null}")
                    android.util.Log.d("AleneSpill", "Has basket coords: ${hole.basketLatitude != null && hole.basketLongitude != null}")
                } ?: run {
                    android.util.Log.e("AleneSpill", "❌ No hole data found for hole $currentHole")
                }
                android.util.Log.d("AleneSpill", "==================================")
            }

            if (currentHoleData != null &&
                currentHoleData.teeLatitude != null &&
                currentHoleData.teeLongitude != null &&
                currentHoleData.basketLatitude != null &&
                currentHoleData.basketLongitude != null) {
                HoleMapCard(
                    holeNumber = currentHole,
                    teePosition = LatLng(currentHoleData.teeLatitude, currentHoleData.teeLongitude),
                    basketPosition = LatLng(currentHoleData.basketLatitude, currentHoleData.basketLongitude),
                    par = currentHoleData.par
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Show placeholder when no GPS data with more detailed error message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "No GPS data",
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ingen kartdata for hull $currentHole",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center
                        )
                        if (holesData.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(GPS-data mangler i databasen)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF999999),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

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

        // Note: Removed the blocking swipe detection box that was preventing button clicks
        // Users can click the arrow indicator at the bottom to show/hide the bottom bar

        // Animated arrow indicator at bottom - now clickable
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
                contentDescription = if (isBottomBarVisible) "Sveip ned for å skjule" else "Trykk for å vise navigasjon",
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        // Toggle bottom bar visibility when arrow is clicked
                        onBottomBarVisibilityChange?.invoke(!isBottomBarVisible)
                    }
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

@Composable
fun HoleMapCard(
    holeNumber: Int,
    teePosition: LatLng,
    basketPosition: LatLng,
    par: Int
) {
    val cameraPositionState = rememberCameraPositionState()
    var isMapLoaded by remember { mutableStateOf(false) }

    // Calculate bounds to fit both tee and basket
    LaunchedEffect(teePosition, basketPosition) {
        android.util.Log.d("AleneSpill", "📍 Loading map for hole $holeNumber")
        android.util.Log.d("AleneSpill", "   Tee: (${teePosition.latitude}, ${teePosition.longitude})")
        android.util.Log.d("AleneSpill", "   Basket: (${basketPosition.latitude}, ${basketPosition.longitude})")

        val bounds = LatLngBounds.builder()
            .include(teePosition)
            .include(basketPosition)
            .build()

        try {
            kotlinx.coroutines.delay(100) // Small delay to ensure map is ready
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 150),
                durationMs = 1000
            )
            isMapLoaded = true
            android.util.Log.d("AleneSpill", "✅ Map loaded and camera animated for hole $holeNumber")
        } catch (_: Exception) {
            // Fallback to center position if bounds animation fails
            val center = LatLng(
                (teePosition.latitude + basketPosition.latitude) / 2,
                (teePosition.longitude + basketPosition.longitude) / 2
            )
            cameraPositionState.position = CameraPosition.fromLatLngZoom(center, 16f)
            isMapLoaded = true
            android.util.Log.d("AleneSpill", "✅ Map loaded with fallback camera for hole $holeNumber")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hull $holeNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2)
                )
                Text(
                    text = "Par $par",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = false,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false
                    ),
                    properties = MapProperties(
                        mapType = MapType.HYBRID
                    )
                ) {
                    // Tee marker (green)
                    Marker(
                        state = MarkerState(position = teePosition),
                        title = "Tee",
                        snippet = "Start punkt",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    // Basket marker (red)
                    Marker(
                        state = MarkerState(position = basketPosition),
                        title = "Basket",
                        snippet = "Mål",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // Line connecting tee to basket
                    Polyline(
                        points = listOf(teePosition, basketPosition),
                        color = Color(0xFF4A90E2),
                        width = 5f
                    )
                }

                // Legend overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF34A853), CircleShape)
                        )
                        Text(
                            text = "Tee",
                            fontSize = 11.sp,
                            color = Color(0xFF333333)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFEA4335), CircleShape)
                        )
                        Text(
                            text = "Basket",
                            fontSize = 11.sp,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }
    }
}
