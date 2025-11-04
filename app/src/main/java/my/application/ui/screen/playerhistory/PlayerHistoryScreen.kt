package my.application.ui.screen.playerhistory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import my.application.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*




@Composable
fun PlayerHistoryScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar (
                title = "Spillerhistorik",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text ="Din Spillerhistorikk",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            //Eksempel data, statisk
            ElevatedCard(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Stastistikk",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Beste Spiller i Oktober",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
@Preview
@Composable
fun PlayerHistoryScreenPreview() {
    MyApplicationTheme {
        PlayerHistoryScreen(navController = rememberNavController())
    }
}

