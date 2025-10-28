// In app/src/main/java/My/Application/ui/screen/mainmenu/MainMenuScreen.kt
package My.Application.ui.screen.mainmenu

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import My.Application.data.GameSessionManager
import My.Application.ui.components.AppTopBar
import My.Application.ui.theme.MyApplicationTheme

@Composable
fun MainMenuScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager(context) }
    var hasActiveSession by remember { mutableStateOf(false) }
    var savedSession by remember { mutableStateOf<My.Application.data.GameSession?>(null) }

    // Check for active session on composition
    LaunchedEffect(Unit) {
        hasActiveSession = sessionManager.hasActiveSession()
        savedSession = sessionManager.getGameSession()
    }

    Scaffold(topBar = {
        AppTopBar(
            title = "=Disc", // Your app's main title
            canNavigateBack = false, // The main menu doesn't have a back button
            navController = navController
        )
    }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Resume Game Button (only shown if there's an active session)
            if (hasActiveSession && savedSession != null) {
                Button(
                    onClick = {
                        // Navigate to the saved game
                        val session = savedSession!!
                        if (session.sessionId != null) {
                            navController.navigate("play_session/${session.courseId}/${session.sessionId}")
                        } else {
                            navController.navigate("play_course/${session.courseId}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fortsett Spill (Hull ${savedSession!!.currentHole}/${savedSession!!.totalHoles})")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    navController.navigate("start_round")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Ny Runde")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO: Handle 'View History' click */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spillehistorikk")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("my_profile") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Min Profil")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    MyApplicationTheme {
        MainMenuScreen(navController = rememberNavController())
    }
}
