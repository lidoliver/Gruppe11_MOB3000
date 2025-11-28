package my.application.ui.spill

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import my.application.data.local.MultiplayerSessionManager
import my.application.ui.components.AppTopBar

/**
 * Screen to join an existing multiplayer session by entering a session code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerJoinScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sessionCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Bli med i session",
                canNavigateBack = true,
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                text = "Skriv inn session-kode",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = sessionCode,
                onValueChange = { sessionCode = it.trim() },
                label = { Text("Session-kode") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    if (sessionCode.isBlank()) {
                        error = "Session-kode kan ikke være tom"
                        return@Button
                    }

                    isJoining = true
                    error = null

                    scope.launch {
                        try {
                            // For nå antar vi at banen allerede er valgt/eksisterer i den lokale sessionen på host.
                            // Joiner kun lokalt ved å opprette en LocalMultiplayerSession med kjent kode,
                            // mens baneinfo evt. kan fylles inn senere hvis nødvendig.
                            val session = MultiplayerSessionManager.joinSession(
                                sessionId = sessionCode,
                                courseId = "", // Kan erstattes med faktisk courseId hvis tilgjengelig
                                courseName = "", // Kan erstattes med faktisk courseName hvis tilgjengelig
                                totalHoles = 0 // Kan erstattes med faktisk antall hull hvis tilgjengelig
                            )

                            navController.navigate("multiplayer_lobby/${session.sessionId}") {
                                popUpTo("multiplayer_home") { inclusive = false }
                            }
                        } catch (t: Throwable) {
                            error = t.message ?: "Kunne ikke bli med i session"
                        } finally {
                            isJoining = false
                        }
                    }
                },
                enabled = !isJoining,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (isJoining) "Kobler til..." else "Bli med")
            }
        }
    }
}
