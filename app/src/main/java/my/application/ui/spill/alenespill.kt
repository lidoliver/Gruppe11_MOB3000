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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import my.application.data.remote.dto.HoleData

/**
 * Hovedskjerm for å spille disc golf
 * Håndterer både solo-spill og multiplayer-økter
 * Viser kartvisning av nåværende hull, poenginntasting og navigasjon mellom hull
 * @param navController Navigasjonskontroller for å navigere mellom skjermer
 * @param courseId ID-en til banen som spilles
 * @param sessionId Økts-ID for multiplayer (null for solo-spill)
 * @param isBottomBarVisible Om bunnlinjen er synlig
 * @param onBottomBarVisibilityChange Callback for å endre synlighet av bunnlinjen
 */
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
    val coroutineScope = rememberCoroutineScope()

    var courseName by remember { mutableStateOf("Disc Golf Bane") }
    var currentHole by remember { mutableIntStateOf(1) }
    var totalHoles by remember { mutableIntStateOf(18) }
    var holeScores by remember { mutableStateOf(List(18) { 0 }) }
    var showScores by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var holesData by remember { mutableStateOf<List<HoleData>>(emptyList()) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var locationPermissionDenied by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isSessionPlay = sessionId != null

    // Be om plasseringstillatelse for GPS-sporing
    my.application.ui.components.RequestLocationPermission(
        onPermissionGranted = {
            locationPermissionGranted = true
            locationPermissionDenied = false
        },
        onPermissionDenied = {
            locationPermissionGranted = false
            locationPermissionDenied = true
        }
    )

    // Last inn data - hent alltid banedata fra database, deretter gjenopprett øktfremgang
    LaunchedEffect(courseId, sessionId) {
        loading = true
        error = null

        if (courseId != null) {
            // Hent bane fra backend-database (alltid nødvendig for GPS-data)
            try {
                val result = trackRepository.getTrackById(courseId)
                result.onSuccess { track ->
                    // Last inn hulldata med GPS-koordinater
                    holesData = track.holesData ?: emptyList()

                    // Prøv å gjenopprette lagret økt hvis den matcher gjeldende bane
                    val savedSession = sessionManager.getGameSession()
                    if (savedSession != null &&
                        savedSession.courseId == courseId &&
                        savedSession.sessionId == sessionId) {
                        // Gjenopprett lagret øktfremgang
                        courseName = savedSession.courseName
                        currentHole = savedSession.currentHole
                        totalHoles = savedSession.totalHoles
                        holeScores = savedSession.holeScores
                    } else {
                        // Start nytt spill med data fra database
                        courseName = track.name
                        totalHoles = track.holes
                        holeScores = List(track.holes) { 0 }

                        if (isSessionPlay) {
                            courseName = "${track.name} - Multiplayer"
                        }

                        // Lagre innledende økt
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
            // Tilbakefall til demodata hvis ingen courseId
            totalHoles = 18
            holeScores = List(18) { 0 }
            courseName = "Demo Course"
            loading = false
        }
    }

    // Automatisk lagring når tilstand endres (hull eller poeng)
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

    // Slett økt ved avmontering hvis spillet er ferdig
    DisposableEffect(Unit) {
        onDispose {
            // Ikke slett - behold økten lagret for gjenoppretting
        }
    }

    if (loading) {
        LoadingScreen()
        return
    }

    if (error != null) {
        ErrorScreen(
            error = error!!,
            navController = navController
        )
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
            // Hovedinnhold med scrolling
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
            // Fremdriftsseksjon med kart- og poengoversikt-ikoner
            GameProgress(
                currentHole = currentHole,
                totalHoles = totalHoles,
                onMapClick = {
                    // Naviger til banekartskjerm
                    courseId?.let {
                        navController.navigate("course_map/$it")
                    }
                },
                onShowScoresClick = { showScores = !showScores },
                showScores = showScores
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Kartvisning av nåværende hull
            val currentHoleData = holesData.find { it.holeNumber == currentHole }


            if (currentHoleData != null &&
                currentHoleData.teeLatitude != null &&
                currentHoleData.teeLongitude != null &&
                currentHoleData.basketLatitude != null &&
                currentHoleData.basketLongitude != null) {
                HoleMapCard(
                    holeNumber = currentHole,
                    teePosition = LatLng(currentHoleData.teeLatitude, currentHoleData.teeLongitude),
                    basketPosition = LatLng(currentHoleData.basketLatitude, currentHoleData.basketLongitude),
                    par = currentHoleData.par,
                    hasLocationPermission = locationPermissionGranted
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Vis plassholder når ingen GPS-data med detaljert feilmelding
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

            // Poenginntasting for nåværende hull
            CurrentHoleCard(
                score = holeScores[currentHole - 1],
                onScoreChange = { newScore ->
                    holeScores = holeScores.toMutableList().apply {
                        this[currentHole - 1] = newScore
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Navigasjonsknapper (Forrige/Neste/Fullfør)
            NavigationButtons(
                currentHole = currentHole,
                totalHoles = totalHoles,
                onPrevious = { if (currentHole > 1) currentHole-- },
                onNext = { if (currentHole < totalHoles) currentHole++ },
                onFinish = {
                    // Fang opp nåværende tilstand før oppstart av coroutine
                    val currentHoleScores = holeScores
                    val currentHolesData = holesData
                    val currentCourseId = courseId

                    // Lagre spillkort til backend før avslutning
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val authManager = my.application.data.auth.AuthManager.getInstance(context)
                            val userRepository = my.application.data.repository.UserRepository.getInstance()
                            val userEmail = authManager.getUserEmail() ?: "unknown"

                            // Bygg hullresultater med hull-ID-er
                            val scoresToSave = if (currentHolesData.isNotEmpty()) {
                                // Bruk hulldata fra database hvis tilgjengelig
                                currentHolesData.mapIndexed { index, holeData ->
                                    my.application.data.remote.dto.HoleScore(
                                        hullId = holeData.holeId?.toString() ?: (index + 1).toString(),
                                        poeng = currentHoleScores.getOrNull(index) ?: 0
                                    )
                                }
                            } else {
                                // Generer hullresultater basert på antall hull spilt
                                currentHoleScores.mapIndexed { index, score ->
                                    my.application.data.remote.dto.HoleScore(
                                        hullId = (index + 1).toString(),
                                        poeng = score
                                    )
                                }
                            }

                            if (currentCourseId != null && scoresToSave.isNotEmpty()) {
                                val result = userRepository.createScorecard(
                                    baneId = currentCourseId,
                                    spillerNavn = userEmail,
                                    hullPoeng = scoresToSave
                                )
                                if (result.isSuccess) {
                                    android.util.Log.d("PlayDiscGolfScreen", "Scorecard saved successfully")
                                } else {
                                    android.util.Log.e("PlayDiscGolfScreen", "Failed to save scorecard: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PlayDiscGolfScreen", "Error saving scorecard", e)
                        } finally {
                            withContext(Dispatchers.Main) {
                                // Store game results for post-game screen
                                val authManager = my.application.data.auth.AuthManager.getInstance(context)
                                val userEmail = authManager.getUserEmail() ?: "Ukjent spiller"
                                val gameResults = my.application.data.GameResults(
                                    courseName = courseName,
                                    totalHoles = totalHoles,
                                    playerScores = mapOf(userEmail to holeScores),
                                    isSinglePlayer = sessionId == null
                                )
                                my.application.data.GameResultsManager.setGameResults(gameResults)

                                // Slett lagret økt når spillet er ferdig
                                sessionManager.clearGameSession()

                                // Navigate to post-game screen
                                val encodedCourseName = java.net.URLEncoder.encode(courseName, "UTF-8")
                                navController.navigate("post_game/$encodedCourseName/$totalHoles/${sessionId == null}") {
                                    popUpTo("mainmenu") { inclusive = false }
                                }
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Poengoversikt (vises/skjules ved klikk)
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

        // Merk: Fjernet den blokkerende sveipedeteksjonsboksen som forhindret knappetrykk
        // Brukere kan klikke på pilindikatoren nederst for å vise/skjule bunnlinjen

        // Animert pilindikator nederst - nå klikkbar for å vise/skjule navigasjon
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
                        // Bytt synlighet av bunnlinje når pil klikkes
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

/**
 * Lasteskjerm som vises mens banedata hentes
 */
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

/**
 * Feilskjerm som vises når noe går galt
 * Håndterer spesielt autentiseringsfeil og viser passende melding
 */
@Composable
fun ErrorScreen(error: String, navController: NavController) {
    // Sjekk om feilen er relatert til autentisering
    val isAuthError = error.contains("authentication", ignoreCase = true) ||
                      error.contains("not authenticated", ignoreCase = true) ||
                      error.contains("token", ignoreCase = true)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (isAuthError) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Logg inn for å spille",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            } else {
                Text(
                    text = error,
                    color = Color(0xFFE74C3C),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isAuthError) {
                        navController.navigate("login")
                    } else {
                        navController.popBackStack()
                    }
                }
            ) {
                Text(if (isAuthError) "Logg inn" else "Gå tilbake")
            }
        }
    }
}

/**
 * Komponent som viser spillfremdriften
 * Inkluderer hull-informasjon, fremdriftslinje, og knapper for kart og poengoversikt
 */
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

/**
 * Kort som viser og håndterer poenginntasting for nåværende hull
 * @param score Gjeldende poengsum
 * @param onScoreChange Callback når poengsummen endres
 */
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

/**
 * Kontroller for å øke/minske poengsummen
 * Viser minus-knapp, poengsum, og pluss-knapp
 */
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

/**
 * Navigasjonsknapper for å flytte mellom hull eller fullføre runden
 * Viser Forrige/Neste eller Fullfør basert på gjeldende hull
 */
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

/**
 * Oversikt over alle hull og deres poengsummer
 * Vises/skjules ved klikk på poengoversikt-knappen
 * @param holeScores Liste over poengsummer for hvert hull
 * @param currentHole Nåværende hull (markeres i oversikten)
 * @param onHoleClick Callback når bruker klikker på et hull
 */
@Composable
fun ScoreSummary(
    holeScores: List<Int>,
    currentHole: Int,
    onHoleClick: (Int) -> Unit
) {
    // Beregn optimalt antall kolonner basert på antall hull
    val totalHoles = holeScores.size
    val columnCount = when {
        totalHoles <= 6 -> 3  // 6 hull eller færre: 3 kolonner (større kort)
        totalHoles <= 9 -> 3  // 9 hull: 3 kolonner
        totalHoles <= 12 -> 4 // 12 hull: 4 kolonner
        totalHoles <= 15 -> 5 // 15 hull: 5 kolonner
        else -> 6             // 18 hull: 6 kolonner (standard)
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

/**
 * Enkelt element i poengoversikten som viser hullnummer og poengsum
 * @param holeNumber Hullnummer
 * @param score Poengsum for hullet
 * @param isCurrent Om dette er nåværende hull (markeres spesielt)
 * @param onClick Callback når hullet klikkes
 */
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

/**
 * Kartkort som viser nåværende hull med tee, kurv og brukerens posisjon
 * Inkluderer Google Maps med markører og linje mellom tee og kurv
 * @param holeNumber Hullnummer
 * @param teePosition GPS-koordinater for tee
 * @param basketPosition GPS-koordinater for kurv
 * @param par Par for hullet
 * @param hasLocationPermission Om appen har tillatelse til å vise brukerens posisjon
 */
@Composable
fun HoleMapCard(
    holeNumber: Int,
    teePosition: LatLng,
    basketPosition: LatLng,
    par: Int,
    hasLocationPermission: Boolean = false
) {
    val context = LocalContext.current
    val locationManager = remember { my.application.service.LocationManager(context) }
    val cameraPositionState = rememberCameraPositionState()
    var isMapLoaded by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Hent posisjonsoppdateringer - reagerer på tillatelses endringer
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationManager.getLocationUpdates(intervalMillis = 3000).collect { location ->
                userLocation = location
            }
        }
    }

    // Beregn grenser for å passe både tee og kurv i kartvisningen
    LaunchedEffect(teePosition, basketPosition) {
        val bounds = LatLngBounds.builder()
            .include(teePosition)
            .include(basketPosition)
            .build()

        try {
            kotlinx.coroutines.delay(100) // Liten forsinkelse for å sikre at kartet er klart
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 150),
                durationMs = 1000
            )
            isMapLoaded = true
        } catch (_: Exception) {
            // Fallback til senterposisjon hvis bounds-animasjon feiler
            val center = LatLng(
                (teePosition.latitude + basketPosition.latitude) / 2,
                (teePosition.longitude + basketPosition.longitude) / 2
            )
            cameraPositionState.position = CameraPosition.fromLatLngZoom(center, 16f)
            isMapLoaded = true
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
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = true,
                        compassEnabled = true,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true,
                        tiltGesturesEnabled = true,
                        rotationGesturesEnabled = true
                    ),
                    properties = MapProperties(
                        mapType = MapType.HYBRID,
                        isMyLocationEnabled = hasLocationPermission
                    )
                ) {
                    // Tee-markør (grønn)
                    Marker(
                        state = MarkerState(position = teePosition),
                        title = "Tee",
                        snippet = "Start punkt",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    // Kurvmarkør (rød)
                    Marker(
                        state = MarkerState(position = basketPosition),
                        title = "Basket",
                        snippet = "Mål",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // Brukerposisjons-markør (blå) - tilpasset markør for synlighet
                    userLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Din posisjon",
                            snippet = "Du er her",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }

                    // Linje som forbinder tee med kurv
                    Polyline(
                        points = listOf(teePosition, basketPosition),
                        color = Color(0xFF4A90E2),
                        width = 5f
                    )
                }

                // Forklarings-overlegg (legend)
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
                    if (userLocation != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF00B8D4), CircleShape)
                            )
                            Text(
                                text = "Deg",
                                fontSize = 11.sp,
                                color = Color(0xFF333333)
                            )
                        }
                    }
                }
            }
        }
    }
}
