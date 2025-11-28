package my.application.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.data.remote.dto.ClubRequestDto
import my.application.ui.components.AppTopBar

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
    var actionMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    // Handle auth errors
    LaunchedEffect(uiState.authError) {
        if (uiState.authError) {
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
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(actionMessage)
                }
            }
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
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadClubRequests() }) {
                            Text("Prøv på nytt")
                        }
                    }
                }
                uiState.clubRequests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingen klubb forespørsler",
                            style = MaterialTheme.typography.bodyLarge
                        )
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
                    if (actionType == "approve") "Godta Klubbforespørsel" else "Avvis Klubbforespørsel"
                )
            },
            text = {
                Text(
                    if (actionType == "approve")
                        "Er du sikker på at du vil godkjenne forespørselen for \"${selectedRequest?.clubName}\"? Brukeren vil bli tildelt klubbadmin."
                    else
                        "Are you sure you want to reject the club request for \"${selectedRequest?.clubName}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        selectedRequest?.let { request ->
                            if (actionType == "approve") {
                                viewModel.approveClubRequest(
                                    request.id,
                                    onSuccess = {
                                        actionMessage = "Klubb godkjent"
                                        showSnackbar = true
                                    },
                                    onError = { error ->
                                        actionMessage = error
                                        showSnackbar = true
                                    }
                                )
                            } else {
                                viewModel.rejectClubRequest(
                                    request.id,
                                    onSuccess = {
                                        actionMessage = "Klubb avvist"
                                        showSnackbar = true
                                    },
                                    onError = { error ->
                                        actionMessage = error
                                        showSnackbar = true
                                    }
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "approve")
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

