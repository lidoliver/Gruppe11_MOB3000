// In app/src/main/java/My/Application/MainActivity.kt
package My.Application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import My.Application.ui.screen.mainmenu.MainMenuScreen
import My.Application.ui.screen.myprofile.MyProfileScreen
import My.Application.ui.screen.startround.StartRoundScreen
import My.Application.ui.spill.PlayDiscGolfScreen
import My.Application.ui.theme.MyApplicationTheme

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
    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(navController = navController)
        }
        composable("start_round") {
            StartRoundScreen(navController = navController)
        }
        composable("my_profile") {
            MyProfileScreen(navController = navController)
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
                courseId = courseId
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
                sessionId = sessionId
            )
        }
    }
}
