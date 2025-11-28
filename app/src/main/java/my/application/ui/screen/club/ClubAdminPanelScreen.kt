package my.application.ui.screen.club

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
import my.application.data.remote.dto.ClubJoinRequestDto
import my.application.ui.components.AppTopBar

@Composable
fun ClubAdminPanelScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: ClubAdminPanelViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ClubAdminPanelViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )

    val uiState = viewModel.uiState
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<ClubJoinRequestDto?>(null) }
    var actionType by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Klubbadmin Panel",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.requests.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadJoinRequests() }) {
                            Text("Prøv på nytt")
                        }
                    }
                }
                uiState.clubId == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ingen klubb funnet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                uiState.requests.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ingen søknader",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Du har ingen ventende medlemssøknader",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Medlemssøknader (${uiState.requests.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(uiState.requests) { request ->
                            JoinRequestCard(
                                request = request,
                                onApprove = {
                                    selectedRequest = request
                                    actionType = "approve"
                                    showConfirmDialog = true
                                },
                                onReject = {
                                    selectedRequest = request
                                    actionType = "reject"
                                    showConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Success message
            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(uiState.successMessage!!)
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearMessages()
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
                    if (actionType == "approve") "Godkjenn søknad" else "Avvis søknad"
                )
            },
            text = {
                Text(
                    if (actionType == "approve")
                        "Er du sikker på at du vil godkjenne ${selectedRequest?.username ?: selectedRequest?.userEmail} sin søknad om medlemskap?"
                    else
                        "Er du sikker på at du vil avvise ${selectedRequest?.username ?: selectedRequest?.userEmail} sin søknad?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        selectedRequest?.let { request ->
                            if (actionType == "approve") {
                                viewModel.approveRequest(request.id)
                            } else {
                                viewModel.rejectRequest(request.id)
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
fun JoinRequestCard(
    request: ClubJoinRequestDto,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = request.username ?: request.userEmail,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = request.userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (request.message != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Melding:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = request.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sendt: ${request.requestDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
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

