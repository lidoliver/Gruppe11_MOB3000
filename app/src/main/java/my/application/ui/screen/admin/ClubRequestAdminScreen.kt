package my.application.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import my.application.data.remote.dto.ClubRequestDto
import my.application.ui.components.AppTopBar

/**
 * Admin-skjerm for å håndtere klubbforespørsler
 * Viser ventende forespørsler om opprettelse av klubber
 * Admin kan godkjenne eller avvise forespørsler
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubRequestAdminScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: ClubRequestAdminViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ClubRequestAdminViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )

    val uiState = viewModel.uiState
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<ClubRequestDto?>(null) }
    var actionType by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Håndter autentiseringsfeil - naviger tilbake til innlogging
    LaunchedEffect(uiState.authError) {
        if (uiState.authError) {
            snackbarHostState.showSnackbar(
                message = "Autentiseringsfeil. Vennligst logg inn på nytt.",
                duration = SnackbarDuration.Long
            )
            navController.navigate("login") {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Klub Requests",
                canNavigateBack = true,
                navController = navController
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Laster klubbforespørsler...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "En feil oppstod",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "Ukjent feil",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadClubRequests() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prøv på nytt")
                        }
                    }
                }
                uiState.clubRequests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "No requests",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ingen klubbforespørsler",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Det er ingen ventende forespørsler om opprettelse av klubber",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { viewModel.loadClubRequests() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Oppdater")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.clubRequests) { request ->
                            ClubRequestCard(
                                request = request,
                                onApprove = {
                                    selectedRequest = request
                                    actionType = "Godkjenn"
                                    showConfirmDialog = true
                                },
                                onReject = {
                                    selectedRequest = request
                                    actionType = "Avvis"
                                    showConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    if (actionType == "Godkjenn") "Godta Klubbforespørsel" else "Avvis Klubbforespørsel"
                )
            },
            text = {
                Text(
                    if (actionType == "Godkjenn")
                        "Er du sikker på at du vil godkjenne forespørselen for \"${selectedRequest?.clubName}\"? Brukeren vil bli tildelt klubbadmin."
                    else
                        "Er du sikker på at du vil avvise klubbforespørselen for \"${selectedRequest?.clubName}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        selectedRequest?.let { request ->
                            if (actionType == "Godkjenn") {
                                viewModel.approveClubRequest(
                                    request.id,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Klubb godkjent vellykket",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Feil ved godkjenning: $error",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                )
                            } else {
                                viewModel.rejectClubRequest(
                                    request.id,
                                    onSuccess = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Klubb avvist",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onError = { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Feil ved avvisning: $error",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "Godkjenn")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Bekreft")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Avbryt")
                }
            }
        )
    }
}

/**
 * Kort som viser en enkelt klubbforespørsel
 * Viser klubbnavn, beskrivelse, hvem som søkte og handlingsknapper
 * @param request Klubbforespørselen som skal vises
 * @param onApprove Callback når admin godkjenner forespørselen
 * @param onReject Callback når admin avviser forespørselen
 */
@Composable
fun ClubRequestCard(
    request: ClubRequestDto,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = request.clubName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Requested by: ${request.requestedBy}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (request.location != null) {
                Text(
                    text = "Location: ${request.location}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (request.description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Submitted: ${request.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Approve",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Godkjenn")
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Avvis")
                }
            }
        }
    }
}

