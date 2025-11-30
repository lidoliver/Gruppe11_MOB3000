package my.application.ui.spill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar

/**
 * Post-game screen showing final scores and statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostGameScreen(
    navController: NavController,
    courseName: String,
    totalHoles: Int,
    playerScores: Map<String, List<Int>>, // email to list of scores per hole
    isSinglePlayer: Boolean = true
) {
    // Calculate totals and statistics
    val playerStats = playerScores.mapValues { (email, scores) ->
        val total = scores.sum()
        val par = totalHoles * 3 // Assuming par 3 per hole
        val relativeToPar = total - par
        PlayerStats(
            email = email,
            scores = scores,
            total = total,
            relativeToPar = relativeToPar,
            bestHole = scores.minOrNull() ?: 0,
            worstHole = scores.maxOrNull() ?: 0,
            averageScore = if (scores.isNotEmpty()) scores.average() else 0.0
        )
    }

    val sortedPlayers = playerStats.entries.sortedBy { it.value.total }
    val winner = sortedPlayers.firstOrNull()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Resultat",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSinglePlayer) "Runde fullført!" else "Spill ferdig!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = courseName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "$totalHoles hull",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Winner announcement (multiplayer only)
            if (!isSinglePlayer && winner != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "🏆 Vinner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = winner.key,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${winner.value.total} slag (${formatRelativeToPar(winner.value.relativeToPar)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Player scorecards
            items(sortedPlayers) { entry ->
                val (email, stats) = entry
                val position = sortedPlayers.indexOfFirst { it.key == email } + 1
                PlayerScoreCard(
                    playerEmail = email,
                    stats = stats,
                    position = position,
                    showPosition = !isSinglePlayer,
                    totalHoles = totalHoles
                )
            }

            // Detailed hole-by-hole scores
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detaljert scorekort",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                HoleByHoleScoreCard(playerScores, totalHoles)
            }

            // Action buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            navController.navigate("start_runde") {
                                popUpTo("mainmenu") { inclusive = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Spill på nytt")
                    }

                    OutlinedButton(
                        onClick = {
                            navController.navigate("mainmenu") {
                                popUpTo("mainmenu") { inclusive = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tilbake til hovedmeny")
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PlayerScoreCard(
    playerEmail: String,
    stats: PlayerStats,
    position: Int,
    showPosition: Boolean,
    totalHoles: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showPosition) {
                        Surface(
                            color = when (position) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "#$position",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (position <= 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = playerEmail.substringBefore("@"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playerEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.total}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatRelativeToPar(stats.relativeToPar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            stats.relativeToPar < 0 -> Color(0xFF4CAF50)
                            stats.relativeToPar > 0 -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem("Beste", "${stats.bestHole}", Icons.Default.TrendingDown)
                StatItem("Verste", "${stats.worstHole}", Icons.Default.TrendingUp)
                StatItem("Snitt", String.format("%.1f", stats.averageScore), Icons.Default.Analytics)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HoleByHoleScoreCard(
    playerScores: Map<String, List<Int>>,
    totalHoles: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hull",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                playerScores.keys.forEach { email ->
                    Text(
                        text = email.substringBefore("@").take(8),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Score rows
            for (hole in 1..totalHoles) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$hole",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    playerScores.values.forEach { scores ->
                        val score = scores.getOrNull(hole - 1) ?: 0
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (hole < totalHoles) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Totalt",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                playerScores.values.forEach { scores ->
                    Text(
                        text = "${scores.sum()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatRelativeToPar(relativeToPar: Int): String {
    return when {
        relativeToPar < 0 -> "$relativeToPar (under par)"
        relativeToPar > 0 -> "+$relativeToPar (over par)"
        else -> "Par"
    }
}

data class PlayerStats(
    val email: String,
    val scores: List<Int>,
    val total: Int,
    val relativeToPar: Int,
    val bestHole: Int,
    val worstHole: Int,
    val averageScore: Double
)

