// In app/src/main/java/My/Application/ui/screen/mainmenu/MainMenuScreen.kt
package My.Application.ui.screen.mainmenu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import My.Application.ui.components.AppTopBar
import My.Application.ui.theme.MyApplicationTheme

@Composable
fun MainMenuScreen(navController: NavController) {
    Scaffold(
        topBar = { AppTopBar(title = "Main Menu", canNavigateBack = false, navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    navController.navigate("start_round")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Ny Runde")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO: Handle 'View History' click */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spillehistorikk")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("my_profile") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("My Profile")
            }
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
