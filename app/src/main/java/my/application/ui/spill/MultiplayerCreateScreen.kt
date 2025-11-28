package my.application.ui.spill

import androidx.compose.foundation.layout.*
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
import my.application.data.local.MultiplayerSessionManager
import my.application.ui.components.AppTopBar
import my.application.ui.viewmodel.CoursesViewModel

/**
 * Screen to create a new multiplayer session using the existing Firebase-based MultiplayerRepository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerCreateScreen(navController: NavController) {
    val context = LocalContext.current
    val coursesViewModel: CoursesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CoursesViewModel(context.applicationContext as android.app.Application) }
        }
    )

    val scope = rememberCoroutineScope()

    val courses by coursesViewModel.courses.collectAsState()
    val isLoadingCourses by coursesViewModel.isLoading.collectAsState()
    val loadError by coursesViewModel.error.collectAsState()

    var selectedCourseIndex by remember { mutableStateOf(-1) }
    val selectedCourse = courses.getOrNull(selectedCourseIndex)

    var isCreating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        coursesViewModel.loadCourses()
    }

    LaunchedEffect(error, loadError) {
        error?.let { snackbarHostState.showSnackbar(it) }
        loadError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Opprett session",
                canNavigateBack = true,
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                text = "Velg bane for multiplayer",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            if (isLoadingCourses) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (courses.isEmpty()) {
                Text(
                    text = "Fant ingen baner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCourse?.name ?: "Velg bane",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bane") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        courses.forEachIndexed { index, course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = {
                                    selectedCourseIndex = index
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val course = selectedCourse
                    if (course == null) {
                        error = "Du må velge en bane først"
                        return@Button
                    }

                    isCreating = true
                    error = null

                    scope.launch {
                        try {
                            val session = MultiplayerSessionManager.createSession(
                                courseId = course.baneId.toString(),
                                courseName = course.name,
                                totalHoles = course.holes
                            )

                            navController.navigate("multiplayer_lobby/${session.sessionId}") {
                                popUpTo("multiplayer_home") { inclusive = false }
                            }
                        } catch (t: Throwable) {
                            error = t.message ?: "Kunne ikke opprette session"
                        } finally {
                            isCreating = false
                        }
                    }
                },
                enabled = !isCreating && courses.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (isCreating) "Oppretter..." else "Opprett session")
            }
        }
    }
}
