// In app/src/main/java/My/Application/MainActivity.kt
package my.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import my.application.ui.screen.messages.MessagesScreen
import my.application.ui.screen.friends.FriendsScreen
import my.application.ui.screen.friends.FriendSearchScreen
import my.application.ui.screen.friends.FriendRequestsScreen
import my.application.ui.screen.myprofile.MyProfileScreen
import my.application.ui.screen.startround.StartRoundScreen
import my.application.ui.screen.tips.TipsScreen
import my.application.ui.screen.login.LoginScreen
import my.application.ui.spill.BanevelgingAleneScreen
import my.application.ui.spill.PlayDiscGolfScreen
import my.application.ui.spill.CourseMapScreen
import my.application.ui.theme.MyApplicationTheme
import my.application.ui.screen.playerhistory.PlayerHistoryScreen
import my.application.ui.screen.playerhistory.PlayerHistoryViewModel
import my.application.data.remote.RetrofitProvider
import my.application.data.auth.AuthManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge() // make call explicit
        setContent {
            MyApplicationTheme {
                // Initialize auth on app start
                InitializeAuth()
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InitializeAuth() {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        val authManager = my.application.data.auth.AuthManager.getInstance(context)
        authManager.initializeAuth()
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // State to control bottom bar visibility
    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Check if we're on a play screen or login screen
    val isPlayScreen = currentRoute?.startsWith("play_") == true
    val isLoginScreen = currentRoute == "login"

    // Automatically hide bottom bar when entering play screen or login screen
    LaunchedEffect(currentRoute) {
        isBottomBarVisible = !isPlayScreen && !isLoginScreen
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
            composable("login") {
                LoginScreen(navController = navController)
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
            composable("messages") {
                MessagesScreen(navController = navController)
            }
            composable("friend_search") {
                FriendSearchScreen(navController = navController)
            }
            composable("friend_requests") {
                FriendRequestsScreen(navController = navController)
            }
            composable(
                route = "friend_profile/{friendEmail}/{friendName}",
                arguments = listOf(
                    navArgument("friendEmail") { type = NavType.StringType },
                    navArgument("friendName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val friendEmail = backStackEntry.arguments?.getString("friendEmail") ?: ""
                val friendName = backStackEntry.arguments?.getString("friendName") ?: ""

                my.application.ui.screen.friends.FriendProfileScreen(
                    navController = navController,
                    friendEmail = friendEmail,
                    friendName = friendName
                )
            }
            composable("player_history") { backStackEntry ->
                val context = LocalContext.current
                val authManager = AuthManager.getInstance(context)
                val scope = rememberCoroutineScope()
                var userEmail by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    scope.launch {
                        userEmail = authManager.getUserEmail()
                    }
                }

                val viewModel = remember {
                    PlayerHistoryViewModel(RetrofitProvider.playerApi)
                }

                userEmail?.let { email ->
                    PlayerHistoryScreen(
                        navController = navController,
                        viewModel = viewModel,
                        playerEmail = email
                    )
                } ?: run {
                    // Show loading or placeholder while fetching user email
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            composable(
                route = "friend_history/{friendEmail}/{friendName}",
                arguments = listOf(
                    navArgument("friendEmail") { type = NavType.StringType },
                    navArgument("friendName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val friendEmail = backStackEntry.arguments?.getString("friendEmail") ?: ""
                val friendName = backStackEntry.arguments?.getString("friendName") ?: ""

                val viewModel = remember {
                    PlayerHistoryViewModel(RetrofitProvider.playerApi)
                }

                PlayerHistoryScreen(
                    navController = navController,
                    viewModel = viewModel,
                    playerEmail = friendEmail,
                    playerName = friendName
                )
            }
            composable(
                route = "conversation/{contactEmail}/{contactName}",
                arguments = listOf(
                    navArgument("contactEmail") { type = NavType.StringType },
                    navArgument("contactName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val contactEmail = backStackEntry.arguments?.getString("contactEmail") ?: ""
                val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
                ConversationScreen(
                    navController = navController,
                    contactName = contactName,
                    contactEmail = contactEmail
                )
            }
            composable("tips") {
                TipsScreen(navController = navController)
            }

            // Admin panel routes
            composable("admin_panel") {
                my.application.ui.screen.admin.AdminPanelScreen(navController = navController)
            }
            composable("admin_users") {
                my.application.ui.screen.admin.UserAdminScreen(navController = navController)
            }
            composable("admin_club_requests") {
                my.application.ui.screen.admin.ClubRequestAdminScreen(navController = navController)
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

            // Route for course map view
            composable(
                route = "course_map/{courseId}",
                arguments = listOf(
                    navArgument("courseId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("courseId")
                CourseMapScreen(
                    navController = navController,
                    courseId = courseId
                )
            }
        }
    }
}
