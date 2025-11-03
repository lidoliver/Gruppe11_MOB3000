package My.application.ui.screen.myprofile

import My.application.ui.components.AppTopBar
import My.application.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun MyProfileScreen(navController: NavController) {
    Scaffold(
        topBar = { AppTopBar(title = "Min Profil", canNavigateBack = true, navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Placeholder for player information
            Text("Player Name: John Doe", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Email: john.doe@example.com", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rounds Played: 15", fontSize = 16.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // Button to change password
            Button(
                onClick = { /* TODO: Navigate to change password screen */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MinProfilSkjermPreview() {
    MyApplicationTheme {
        MyProfileScreen(navController = rememberNavController())
    }
}
