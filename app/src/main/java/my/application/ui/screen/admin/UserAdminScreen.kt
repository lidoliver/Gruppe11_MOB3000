package my.application.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.data.remote.dto.UserDto
import my.application.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAdminScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: UserAdminViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return UserAdminViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )

    val uiState = viewModel.uiState
    var searchQuery by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserDto?>(null) }
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
                title = "User Administration",
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search users by email, username, or name") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        Button(onClick = { viewModel.loadAllUsers() }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No users found" else "No users available",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredUsers) { user ->
                            UserCard(
                                user = user,
                                onBan = {
                                    selectedUser = user
                                    actionType = "ban"
                                    showConfirmDialog = true
                                },
                                onUnban = {
                                    selectedUser = user
                                    actionType = "unban"
                                    showConfirmDialog = true
                                },
                                onDelete = {
                                    selectedUser = user
                                    actionType = "delete"
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
    if (showConfirmDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    when (actionType) {
                        "ban" -> "Ban User"
                        "unban" -> "Unban User"
                        "delete" -> "Delete User"
                        else -> "Confirm Action"
                    }
                )
            },
            text = {
                Text(
                    when (actionType) {
                        "ban" -> "Are you sure you want to ban ${selectedUser?.email}?"
                        "unban" -> "Are you sure you want to unban ${selectedUser?.email}?"
                        "delete" -> "Are you sure you want to delete ${selectedUser?.email}? This action cannot be undone."
                        else -> "Confirm this action?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        selectedUser?.let { user ->
                            when (actionType) {
                                "ban" -> viewModel.banUser(
                                    user.email,
                                    onSuccess = {
                                        actionMessage = "User banned successfully"
                                        showSnackbar = true
                                    },
                                    onError = { error ->
                                        actionMessage = error
                                        showSnackbar = true
                                    }
                                )
                                "unban" -> viewModel.unbanUser(
                                    user.email,
                                    onSuccess = {
                                        actionMessage = "User unbanned successfully"
                                        showSnackbar = true
                                    },
                                    onError = { error ->
                                        actionMessage = error
                                        showSnackbar = true
                                    }
                                )
                                "delete" -> viewModel.deleteUser(
                                    user.email,
                                    onSuccess = {
                                        actionMessage = "User deleted successfully"
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
                        containerColor = if (actionType == "delete") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCard(
    user: UserDto,
    onBan: () -> Unit,
    onUnban: () -> Unit,
    onDelete: () -> Unit
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
                text = user.username ?: "No username",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user.firstName != null || user.lastName != null) {
                Text(
                    text = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Role: ${user.role ?: "user"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (user.role) {
                        "admin" -> MaterialTheme.colorScheme.primary
                        "banned" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Action buttons - only show if not admin
                if (user.role != "admin") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (user.role == "banned") {
                            IconButton(onClick = onUnban) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Unban",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(onClick = onBan) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Ban",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

