package my.application.ui.spill

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import my.application.ui.components.AppTopBar
import kotlinx.coroutines.delay

// Data class for course information - ready for database integration
data class Course(
    val id: String,
    val name: String,
    val holeCount: Int,
    val description: String? = null
)

@Composable
fun BanevelgingAleneScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load courses - currently demo data, easily swappable for API/database call
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // TODO: Replace this with actual database/API call
            // Example: courses = courseRepository.getAllCourses()
            courses = loadDemoCourses()
            isLoading = false
        } catch (_: Exception) {
            error = "Kunne ikke laste baner"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Velg Bane",
                canNavigateBack = true,
                navController = navController
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Ukjent feil",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Velg en bane",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    courses.forEach { course ->
                        CourseCard(
                            course = course,
                            onClick = {
                                navController.navigate("play_course/${course.id}")
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// Simulates API/database call - replace this entire function when ready
private suspend fun loadDemoCourses(): List<Course> {
    delay(500) // Simulate network delay

    // TODO: Replace with actual database call
    // Example: return database.courseDao().getAllCourses()
    // Or: return api.getCourses()

    return listOf(
        Course(
            id = "course-6-holes",
            name = "6-Hull Bane",
            holeCount = 6,
            description = "Kort bane - perfekt for hurtigspill"
        ),
        Course(
            id = "course-9-holes",
            name = "9-Hull Bane",
            holeCount = 9,
            description = "Standard 9-hulls bane"
        ),
        Course(
            id = "course-12-holes",
            name = "12-Hull Bane",
            holeCount = 12,
            description = "Mellomlang bane"
        ),
        Course(
            id = "course-15-holes",
            name = "15-Hull Bane",
            holeCount = 15,
            description = "Lang bane for erfarne spillere"
        ),
        Course(
            id = "course-18-holes",
            name = "18-Hull Bane",
            holeCount = 18,
            description = "Standard full bane"
        )
    )
}

@Composable
fun CourseCard(
    course: Course,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    text = course.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.description ?: "${course.holeCount} hull",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${course.holeCount} hull",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


