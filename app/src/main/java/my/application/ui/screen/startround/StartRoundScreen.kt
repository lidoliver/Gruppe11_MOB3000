// In app/src/main/java/My/Application/ui/screen/startround/StartRoundScreen.kt

package my.application.ui.screen.startround

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import my.application.ui.components.AppTopBar
import my.application.ui.theme.MyApplicationTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun StartRoundScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: StartRoundViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return StartRoundViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val activeSessionManager = remember { my.application.data.ActiveSessionManager.getInstance(context) }
    val activeSessionId by remember { mutableStateOf(activeSessionManager.getActiveSessionId()) }
    Scaffold(
        topBar = {
            // Send tittel og tilbakenavigeringsinformasjon til vår AppTopBar
            AppTopBar(
                title = "Start Runde",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        if (!isAuthenticated) {
            // Not authenticated - show login prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Du må logge inn for å starte en runde",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Logg inn")
                }
            }
        } else {
            // Authenticated - show game mode selection
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show alert if there's an active multiplayer session
                if (activeSessionId != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Du har en aktiv multiplayer-lobby",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Fullfør eller forlat lobbyen før du starter et nytt spill.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    navController.navigate("multiplayer_lobby/$activeSessionId")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gå tilbake til lobby")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = "Hvordan vil du spille?",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 48.dp) // Add space below the title
                )

                // "Play Alone" option
                GameModeCard(
                    icon = Icons.Filled.Person,
                    title = "Spill alene",
                    description = "Start en runde kun for deg selv.",
                    onClick = {
                        // Navigate to course selection screen
                        navController.navigate("course_selection_alone")
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // "Invite Players" option
                GameModeCard(
                    icon = Icons.Filled.Groups,
                    title = "Inviter spillere",
                    description = "Lag en gruppe og spill med venner.",
                    onClick = {
                        navController.navigate("course_selection_multiplayer")
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // "Join Game" option - THIS IS WHERE YOU ENTER SESSION CODES!
                GameModeCard(
                    icon = Icons.Filled.QrCodeScanner,
                    title = "Bli med i spill",
                    description = "Skriv inn spillkode for å bli med.",
                    onClick = {
                        navController.navigate("join_multiplayer_session")
                    }
                )
            }
        }
    }
}

/**
 * Gjenbrukbar composable for spillmoduskort
 */
@Composable
fun GameModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Gjør hele kortet klikkbart
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // Teksten beskriver ikonets formål
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun StartRoundScreenPreview() {
    MyApplicationTheme {
        StartRoundScreen(navController = rememberNavController())
    }
}
