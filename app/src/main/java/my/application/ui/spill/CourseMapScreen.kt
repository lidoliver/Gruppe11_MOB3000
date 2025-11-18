package my.application.ui.spill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import my.application.ui.components.AppTopBar
import my.application.data.repository.TrackRepository

// Data class for hole information
data class HoleLocation(
    val holeNumber: Int,
    val teePosition: LatLng,
    val basketPosition: LatLng,
    val par: Int = 3
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMapScreen(
    navController: NavController,
    courseId: String?
) {
    val context = LocalContext.current
    val trackRepository = remember { TrackRepository(context) }

    var courseName by remember { mutableStateOf("Disc Golf Course") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedHole by remember { mutableStateOf<Int?>(null) }
    var holes by remember { mutableStateOf<List<HoleLocation>>(emptyList()) }

    // Default camera position (Oslo, Norway)
    val defaultPosition = LatLng(59.9139, 10.7522)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 15f)
    }

    // Load course data and hole coordinates from database
    LaunchedEffect(courseId) {
        loading = true
        error = null

        if (courseId != null) {
            try {
                // Get course info with holes data from database
                val trackResult = trackRepository.getTrackById(courseId)
                trackResult.onSuccess { track ->
                    courseName = track.name

                    // Check if track has holes data with GPS coordinates from database
                    if (track.holesData != null && track.holesData.isNotEmpty()) {
                        // Convert holes data to HoleLocation objects - only valid coordinates
                        val fetchedHoles = track.holesData.mapNotNull { holeData ->
                            if (holeData.teeLatitude != null && holeData.teeLongitude != null &&
                                holeData.basketLatitude != null && holeData.basketLongitude != null) {
                                HoleLocation(
                                    holeNumber = holeData.holeNumber,
                                    teePosition = LatLng(holeData.teeLatitude, holeData.teeLongitude),
                                    basketPosition = LatLng(holeData.basketLatitude, holeData.basketLongitude),
                                    par = holeData.par
                                )
                            } else {
                                println("Skipping hole ${holeData.holeNumber} - missing GPS coordinates")
                                null
                            }
                        }

                        if (fetchedHoles.isNotEmpty()) {
                            holes = fetchedHoles
                            // Center camera on first hole
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                fetchedHoles[0].teePosition,
                                16f
                            )
                            println("✅ Loaded ${fetchedHoles.size} holes with GPS coordinates from database")
                        } else {
                            error = "No holes with GPS coordinates found in database for this course"
                            println("❌ Course has ${track.holesData.size} holes but none have GPS coordinates")
                        }
                    } else {
                        error = "No holes data found in database for this course"
                        println("❌ Course '${track.name}' has no holes in database")
                    }

                    loading = false
                }.onFailure { exception ->
                    error = "Failed to load course: ${exception.message}"
                    loading = false
                    println("❌ Error loading course: ${exception.message}")
                }
            } catch (e: Exception) {
                error = "Error loading course data: ${e.message}"
                loading = false
                println("❌ Exception loading course: ${e.message}")
            }
        } else {
            error = "No course ID provided"
            loading = false
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = courseName,
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
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }
                else -> {
                    // Map view
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = false,
                            mapType = MapType.HYBRID
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false,
                            compassEnabled = true
                        )
                    ) {
                        // Draw markers for each hole
                        holes.forEach { hole ->
                            // Tee marker (green)
                            Marker(
                                state = MarkerState(position = hole.teePosition),
                                title = "Hole ${hole.holeNumber} - Tee",
                                snippet = "Par ${hole.par}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                onClick = {
                                    selectedHole = hole.holeNumber
                                    true
                                }
                            )

                            // Basket marker (red)
                            Marker(
                                state = MarkerState(position = hole.basketPosition),
                                title = "Hole ${hole.holeNumber} - Basket",
                                snippet = "Par ${hole.par}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                                onClick = {
                                    selectedHole = hole.holeNumber
                                    true
                                }
                            )

                            // Draw polyline between tee and basket
                            Polyline(
                                points = listOf(hole.teePosition, hole.basketPosition),
                                color = Color.Blue,
                                width = 5f
                            )
                        }
                    }

                    // Hole info card overlay
                    selectedHole?.let { holeNum ->
                        val hole = holes.find { it.holeNumber == holeNum }
                        hole?.let {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Hole ${it.holeNumber}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Par ${it.par}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Button(
                                                onClick = { selectedHole = null },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Close")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    // Center camera on this hole
                                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                                        it.teePosition,
                                                        18f
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Center")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Legend overlay
                    if (selectedHole == null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Legend",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color.Green, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tee", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color.Red, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Basket", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .height(2.dp)
                                                .background(Color.Blue)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Flight Path", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



