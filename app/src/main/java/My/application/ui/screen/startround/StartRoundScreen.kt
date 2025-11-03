// In app/src/main/java/My/Application/ui/screen/startround/StartRoundScreen.kt

package My.application.ui.screen.startround

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import My.application.ui.components.AppTopBar
import My.application.ui.theme.MyApplicationTheme

@Composable
fun StartRoundScreen(navController: NavController) {
    Scaffold(
        topBar = {
            // Pass the title and back navigation info to our AppTopBar
            AppTopBar(
                title = "Start Runde",
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
            verticalArrangement = Arrangement.Center, // Center the cards vertically
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hvordan vil du spille?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp) // Add space below the title
            )

            // "Play Alone" option
            GameModeCard(
                icon = Icons.Filled.Person,
                title = "Spill alene",
                description = "Start en runde kun for deg selv.",
                onClick = {
                    // Navigate to course selection screen
                    navController.navigate("course_selection_alone")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // "Invite Players" option
            GameModeCard(
                icon = Icons.Filled.Groups,
                title = "Inviter spillere",
                description = "Lag en gruppe og spill med venner.",
                onClick = { /* TODO: Navigate to multiplayer setup */ }
            )
        }
    }
}

// A new, reusable composable for our stylish option cards
@Composable
fun GameModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the whole card clickable
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // The text describes the icon's purpose
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun StartRoundScreenPreview() {
    MyApplicationTheme {
        StartRoundScreen(navController = rememberNavController())
    }
}
