package my.application.ui.screen.playerhistory

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar

@Composable
fun PlayerStatisticsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Player Statistics",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Player Statistics Screen",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Statistics functionality will be implemented here",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

