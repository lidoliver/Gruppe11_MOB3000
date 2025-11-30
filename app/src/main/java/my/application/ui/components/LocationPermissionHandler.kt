package my.application.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import my.application.service.LocationManager

/**
 * Composable som håndterer forespørsel om posisjonstillatelse
 * Sjekker først om tillatelse allerede er gitt, ellers ber om tillatelse
 * @param onPermissionGranted Callback når tillatelse er gitt
 * @param onPermissionDenied Callback når tillatelse er avslått
 */
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val locationManager = remember { LocationManager(context) }

    // Launcher for tillatelsesforespørsel
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                onPermissionGranted()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                onPermissionGranted()
            }
            else -> {
                onPermissionDenied()
            }
        }
    }

    // Sjekk tillatelse ved oppstart
    LaunchedEffect(Unit) {
        if (locationManager.hasLocationPermission()) {
            onPermissionGranted()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

