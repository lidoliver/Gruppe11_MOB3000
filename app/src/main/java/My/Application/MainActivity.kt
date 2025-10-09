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
import My.Application.ui.screen.mainmenu.MainMenuScreen // <-- Add this import
import My.Application.ui.screen.startround.StartRoundScreen
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
            // This now correctly calls the function from the other file
            MainMenuScreen(navController = navController)
        }
        composable("start_round") {
            // Your StartRoundScreen file is already organized correctly
            StartRoundScreen()
        }
    }
}
