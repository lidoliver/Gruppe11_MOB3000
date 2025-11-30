package my.application.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import my.application.data.GameSessionManager
import my.application.ui.screen.friends.FriendsViewModel

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("main_menu", Icons.Default.Home, "Hjem")
    object Tips : BottomNavItem("tips", Icons.Default.Info, "Tips")
    object Play : BottomNavItem("start_round", Icons.Default.PlayArrow, "Spill")
    object Friends : BottomNavItem("friends", Icons.Default.Person, "Venner")
    object Profile : BottomNavItem("my_profile", Icons.Default.AccountCircle, "Profil")
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager(context) }

    // Get FriendsViewModel to track friend request count
    val friendsViewModel: FriendsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as android.app.Application
        )
    )

    // Collect friend request count
    val friendRequestsCount by friendsViewModel.friendRequestsCount.collectAsState()

    // Reload friend requests count periodically (every 30 seconds) to update badge
    // This ensures the badge updates even when user is not on the Friends screen
    LaunchedEffect(Unit) {
        friendsViewModel.loadFriendRequestsCount() // Initial load

        // Poll every 30 seconds for new friend requests
        while (true) {
            kotlinx.coroutines.delay(30_000) // 30 seconds
            friendsViewModel.loadFriendRequestsCount()
        }
    }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Tips,
        BottomNavItem.Play,
        BottomNavItem.Friends,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Also refresh when navigation changes (user switches screens)
    LaunchedEffect(currentRoute) {
        friendsViewModel.loadFriendRequestsCount()
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    // Add badge for Friends icon when there are pending requests
                    if (item == BottomNavItem.Friends && friendRequestsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = if (friendRequestsCount > 9) "9+" else friendRequestsCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = if (item == BottomNavItem.Play) {
                                Modifier.size(32.dp)
                            } else {
                                Modifier.size(24.dp)
                            }
                        )
                    }
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    // Spesiell håndtering for Spill-knapp - sjekk for aktivt spill
                    if (item == BottomNavItem.Play) {
                        val activeSession = sessionManager.getGameSession()
                        if (activeSession != null) {
                            // Naviger til det aktive spillet
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
                            // Pop opp til startdestinasjon for å unngå å bygge opp en stor stack
                            popUpTo("main_menu") {
                                saveState = true
                            }
                            // Unngå flere kopier av samme destinasjon
                            launchSingleTop = true
                            // Gjenopprett tilstand når man velger et tidligere valgt element
                            restoreState = true
                        }

                        // Oppdater antall venneforespørsler når man navigerer til venner-skjermen
                        if (item == BottomNavItem.Friends) {
                            friendsViewModel.loadFriendRequestsCount()
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

