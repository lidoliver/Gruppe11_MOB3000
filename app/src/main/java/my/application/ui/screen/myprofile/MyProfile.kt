package my.application.ui.screen.myprofile

import my.application.ui.components.AppTopBar
import my.application.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import my.application.data.auth.AuthManager
import kotlinx.coroutines.launch

@Composable
fun MyProfileScreen(
    navController: NavController,
    viewModel: MyProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val authManager = AuthManager.getInstance(context)
    val userEmail by authManager.getUserEmailFlow().collectAsState(initial = null)
    val uiState = viewModel.uiState
    val coroutineScope = rememberCoroutineScope()
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Check authentication and redirect to login if not authenticated
    LaunchedEffect(userEmail) {
        if (userEmail == null) {
            // User is not logged in, redirect to login
            navController.navigate("login") {
                popUpTo("main_menu") { inclusive = false }
            }
        }
    }

    // If not authenticated, show loading and return early
    if (userEmail == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Load user profile when email is available
    LaunchedEffect(userEmail) {
        userEmail?.let { email ->
            viewModel.loadUserProfile(email)
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Min Profil", canNavigateBack = true, navController = navController) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            userEmail?.let { viewModel.loadUserProfile(it) }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.user != null -> {
                    var isEditingUsername by remember { mutableStateOf(false) }
                    var editedUsername by remember { mutableStateOf(uiState.user.username ?: "") }
                    var usernameError by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Username section with edit capability
                        if (isEditingUsername) {
                            OutlinedTextField(
                                value = editedUsername,
                                onValueChange = {
                                    editedUsername = it
                                    usernameError = ""
                                },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = usernameError.isNotEmpty(),
                                supportingText = if (usernameError.isNotEmpty()) {
                                    { Text(usernameError, color = MaterialTheme.colorScheme.error) }
                                } else null,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (editedUsername.isBlank()) {
                                            usernameError = "Username cannot be empty"
                                        } else if (editedUsername.contains("@")) {
                                            usernameError = "Username cannot contain @ symbol"
                                        } else if (editedUsername.length < 3) {
                                            usernameError = "Username must be at least 3 characters"
                                        } else if (editedUsername.length > 64) {
                                            usernameError = "Username must be less than 64 characters"
                                        } else {
                                            userEmail?.let { email ->
                                                viewModel.updateUsername(
                                                    email = email,
                                                    newUsername = editedUsername,
                                                    onSuccess = {
                                                        isEditingUsername = false
                                                        usernameError = ""
                                                    },
                                                    onError = { error ->
                                                        usernameError = error
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save")
                                }
                                OutlinedButton(
                                    onClick = {
                                        isEditingUsername = false
                                        editedUsername = uiState.user.username ?: ""
                                        usernameError = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Username: ${uiState.user.username ?: "Not set"}",
                                    fontSize = 20.sp,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                IconButton(onClick = {
                                    isEditingUsername = true
                                    editedUsername = uiState.user.username ?: ""
                                }) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                        contentDescription = "Edit username"
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.user.firstName != null || uiState.user.lastName != null) {
                            Text(
                                text = "${uiState.user.firstName ?: ""} ${uiState.user.lastName ?: ""}".trim(),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "Email: ${uiState.user.email}",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.user.clubName != null) {
                            Text(
                                text = "Club: ${uiState.user.clubName}",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "Rounds Played: ${uiState.scorecards.size}",
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Button to change password
                        Button(
                            onClick = { showChangePasswordDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Password")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logout button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    authManager.clearAuth()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var dialogError by remember { mutableStateOf("") }
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    onSuccess = {
                        showChangePasswordDialog = false
                        // Optionally show a success message
                    },
                    onError = { error ->
                        dialogError = error
                    }
                )
            },
            externalError = dialogError
        )
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    externalError: String = ""
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Update error message when external error changes
    LaunchedEffect(externalError) {
        if (externalError.isNotEmpty()) {
            errorMessage = externalError
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = ""
                    },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> {
                            errorMessage = "Please enter current password"
                        }
                        newPassword.isEmpty() -> {
                            errorMessage = "Please enter new password"
                        }
                        newPassword.length < 6 -> {
                            errorMessage = "Password must be at least 6 characters"
                        }
                        newPassword != confirmPassword -> {
                            errorMessage = "Passwords do not match"
                        }
                        else -> {
                            onConfirm(currentPassword, newPassword)
                        }
                    }
                }
            ) {
                Text("Change Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MinProfilSkjermPreview() {
    MyApplicationTheme {
        MyProfileScreen(navController = rememberNavController())
    }
}
