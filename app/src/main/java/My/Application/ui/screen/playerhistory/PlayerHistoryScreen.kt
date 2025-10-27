package My.Application.ui.screen.playerhistory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import My.Application.ui.components.AppTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import My.Application.ui.components.PlayerHistoryCard



@Composable
fun PlayerHistoryScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar (
                title = "Spillerhistorik",
                canNavigate = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text ="Din Spillerhistorikk",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            //Eksempel data, statisk
            PlayerHistoryCard(
                icon = Icons.Filled.History,
                title = "Siste spilte runde",
                description = "Du spilte runde 23 med 4 venner"
            )

            Spacer(modifier = Modifier.height(24.dp))

            //Eksempel data, statisk
            PlayerHistoryCard(
                icon = Icons.Filled.Star,
                title = "Statistikk",
                description = "Du har 10 poeng"
            )

            Spacer(modifier = Modifier.height(24.dp))

            //Eksempel data, statisk
            PlayerHistoryCard(
                icon = Icons.Filled.Star,
                title = "Statistikk",
                description = "Beste spiller i Oktober"
            )
        }

    }
}

