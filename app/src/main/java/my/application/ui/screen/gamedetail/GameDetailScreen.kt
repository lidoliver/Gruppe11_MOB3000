package my.application.ui.screen.gamedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import my.application.data.remote.dto.*
import my.application.ui.components.AppTopBar
import my.application.ui.theme.MyApplicationTheme
import my.application.ui.utils.DateUtils
@Composable
fun GameDetailScreen(
    navController: NavController,
    gameDetail: GameDetail?,
    holeStatistics: List<HoleStatistic>?,
    courseName: String?
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = courseName ?: "Runddetaljer",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        if (gameDetail != null && holeStatistics != null) {
            GameDetailContent(
                game = gameDetail,
                holeStats = holeStatistics,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kunne ikke laste runddetaljer",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun GameDetailContent(
    game: GameDetail,
    holeStats: List<HoleStatistic>,
    modifier: Modifier = Modifier
) {
    val coursePar = holeStats.sumOf { it.par ?: 3 }
    val scoreDiff = game.totalScore - coursePar

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Resultatsammendragskort
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when {
                        scoreDiff < 0 -> MaterialTheme.colorScheme.primaryContainer
                        scoreDiff > 0 -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when {
                            scoreDiff < 0 -> Icons.Filled.EmojiEvents
                            scoreDiff > 0 -> Icons.Filled.ArrowUpward
                            else -> Icons.Filled.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            scoreDiff < 0 -> MaterialTheme.colorScheme.primary
                            scoreDiff > 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${game.totalScore} kast",
                        style = MaterialTheme.typography.displayMedium,
                        color = when {
                            scoreDiff < 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            scoreDiff > 0 -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                    Text(
                        text = when {
                            scoreDiff > 0 -> "+$scoreDiff over par"
                            scoreDiff < 0 -> "${-scoreDiff} under par"
                            else -> "Akkurat på par!"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            scoreDiff < 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            scoreDiff > 0 -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = DateUtils.formatTimestamp(game.playedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            scoreDiff < 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            scoreDiff > 0 -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Hole-by-hole breakdown header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kast per hull",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Hole details
        items(game.holeScores) { holeScore ->
            val holeStat = holeStats.find { it.holeId == holeScore.holeId }
            holeStat?.let { stat ->
                HoleDetailCard(
                    holeNumber = stat.holeNumber,
                    par = stat.par,
                    distance = stat.distance,
                    score = holeScore.score,
                    averageScore = stat.averageScore,
                    bestScore = stat.bestScore
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HoleDetailCard(
    holeNumber: Int,
    par: Int?,
    distance: Int?,
    score: Int,
    averageScore: String?,
    bestScore: Int?
) {
    val diff = par?.let { score - it } ?: 0

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hull $holeNumber",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            par?.let {
                                Text(
                                    text = "Par $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            distance?.let {
                                Text(
                                    text = "• ${it}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Score badge
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = when {
                        diff < 0 -> MaterialTheme.colorScheme.primaryContainer
                        diff > 0 -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = "$score kast",
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            diff < 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            diff > 0 -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Prestasjonsindikatorer
            if (averageScore != null || bestScore != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    averageScore?.let { avg ->
                        PerformanceIndicator(
                            icon = Icons.Filled.Remove,
                            label = "Ditt snitt",
                            value = avg
                        )
                    }
                    bestScore?.let { best ->
                        PerformanceIndicator(
                            icon = Icons.Filled.Star,
                            label = "Din beste",
                            value = best.toString()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
fun GameDetailScreenPreview() {
    MyApplicationTheme {
        val mockGame = GameDetail(
            gameId = 1,
            courseId = 1,
            totalScore = 54,
            coursePar = 54,
            playedAt = "2025-01-15 10:30",
            holeScores = listOf(
                GameHoleScore(1, 3),
                GameHoleScore(2, 4)
            )
        )
        val mockHoleStats = listOf(
            HoleStatistic(1, 1, 3, 85, "3.2", 2, 5),
            HoleStatistic(2, 2, 3, 95, "3.5", 3, 6)
        )
        GameDetailScreen(
            navController = rememberNavController(),
            gameDetail = mockGame,
            holeStatistics = mockHoleStats,
            courseName = "Test Bane"
        )
    }
}

