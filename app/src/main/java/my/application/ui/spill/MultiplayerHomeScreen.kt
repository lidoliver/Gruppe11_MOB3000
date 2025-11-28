package my.application.ui.spill

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar

/**
 * Simple multiplayer entry screen.
 * - "Opprett session" creates a new Firebase game and navigates to the lobby.
 * - "Bli med i session" lets the user enter a session code and join.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerHomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Multiplayer",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Velg multiplayer-alternativ",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    navController.navigate("multiplayer_create")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Opprett session")
            }

            OutlinedButton(
                onClick = {
                    navController.navigate("multiplayer_join")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Bli med i session")
            }
        }
    }
}

