/**
 * Min profil-skjerm som viser brukerens profilinformasjon og statistikk
 */
package my.application.ui.screen.myprofile

import my.application.ui.components.AppTopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.data.auth.AuthManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MyProfileScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: MyProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MyProfileViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )

    val authManager = AuthManager.getInstance(context)
    val userEmail by authManager.getUserEmailFlow().collectAsState(initial = null)
    val uiState = viewModel.uiState
    val stats by viewModel.stats.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }

    // Håndter autentiseringsfeil ved å navigere til innlogging
    LaunchedEffect(uiState.authError) {
        if (uiState.authError) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    // Last inn profildata på nytt når denne skjermen navigeres til
    val currentBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(userEmail, currentBackStackEntry) {
        userEmail?.let { email ->
            viewModel.loadUserProfile(email)
            viewModel.loadPlayerStats(email)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Min Profil",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                userEmail == null -> {
                    // User is not logged in
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ikke pålogget",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Logg inn for å se profilen din",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { navController.navigate("login") }) {
                            Text("Gå til pålogging")
                        }
                    }
                }
                uiState.isLoading && uiState.user == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null && uiState.user == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            userEmail?.let { viewModel.loadUserProfile(it) }
                        }) {
                            Text("Prøv igjen")
                        }
                    }
                }
                uiState.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Profile Header Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Avatar med redigeringsknapp
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .background(Color(uiState.profileColor))
                                            .clickable { showEditProfileDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(60.dp)
                                        )
                                    }

                                    // Edit button overlay
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .align(Alignment.BottomEnd)
                                            .clickable { showEditProfileDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rediger profil",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Name - clickable to edit
                                Text(
                                    text = if (uiState.user.firstName != null || uiState.user.lastName != null) {
                                        "${uiState.user.firstName ?: ""} ${uiState.user.lastName ?: ""}".trim()
                                    } else {
                                        uiState.user.username ?: "Bruker"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.clickable { showEditProfileDialog = true }
                                )

                                // Username - clickable to edit
                                if (uiState.user.username != null) {
                                    Text(
                                        text = "@${uiState.user.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        modifier = Modifier.clickable { showEditProfileDialog = true }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Email
                                Text(
                                    text = uiState.user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )

                                // Klubbinformasjon
                                if (uiState.user.clubName != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📍 ${uiState.user.clubName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        // Hurtighandlinger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    navController.navigate("player_history")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Historikk")
                            }

                            OutlinedButton(
                                onClick = { showChangePasswordDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Passord")
                            }
                        }

                        // Performance Overview
                        stats?.let { statsData ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Mine prestasjoner",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Stats Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        StatCard(
                                            title = "Runder spilt",
                                            value = statsData.totalGames.toString(),
                                            icon = Icons.Default.Sports,
                                            modifier = Modifier.weight(1f)
                                        )

                                        StatCard(
                                            title = "Gjennomsnitt",
                                            value = statsData.averageScore?.let {
                                                String.format(Locale.US, "%.1f", it)
                                            } ?: "N/A",
                                            icon = Icons.Default.TrendingUp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        StatCard(
                                            title = "Best score",
                                            value = statsData.bestScore?.toString() ?: "N/A",
                                            icon = Icons.Default.Star,
                                            modifier = Modifier.weight(1f)
                                        )

                                        StatCard(
                                            title = "Verst score",
                                            value = statsData.worstScore?.toString() ?: "N/A",
                                            icon = Icons.Default.RemoveCircle,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Course Stats
                                    if (statsData.totalRoundsByCourse.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "Mine favorittbaner",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        statsData.totalRoundsByCourse.entries
                                            .sortedByDescending { it.value }
                                            .take(5)
                                            .forEach { (courseName, rounds) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = courseName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "$rounds ${if (rounds == 1) "runde" else "runder"}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                        }

                        // Ekstra informasjonskort
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Kontoinformasjon",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                InfoRow(
                                    label = "Venner",
                                    value = uiState.friendsCount.toString()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Admin Panel button (only visible for admin users)
                        if (uiState.user.role == "admin") {
                            Button(
                                onClick = { navController.navigate("admin_panel") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Admin Panel")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Club Admin Panel button (only visible for klubbadmin users)
                        if (uiState.user.role == "klubbadmin") {
                            Button(
                                onClick = { navController.navigate("club_admin_panel") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Klubbadmin Panel")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Logout Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    authManager.clearAuth()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logg ut")
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Dialog for endring av passord
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    onSuccess = {
                        showChangePasswordDialog = false
                    },
                    onError = { /* Feil håndteres i dialog */ }
                )
            }
        )
    }

    // Dialog for redigering av profil (sammenslått: profilbilde, brukernavn, fornavn, etternavn)
    if (showEditProfileDialog) {
        EditProfileDialog(
            user = uiState.user,
            currentColor = uiState.profileColor,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedUser, newColor ->
                userEmail?.let { email ->
                    // Oppdater profilfarge
                    if (newColor != uiState.profileColor) {
                        viewModel.updateProfileColor(newColor)
                    }
                    // Oppdater brukerprofil
                    viewModel.updateProfile(email, updatedUser)
                    showEditProfileDialog = false
                }
            },
            viewModel = viewModel
        )
    }

    // Dialog for redigering av brukernavn
    if (showEditUsernameDialog) {
        EditUsernameDialog(
            currentUsername = uiState.user?.username,
            onDismiss = { showEditUsernameDialog = false },
            onSave = { newUsername ->
                userEmail?.let { email ->
                    viewModel.updateUsername(
                        email = email,
                        newUsername = newUsername,
                        onSuccess = {
                            showEditUsernameDialog = false
                        },
                        onError = { error ->
                            // Error is handled in the ViewModel
                            showEditUsernameDialog = false
                        }
                    )
                }
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Endre passord") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Nåværende passord") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Nytt passord") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    label = { Text("Bekreft nytt passord") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> {
                            errorMessage = "Vennligst skriv inn nåværende passord"
                        }
                        newPassword.isEmpty() -> {
                            errorMessage = "Vennligst skriv inn nytt passord"
                        }
                        newPassword.length < 6 -> {
                            errorMessage = "Passordet må være minst 6 tegn"
                        }
                        newPassword != confirmPassword -> {
                            errorMessage = "Passordene stemmer ikke overens"
                        }
                        else -> {
                            onConfirm(currentPassword, newPassword)
                        }
                    }
                }
            ) {
                Text("Endre passord")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    user: my.application.data.remote.dto.UserDto?,
    currentColor: Long,
    onDismiss: () -> Unit,
    onSave: (my.application.data.remote.dto.UserDto, Long) -> Unit,
    viewModel: MyProfileViewModel
) {
    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var selectedColor by remember { mutableStateOf(currentColor) }
    var errorMessage by remember { mutableStateOf("") }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val availableColors = listOf(
        0xFF1976D2 to "Blå",
        0xFF388E3C to "Grønn",
        0xFFD32F2F to "Rød",
        0xFF7B1FA2 to "Lilla",
        0xFFF57C00 to "Oransje",
        0xFF0097A7 to "Cyan",
        0xFFC2185B to "Rosa",
        0xFF5D4037 to "Brun",
        0xFF455A64 to "Blågrå",
        0xFFE91E63 to "Pink",
        0xFF9C27B0 to "Fiolett",
        0xFF673AB7 to "Dyp lilla"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rediger profil") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                // Tab selector
                TabRow(selectedTabIndex = currentTab) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        text = { Text("Profilinfo") }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        text = { Text("Profilbilde") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab content
                when (currentTab) {
                    0 -> {
                        // Profile Info Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = {
                                    firstName = it
                                    errorMessage = ""
                                },
                                label = { Text("Fornavn") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = lastName,
                                onValueChange = {
                                    lastName = it
                                    errorMessage = ""
                                },
                                label = { Text("Etternavn") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    errorMessage = ""
                                },
                                label = { Text("Brukernavn") },
                                prefix = { Text("@") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = errorMessage.isNotEmpty()
                            )

                            if (errorMessage.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (isCheckingUsername) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sjekker tilgjengelighet...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tips: Du kan la fornavn og etternavn være tomt hvis du kun vil vise brukernavnet ditt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    1 -> {
                        // Profile Picture Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Preview
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(selectedColor))
                                    .align(Alignment.CenterHorizontally),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Velg en farge for profilbildet ditt",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Color grid
                            val rows = availableColors.chunked(3)
                            rows.forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowColors.forEach { (colorValue, colorName) ->
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorValue))
                                                    .clickable { selectedColor = colorValue },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (colorValue == selectedColor) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Valgt",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = colorName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    repeat(3 - rowColors.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        username.isEmpty() -> {
                            errorMessage = "Brukernavnet kan ikke være tomt"
                        }
                        username.length < 3 -> {
                            errorMessage = "Brukernavnet må være minst 3 tegn"
                        }
                        username.contains(" ") -> {
                            errorMessage = "Brukernavnet kan ikke inneholde mellomrom"
                        }
                        !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                            errorMessage = "Brukernavnet kan bare inneholde bokstaver, tall og understreker"
                        }
                        username != user?.username -> {
                            // Sjekk om brukernavnet er tilgjengelig
                            isCheckingUsername = true
                            coroutineScope.launch {
                                val result = viewModel.checkUsernameAvailability(username)
                                isCheckingUsername = false
                                if (result.isSuccess && result.getOrNull() == true) {
                                    val updatedUser = user!!.copy(
                                        firstName = firstName.takeIf { it.isNotBlank() },
                                        lastName = lastName.takeIf { it.isNotBlank() },
                                        username = username
                                    )
                                    onSave(updatedUser, selectedColor)
                                } else {
                                    errorMessage = "Dette brukernavnet er allerede tatt"
                                }
                            }
                        }
                        else -> {
                            // Username hasn't changed, just update other fields
                            val updatedUser = user!!.copy(
                                firstName = firstName.takeIf { it.isNotBlank() },
                                lastName = lastName.takeIf { it.isNotBlank() },
                                username = username
                            )
                            onSave(updatedUser, selectedColor)
                        }
                    }
                },
                enabled = !isCheckingUsername
            ) {
                Text("Lagre")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCheckingUsername
            ) {
                Text("Avbryt")
            }
        }
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfilePictureDialog(
    currentColor: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val availableColors = listOf(
        0xFF1976D2 to "Blå",
        0xFF388E3C to "Grønn",
        0xFFD32F2F to "Rød",
        0xFF7B1FA2 to "Lilla",
        0xFFF57C00 to "Oransje",
        0xFF0097A7 to "Cyan",
        0xFFC2185B to "Rosa",
        0xFF5D4037 to "Brun",
        0xFF455A64 to "Blågrå",
        0xFFE91E63 to "Pink",
        0xFF9C27B0 to "Fiolett",
        0xFF673AB7 to "Dyp lilla"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Velg profilfarve",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Velg en farge for profilbildet ditt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Color grid - 3 columns
                val rows = availableColors.chunked(3)
                rows.forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowColors.forEach { (colorValue, colorName) ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorValue))
                                        .clickable { onColorSelected(colorValue) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Vis avkryssingsmerke hvis dette er gjeldende farge
                                    if (colorValue == currentColor) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Valgt",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = colorName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Fyll ut resterende plasser hvis raden har mindre enn 3 elementer
                        repeat(3 - rowColors.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Lukk")
            }
        }
    )
}

@Composable
fun EditUsernameDialog(
    currentUsername: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    viewModel: MyProfileViewModel
) {
    var username by remember { mutableStateOf(currentUsername ?: "") }
    var errorMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Valider brukernavn i sanntid
    LaunchedEffect(username) {
        if (username.isEmpty() || username == currentUsername) {
            isAvailable = null
            errorMessage = ""
            return@LaunchedEffect
        }

        // Valider format
        if (username.contains("@")) {
            errorMessage = "Brukernavn kan ikke inneholde '@'"
            isAvailable = false
            return@LaunchedEffect
        }

        if (username.length < 3) {
            errorMessage = "Brukernavn må være minst 3 tegn"
            isAvailable = false
            return@LaunchedEffect
        }

        if (username.length > 30) {
            errorMessage = "Brukernavn kan være maks 30 tegn"
            isAvailable = false
            return@LaunchedEffect
        }

        if (!username.matches(Regex("^[a-zA-Z0-9_]*$"))) {
            errorMessage = "Brukernavn kan kun inneholde bokstaver, tall og _"
            isAvailable = false
            return@LaunchedEffect
        }

        // Sjekk tilgjengelighet
        isChecking = true
        errorMessage = ""
        kotlinx.coroutines.delay(500) // Forsinkelse for å unngå for mange forespørsler

        coroutineScope.launch {
            val result = viewModel.checkUsernameAvailability(username)
            isChecking = false
            result.fold(
                onSuccess = { available ->
                    isAvailable = available
                    if (!available) {
                        errorMessage = "Brukernavn er allerede tatt"
                    }
                },
                onFailure = {
                    errorMessage = "Kunne ikke sjekke tilgjengelighet"
                    isAvailable = false
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rediger brukernavn") },
        text = {
            Column {
                Text(
                    text = "Brukernavn kan kun inneholde bokstaver, tall og _, og kan ikke inneholde @-tegn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        isAvailable = null
                    },
                    label = { Text("Brukernavn") },
                    placeholder = { Text("dittbrukernavn") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage.isNotEmpty() || isAvailable == false,
                    supportingText = {
                        when {
                            errorMessage.isNotEmpty() -> {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            isChecking -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sjekker tilgjengelighet...")
                                }
                            }
                            isAvailable == true -> {
                                Text(
                                    text = "✓ Brukernavn er tilgjengelig",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isNotEmpty() && isAvailable == true) {
                        onSave(username)
                    }
                },
                enabled = username.isNotEmpty() && isAvailable == true && !isChecking
            ) {
                Text("Lagre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}
