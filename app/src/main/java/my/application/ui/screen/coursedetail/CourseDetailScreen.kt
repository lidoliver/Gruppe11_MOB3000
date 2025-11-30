package my.application.ui.screen.coursedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import my.application.data.remote.api.PlayerApi
import my.application.data.remote.dto.*
import my.application.ui.components.AppTopBar
import my.application.ui.theme.MyApplicationTheme
import my.application.ui.utils.DateUtils

@Composable
fun CourseDetailScreen(
    navController: NavController,
    viewModel: CourseDetailViewModel? = null,
    playerEmail: String? = null,
    courseName: String? = null,
    playerName: String? = null
) {
    val vm = viewModel ?: remember {
        val fakeApi = object : PlayerApi {
            override suspend fun getPlayerStats(email: String) = PlayerStats(
                email = email,
                totalGames = 0,
                averageScore = null,
                bestScore = null,
                worstScore = null,
                lastPlayedAt = null,
                totalRoundsByCourse = emptyMap()
            )
            override suspend fun getPlayerCourseGames(email: String, courseName: String) = CourseGameHistory(
                email = email,
                courseName = courseName,
                totalGames = 0,
                games = emptyList(),
                holeStatistics = emptyList()
            )
        }
        CourseDetailViewModel(fakeApi)
    }

    val emailToLoad = playerEmail ?: "test@example.com"
    val courseToLoad = courseName ?: "Unknown Course"

    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(emailToLoad, courseToLoad) {
        vm.loadCourseHistory(emailToLoad, courseToLoad)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = courseToLoad,
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is CourseDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CourseDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CourseDetailUiState.Success -> {
                CompositionLocalProvider(LocalNavController provides navController) {
                    CourseDetailContent(
                        courseHistory = state.courseHistory,
                        playerName = playerName,
                        playerEmail = emailToLoad,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CourseDetailContent(
    courseHistory: CourseGameHistory,
    playerName: String?,
    playerEmail: String,
    modifier: Modifier = Modifier
) {
    val navController = LocalNavController.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (playerName != null) "$playerName på ${courseHistory.courseName}" else "Din historikk på ${courseHistory.courseName}",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Summary card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Totalt ${courseHistory.totalGames} runder",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Velg en runde for å se detaljer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                }
            }

        // Liste over runder
        if (courseHistory.games.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ViewList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alle runder (${courseHistory.games.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(courseHistory.games) { game ->
                GameSelectionCard(
                    game = game,
                    coursePar = courseHistory.holeStatistics.sumOf { it.par ?: 3 },
                    onClick = {
                        navController?.navigate("gameDetail/${game.gameId}/$playerEmail/${courseHistory.courseName}")
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// CompositionLocal for NavController
val LocalNavController = compositionLocalOf<NavController?> { null }

/**
 * Kort som viser en enkelt runde med score og dato
 * Kan klikkes for å se detaljert rundestatistikk
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionCard(
    game: GameDetail,
    coursePar: Int,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Sports,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val scoreDiff = game.totalScore - coursePar
                    val scoreText = when {
                        scoreDiff > 0 -> "+$scoreDiff"
                        scoreDiff < 0 -> "$scoreDiff"
                        else -> "±0"
                    }
                    Text(
                        text = "${game.totalScore} kast ($scoreText)",
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            scoreDiff < 0 -> MaterialTheme.colorScheme.primary
                            scoreDiff > 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = DateUtils.formatTimestamp(game.playedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Se detaljer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun CourseDetailScreenPreview() {
    MyApplicationTheme {
        CourseDetailScreen(
            navController = rememberNavController(),
            viewModel = null,
            playerEmail = "test@example.com",
            courseName = "Test Bane",
            playerName = "Test Spiller"
        )
    }
}

