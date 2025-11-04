package my.application.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import my.application.data.GameSessionManager

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("main_menu", Icons.Default.Home, "Hjem")
    object Tips : BottomNavItem("tips", Icons.Default.Info, "Tips")
    object Play : BottomNavItem("start_round", Icons.Default.PlayArrow, "Spill")
    object Messages : BottomNavItem("messages", Icons.Default.Email, "Meldinger")
    object Profile : BottomNavItem("my_profile", Icons.Default.Person, "Profil")
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager(context) }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Tips,
        BottomNavItem.Play,
        BottomNavItem.Messages,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = if (item == BottomNavItem.Play) {
                            Modifier.size(32.dp)
                        } else {
                            Modifier.size(24.dp)
                        }
                    )
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    // Special handling for Play button - check for active game
                    if (item == BottomNavItem.Play) {
                        val activeSession = sessionManager.getGameSession()
                        if (activeSession != null) {
                            // Navigate to the active game
                            val route = if (activeSession.sessionId != null) {
                                "play_session/${activeSession.courseId}/${activeSession.sessionId}"
                            } else {
                                "play_course/${activeSession.courseId}"
                            }
                            navController.navigate(route) {
                                popUpTo("main_menu") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            // No active game, go to start round screen
                            navController.navigate(item.route) {
                                popUpTo("main_menu") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    } else if (item == BottomNavItem.Home) {
                        // Special handling for Home button - always navigate to main menu
                        // and clear everything above it in the back stack
                        if (currentRoute != "main_menu") {
                            navController.navigate("main_menu") {
                                popUpTo("main_menu") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    } else if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo("main_menu") {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (item == BottomNavItem.Play) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = if (item == BottomNavItem.Play) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

