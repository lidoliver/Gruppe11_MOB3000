// In app/src/main/java/My/Application/ui/components/TopBar.kt

package My.Application.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "=Disc",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        actions = {
            // Profile Icon
            IconButton(onClick = { /* TODO: Navigate to Profile Screen */ }) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User Profile",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Dropdown Menu Icon
            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // DropdownMenu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Profile") },
                    onClick = {
                        /* TODO: Handle Profile click */
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Statistics") },
                    onClick = {
                        /* TODO: Handle Statistics click */
                        showMenu = false
                    }
                )
            }
        }
    )
}
