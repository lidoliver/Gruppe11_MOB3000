// In app/src/main/java/My/Application/ui/components/TopBar.kt

package My.Application.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import My.Application.ui.theme.MyApplicationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    canNavigateBack: Boolean,
    navController: NavController
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        // Show back arrow if canNavigateBack is true
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("My Profile") },
                    onClick = {
                        navController.navigate("my_profile")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Statistics") },
                    onClick = {
                        // TODO: Navigate to statistics screen
                        showMenu = false
                    }
                )
            }
        }
    )
}

@Preview
@Composable
fun AppTopBarPreview() {
    MyApplicationTheme {
        AppTopBar(title = "Preview", canNavigateBack = true, navController = rememberNavController())
    }
}
