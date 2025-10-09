// In app/src/main/java/My/Application/ui/screen/startround/StartRoundScreen.kt

package My.Application.ui.screen.startround

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import My.Application.ui.components.AppTopBar
import My.Application.ui.theme.MyApplicationTheme

@Composable
fun StartRoundScreen() {
    Scaffold(
        topBar = { AppTopBar() } // Using our reusable TopBar
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
                onClick = { /* TODO: Handle 'Play Alone' click */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spill alene")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO: Handle 'Invite Players' click */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inviter spillere")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartRoundScreenPreview() {
    MyApplicationTheme {
        StartRoundScreen()
    }
}
