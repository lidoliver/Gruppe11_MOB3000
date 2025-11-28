package my.application.ui.spill

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import my.application.ui.components.AppTopBar
import my.application.ui.viewmodel.CoursesViewModel
import my.application.data.repository.MultiplayerRepository

private const val TAG = "MultiplayerCourseScreen"

@Composable
fun MultiplayerCourseSelectionScreen(navController: NavController) {
    Log.d(TAG, "=== MultiplayerCourseSelectionScreen composable called ===")

    val context = LocalContext.current
    val viewModel: CoursesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                Log.d(TAG, "Creating new CoursesViewModel instance")
                CoursesViewModel(context.applicationContext as android.app.Application)
            }
        }
    )

    Log.d(TAG, "ViewModel instance created: ${viewModel.hashCode()}")

    val repository = remember { MultiplayerRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val trackDtos by viewModel.courses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Log.d(TAG, "Initial state - isLoading: $isLoading, courses: ${trackDtos.size}, error: $error")

    // Debug logging
    LaunchedEffect(isLoading, error, trackDtos.size) {
        Log.d(TAG, "UI State changed - isLoading: $isLoading, error: $error, courses: ${trackDtos.size}")
    }

    var creating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect triggered - loading courses")
        viewModel.loadCourses()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Velg Bane - Multiplayer",
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
                isLoading && trackDtos.isEmpty() -> {
                    Log.d(TAG, "Rendering: Loading state")
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Log.d(TAG, "Rendering: Error state - $error")
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Feil ved lasting av baner",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Ukjent feil",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadCourses() }) {
                            Text("Prøv igjen")
                        }
                    }
                }
                trackDtos.isEmpty() && !isLoading -> {
                    Log.d(TAG, "Rendering: Empty state")
                    Text(
                        text = "Ingen baner tilgjengelig",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Log.d(TAG, "Rendering: Courses list with ${trackDtos.size} courses")
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Velg en bane for multiplayer",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        createError?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        trackDtos.forEach { track ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !creating) {
                                        Log.d(TAG, "Course clicked: ${track.name} (ID: ${track.baneId})")
                                        creating = true
                                        createError = null
                                        coroutineScope.launch {
                                            Log.d(TAG, "Starting session creation...")
                                            val result = repository.createSession(
                                                courseId = track.baneId.toString(),
                                                courseName = track.name,
                                                totalHoles = track.holes
                                            )
                                            result.onSuccess { sessionId ->
                                                Log.d(TAG, "Session created successfully, navigating to lobby: $sessionId")
                                                navController.navigate("multiplayer_lobby/$sessionId")
                                                creating = false
                                            }.onFailure { exception ->
                                                Log.e(TAG, "Failed to create session: ${exception.message}", exception)
                                                createError = exception.message
                                                creating = false
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${track.holes} hull",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (creating) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
