package my.application.ui.screen.playerhistory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import my.application.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.*
import my.application.data.remote.api.PlayerApi
import my.application.data.remote.dto.PlayerStats
import my.application.data.remote.dto.CourseGameHistory
import java.util.Locale
@Composable
fun PlayerHistoryScreen(
    navController: NavController,
    viewModel: PlayerHistoryViewModel? = null,
    playerEmail: String? = null,
    playerName: String? = null
) {
    // Lite vw
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
        PlayerHistoryViewModel(fakeApi)
    }

    val emailToLoad = playerEmail ?: "test@example.com"

    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(emailToLoad) {
        vm.load(emailToLoad)
    }
    Scaffold(
        topBar = {
            AppTopBar (
                title = "Spillerhistorikk",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (playerName != null) "$playerName sin spillerhistorikk" else "Din Spillerhistorikk",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            when (uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
                is UiState.Error -> {
                    Text(
                        text = (uiState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                is UiState.Success -> {
                    val stats = (uiState as UiState.Success).stats

                    // Main statistics card
                    ElevatedCard(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Spillerstatistikk",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Totalt antall runder: ${stats.totalGames}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Gjennomsnittsscore: ${stats.averageScore?.let { String.format(Locale.US, "%.1f", it) } ?: "-"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Beste score: ${stats.bestScore ?: "-"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Dårligste score: ${stats.worstScore ?: "-"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                stats.lastPlayedAt?.let {
                                    Text(
                                        text = "Sist spilt: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Per-course breakdown
                    if (stats.totalRoundsByCourse.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GolfCourse,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Runder per bane",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        stats.totalRoundsByCourse.forEach { (courseName, roundCount) ->
                            ElevatedCard(
                                onClick = {
                                    navController.navigate("courseDetail/$emailToLoad/$courseName${if (playerName != null) "/$playerName" else ""}")
                                },
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Terrain,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = courseName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "$roundCount runder spilt",
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
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PlayerHistoryScreenPreview() {
    MyApplicationTheme {
        PlayerHistoryScreen(
            navController = rememberNavController(),
            viewModel = null,
            playerEmail = "test@example.com"
        )
    }
}
