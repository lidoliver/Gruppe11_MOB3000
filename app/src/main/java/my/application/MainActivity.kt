// In app/src/main/java/My/Application/MainActivity.kt
package my.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import my.application.ui.components.BottomNavigationBar
import my.application.ui.screen.mainmenu.MainMenuScreen
import my.application.ui.screen.messages.ConversationScreen
import my.application.ui.screen.friends.FriendsScreen
import my.application.ui.screen.myprofile.MyProfileScreen
import my.application.ui.screen.startround.StartRoundScreen
import my.application.ui.screen.tips.TipsScreen
import my.application.ui.spill.BanevelgingAleneScreen
import my.application.ui.spill.PlayDiscGolfScreen
import my.application.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // State to control bottom bar visibility
    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Check if we're on a play screen
    val isPlayScreen = currentRoute?.startsWith("play_") == true

    // Automatically hide bottom bar when entering play screen
    LaunchedEffect(currentRoute) {
        isBottomBarVisible = !isPlayScreen
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "main_menu",
            modifier = Modifier.padding(
                bottom = if (isBottomBarVisible) paddingValues.calculateBottomPadding() else 0.dp
            )
        ) {
            composable("main_menu") {
                MainMenuScreen(navController = navController)
            }
            composable("start_round") {
                StartRoundScreen(navController = navController)
            }
            composable("course_selection_alone") {
                BanevelgingAleneScreen(navController = navController)
            }
            composable("my_profile") {
                MyProfileScreen(navController = navController)
            }
            composable("friends") {
                FriendsScreen(navController = navController)
            }
            composable(
                route = "conversation/{contactId}/{contactName}",
                arguments = listOf(
                    navArgument("contactId") { type = NavType.IntType },
                    navArgument("contactName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getInt("contactId") ?: 0
                val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
                ConversationScreen(
                    navController = navController,
                    contactName = contactName,
                    contactId = contactId
                )
            }
            composable("tips") {
                TipsScreen(navController = navController)
            }

            // Route for playing a course (solo mode)
            composable(
                route = "play_course/{courseId}",
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")
                PlayDiscGolfScreen(
                    navController = navController,
                    courseId = courseId,
                    isBottomBarVisible = isBottomBarVisible,
                    onBottomBarVisibilityChange = { visible ->
                        isBottomBarVisible = visible
                    }
                )
            }

            // Route for playing with a session (multiplayer mode)
            composable(
                route = "play_session/{courseId}/{sessionId}",
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType },
                    navArgument("sessionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                PlayDiscGolfScreen(
                    navController = navController,
                    courseId = courseId,
                    sessionId = sessionId,
                    isBottomBarVisible = isBottomBarVisible,
                    onBottomBarVisibilityChange = { visible ->
                        isBottomBarVisible = visible
                    }
                )
            }
        }
    }
}
