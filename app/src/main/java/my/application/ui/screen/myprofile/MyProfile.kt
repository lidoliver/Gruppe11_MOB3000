package my.application.ui.screen.myprofile

import my.application.ui.components.AppTopBar
import my.application.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Display user information
                        Text(
                            text = "Player Name: ${uiState.user.username ?: "N/A"}",
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

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

                        if (uiState.user.role != null) {
                            Text(
                                text = "Role: ${uiState.user.role}",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

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
                            onClick = { /* TODO: Navigate to change password screen */ },
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
}

@Preview(showBackground = true)
@Composable
fun MinProfilSkjermPreview() {
    MyApplicationTheme {
        MyProfileScreen(navController = rememberNavController())
    }
}
