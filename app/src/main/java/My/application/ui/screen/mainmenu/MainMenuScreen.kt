// In app/src/main/java/My/Application/ui/screen/mainmenu/MainMenuScreen.kt
package My.application.ui.screen.mainmenu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import My.application.data.GameSessionManager
import My.application.ui.components.AppTopBar
import My.application.ui.theme.MyApplicationTheme

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val description: String = ""
)

@Composable
fun MainMenuScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager(context) }
    var hasActiveSession by remember { mutableStateOf(false) }
    var savedSession by remember { mutableStateOf<My.application.data.GameSession?>(null) }

    // Check for active session on composition
    LaunchedEffect(Unit) {
        hasActiveSession = sessionManager.hasActiveSession()
        savedSession = sessionManager.getGameSession()
    }

    // Define menu items
    val menuItems = listOf(
        MenuItem(
            title = "Start Ny Runde",
            icon = Icons.Default.Add,
            route = "start_round",
            description = "Begynn et nytt spill"
        ),
        MenuItem(
            title = "Min Profil",
            icon = Icons.Default.Person,
            route = "my_profile",
            description = "Se din profil"
        ),
        MenuItem(
            title = "Meldinger",
            icon = Icons.Default.Email,
            route = "messages",
            description = "Se dine meldinger"
        ),
        MenuItem(
            title = "Tips & Råd",
            icon = Icons.Default.Star,
            route = "tips",
            description = "Lær disc golf"
        ),
        MenuItem(
            title = "Banevelging",
            icon = Icons.Default.Place,
            route = "course_selection_alone",
            description = "Velg en bane"
        )
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "=Disc",
                canNavigateBack = false,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome Text
            Text(
                text = "Velkommen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Resume Game Card (only shown if there's an active session)
            if (hasActiveSession && savedSession != null) {
                ElevatedCard(
                    onClick = {
                        val session = savedSession!!
                        if (session.sessionId != null) {
                            navController.navigate("play_session/${session.courseId}/${session.sessionId}")
                        } else {
                            navController.navigate("play_course/${session.courseId}")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Fortsett Spill",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Hull ${savedSession!!.currentHole} av ${savedSession!!.totalHoles}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Menu Items Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(menuItems) { item ->
                    MenuItemCard(
                        item = item,
                        onClick = { navController.navigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    MyApplicationTheme {
        MainMenuScreen(navController = rememberNavController())
    }
}
