// In app/src/main/java/My/Application/MainActivity.kt
package my.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import my.application.ui.screen.mainmenu.MainMenuScreen
import my.application.ui.screen.myprofile.MyProfileScreen
import my.application.ui.screen.startround.StartRoundScreen
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
    }
}
