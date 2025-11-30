package my.application.ui.screen.club

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar

/**
 * Skjerm for å opprette en ny klubb
 * Viser skjema for klubbnavn og søknadsbrev
 * Sender forespørsel til admin for godkjenning
 */
@Composable
fun CreateClubScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: CreateClubViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return CreateClubViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    // Vis suksessdialog og naviger tilbake når søknaden er sendt
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            // Vent litt for å vise suksessmelding, deretter naviger tilbake
            kotlinx.coroutines.delay(2000)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Opprett Klubb",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        if (uiState.isSubmitted) {
            // Success screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Søknad Sendt!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Din klubbsøknad er sendt til administrator for godkjenning.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // Form screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Søk om å opprette klubb",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Fyll ut skjemaet nedenfor for å søke om å opprette en ny disc golf klubb. Søknaden vil bli vurdert av en administrator.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Club Name Field
                OutlinedTextField(
                    value = uiState.clubName,
                    onValueChange = viewModel::onClubNameChange,
                    label = { Text("Klubbnavn") },
                    placeholder = { Text("F.eks. Oslo Disc Golf Klubb") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.clubNameError != null,
                    supportingText = {
                        if (uiState.clubNameError != null) {
                            Text(
                                text = uiState.clubNameError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

                // Description/Application Letter Field
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Søknadsbrev") },
                    placeholder = {
                        Text("Beskriv hvorfor du ønsker å opprette denne klubben, hva er målet, hvem er målgruppen, etc.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    isError = uiState.descriptionError != null,
                    supportingText = {
                        if (uiState.descriptionError != null) {
                            Text(
                                text = uiState.descriptionError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "${uiState.description.length} / min. 50 tegn",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    minLines = 8,
                    maxLines = 15,
                    enabled = !uiState.isLoading
                )

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Viktig informasjon",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "• Søknaden vil bli vurdert av en administrator\n" +
                                    "• Behandlingstiden kan variere\n" +
                                    "• Du vil bli varslet når søknaden er behandlet\n" +
                                    "• Skriv et grundig søknadsbrev for best sjanse",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Error Message
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = viewModel::submitClubRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sender...")
                    } else {
                        Text(
                            text = "Send Søknad",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

