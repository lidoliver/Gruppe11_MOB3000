package my.application.ui.spill

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import my.application.data.auth.AuthManager
import my.application.data.repository.MultiplayerRepository
import my.application.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinMultiplayerSessionScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { MultiplayerRepository.getInstance(context) }
    val authManager = remember { AuthManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var sessionCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAuthenticated = authManager.getUserEmail() != null
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Bli med i spill",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isAuthenticated) {
                // Not authenticated - show login prompt
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Logg inn for å spille",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Du må logge inn for å bli med i multiplayer-spill",
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Skriv inn spillkoden",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Få spillkoden fra verten som opprettet spillet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Error message
                    error?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Session code input
                    OutlinedTextField(
                        value = sessionCode,
                        onValueChange = {
                            sessionCode = it.trim()
                            error = null
                        },
                        label = { Text("Spillkode") },
                        placeholder = { Text("f.eks. abc123-def456-ghi789") },
                        singleLine = true,
                        enabled = !isJoining,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Join button
                    Button(
                        onClick = {
                            if (sessionCode.isBlank()) {
                                error = "Vennligst skriv inn en spillkode"
                                return@Button
                            }

                            isJoining = true
                            error = null

                            coroutineScope.launch {
                                val result = repository.joinSession(sessionCode)
                                result.onSuccess {
                                    // Successfully joined, navigate to lobby
                                    navController.navigate("multiplayer_lobby/$sessionCode") {
                                        popUpTo("join_multiplayer_session") { inclusive = true }
                                    }
                                }.onFailure { exception ->
                                    error = when {
                                        exception.message?.contains("not found", ignoreCase = true) == true ->
                                            "Spillet ble ikke funnet. Sjekk spillkoden."
                                        exception.message?.contains("already started", ignoreCase = true) == true ->
                                            "Spillet har allerede startet."
                                        exception.message?.contains("Permission denied", ignoreCase = true) == true ->
                                            "Tilgang nektet. Sjekk Firebase-innstillinger."
                                        else -> exception.message ?: "Kunne ikke bli med i spillet"
                                    }
                                    isJoining = false
                                }
                            }
                        },
                        enabled = !isJoining && sessionCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isJoining) "Blir med..." else "Bli med i spill")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Help text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 Tips",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Spillkoden vises i lobby-skjermen til verten. " +
                                        "Du finner den øverst på skjermen under 'Session Code'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

