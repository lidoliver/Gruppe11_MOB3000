// In app/src/main/java/My/Application/MainActivity.kt
package my.application

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import my.application.ui.utils.isLandscape
import my.application.ui.utils.rememberLandscapeUiState
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
import my.application.ui.screen.club.ClubScreen
import my.application.ui.screen.club.CreateClubScreen
import my.application.ui.screen.club.ClubListScreen
import my.application.ui.screen.club.ClubAdminPanelScreen
import my.application.ui.spill.BanevelgingAleneScreen
import my.application.ui.spill.PlayDiscGolfScreen
import my.application.ui.spill.CourseMapScreen
import my.application.ui.theme.MyApplicationTheme
import my.application.ui.screen.playerhistory.PlayerHistoryScreen
import my.application.ui.screen.playerhistory.PlayerHistoryViewModel
import my.application.data.remote.RetrofitProvider
import my.application.data.auth.AuthManager
import my.application.data.auth.HeartbeatManager
import my.application.service.FcmTokenManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import my.application.ui.spill.MultiplayerLobbyScreen
import my.application.ui.spill.MultiplayerCourseSelectionScreen as MultiplayerCourseSelection



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge() // Eksplisitt kall

        // Be om varslingstillatelse for FCM
        FcmTokenManager.requestNotificationPermission(this)

        setContent {
            MyApplicationTheme {
                // Initialiser autentisering ved oppstart
                InitializeAuth()
                // Send intent til AppNavigation for varselhåndtering
                AppNavigation(intent = intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InitializeAuth() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val authManager = AuthManager.getInstance(context)
        authManager.initializeAuth()

        // Initialiser HeartbeatManager - den vil automatisk håndtere heartbeats basert på app-livssyklus
        val application = context.applicationContext as android.app.Application
        HeartbeatManager.getInstance(application)
    }
}

@Composable
fun AppNavigation(intent: Intent? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Detekter landskapsorientering
    val isLandscapeMode = isLandscape()
    val landscapeUiState = rememberLandscapeUiState()

    // Håndter varslingsintent og naviger til riktig skjerm
    LaunchedEffect(intent) {
        intent?.let {
            val notificationType = it.getStringExtra("notification_type")
            val senderEmail = it.getStringExtra("sender_email")
            val senderName = it.getStringExtra("sender_name")

            when (notificationType) {
                "message" -> {
                    // Naviger til samtale med avsender
                    if (!senderEmail.isNullOrBlank()) {
                        navController.navigate("conversation/$senderEmail/${senderName ?: "User"}")
                    }
                }
                "friend_request" -> {
                    // Naviger til venneforespørsler
                    navController.navigate("friend_requests")
                }
                "club_invite" -> {
                    // Naviger til klubb-skjermen
                    navController.navigate("club")
                }
            }
        }
    }

    // Tilstand for å kontrollere synlighet av bunnlinje
    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Sjekk om vi er på spillskjerm eller innloggingsskjerm
    val isPlayScreen = currentRoute?.startsWith("play_") == true
    val isLoginScreen = currentRoute == "login"

    // Skjul automatisk bunnlinje ved inngang til spillskjerm eller innloggingsskjerm
    // I landskapsmodus, skjul linjer som standard med mindre bruker veksler dem
    LaunchedEffect(currentRoute, isLandscapeMode) {
        isBottomBarVisible = !isPlayScreen && !isLoginScreen

        // I landskapsmodus, start med linjer skjult
        if (isLandscapeMode) {
            landscapeUiState.hide()
        }
    }

    // Beregn faktisk synlighet basert på orientering og tilstand
    val actualBottomBarVisible = if (isLandscapeMode) {
        isBottomBarVisible && landscapeUiState.isBottomBarVisible
    } else {
        isBottomBarVisible
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = actualBottomBarVisible,
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
        },
        floatingActionButton = {
            // Vis FAB kun i landskapsmodus når ikke på spill/innloggingsskjermer
            if (isLandscapeMode && !isPlayScreen && !isLoginScreen) {
                AnimatedVisibility(
                    visible = !actualBottomBarVisible,
                    enter = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                    exit = scaleOut(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                ) {
                    FloatingActionButton(
                        onClick = { landscapeUiState.toggleVisibility() },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Toggle Navigation",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start
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
            composable("club") {
                ClubScreen(navController = navController)
            }
            composable("club_list") {
                ClubListScreen(navController = navController)
            }
            composable("create_club") {
                CreateClubScreen(navController = navController)
            }
            composable("club_admin_panel") {
                ClubAdminPanelScreen(navController = navController)
            }
            composable("friend_search") {
                FriendSearchScreen(navController = navController)
            }
            composable("friend_requests") {
                FriendRequestsScreen(navController = navController)
            }
            composable("course_selection_multiplayer") {
                MultiplayerCourseSelection(navController = navController)
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
            composable("player_history") {
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
                    // Vis lasting eller plassholder mens bruker-epost hentes
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            composable("multiplayer_lobby/{sessionId}") { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                MultiplayerLobbyScreen(
                    navController = navController,
                    sessionId = sessionId
                )
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
                route = "courseDetail/{email}/{courseName}",
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("courseName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val courseName = backStackEntry.arguments?.getString("courseName") ?: ""

                val viewModel = remember {
                    my.application.ui.screen.coursedetail.CourseDetailViewModel(RetrofitProvider.playerApi)
                }

                my.application.ui.screen.coursedetail.CourseDetailScreen(
                    navController = navController,
                    viewModel = viewModel,
                    playerEmail = email,
                    courseName = courseName,
                    playerName = null
                )
            }
            composable(
                route = "courseDetail/{email}/{courseName}/{playerName}",
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("courseName") { type = NavType.StringType },
                    navArgument("playerName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
                val playerName = backStackEntry.arguments?.getString("playerName") ?: ""

                val viewModel = remember {
                    my.application.ui.screen.coursedetail.CourseDetailViewModel(RetrofitProvider.playerApi)
                }

                my.application.ui.screen.coursedetail.CourseDetailScreen(
                    navController = navController,
                    viewModel = viewModel,
                    playerEmail = email,
                    courseName = courseName,
                    playerName = playerName
                )
            }
            composable(
                route = "gameDetail/{gameId}/{email}/{courseName}",
                arguments = listOf(
                    navArgument("gameId") { type = NavType.IntType },
                    navArgument("email") { type = NavType.StringType },
                    navArgument("courseName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getInt("gameId") ?: 0
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val courseName = backStackEntry.arguments?.getString("courseName") ?: ""

                // Load the course history to get the specific game
                val viewModel = remember {
                    my.application.ui.screen.coursedetail.CourseDetailViewModel(RetrofitProvider.playerApi)
                }

                LaunchedEffect(email, courseName) {
                    viewModel.loadCourseHistory(email, courseName)
                }

                val uiState by viewModel.uiState.collectAsState()

                when (val state = uiState) {
                    is my.application.ui.screen.coursedetail.CourseDetailUiState.Success -> {
                        val gameDetail = state.courseHistory.games.find { it.gameId == gameId }
                        val holeStatistics = state.courseHistory.holeStatistics

                        my.application.ui.screen.gamedetail.GameDetailScreen(
                            navController = navController,
                            gameDetail = gameDetail,
                            holeStatistics = holeStatistics,
                            courseName = courseName
                        )
                    }
                    is my.application.ui.screen.coursedetail.CourseDetailUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is my.application.ui.screen.coursedetail.CourseDetailUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
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
