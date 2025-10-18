package com.example.presentmate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.presentmate.db.AppDatabase
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

const val GEOFENCE_RADIUS = 100f

@Composable
fun LocationStatusScreen() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val prefs = remember { context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE) }

    val ongoingSession by db.attendanceDao().getAllRecords()
        .map { records -> records.find { it.timeOut == null } }
        .collectAsState(initial = null)

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var distanceToCenter by remember { mutableStateOf<Float?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val geofenceLatitude = prefs.getFloat("geofence_latitude", 0f).toDouble()
    val geofenceLongitude = prefs.getFloat("geofence_longitude", 0f).toDouble()
    val geofenceEnabled = prefs.getBoolean("geofence_enabled", false)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions.values.any { it }
        }
    )

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    if (geofenceEnabled) {
                        val distance = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude, location.longitude,
                            geofenceLatitude, geofenceLongitude,
                            distance
                        )
                        distanceToCenter = distance[0]
                    }
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Use the modern builder-style API for LocationRequest (create() and setters are deprecated)
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setMinUpdateIntervalMillis(5000L)
                .build()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            startLocationUpdates()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasLocationPermission) {
            Text(
                text = "Location permission is required to show your current status.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }) {
                Text("Grant Permission")
            }
            return
        }

        if (!geofenceEnabled) {
            Text(
                text = "Automatic session tracking is not set up.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You can enable it in the Settings screen.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            return
        }

        val geofenceColor = if (distanceToCenter != null && distanceToCenter!! <= GEOFENCE_RADIUS) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
        val secondaryColor = MaterialTheme.colorScheme.secondary

        Box(modifier = Modifier.size(250.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = geofenceColor.copy(alpha = 0.2f),
                    radius = size.minDimension / 2,
                    center = center
                )
                drawCircle(
                    color = geofenceColor,
                    radius = size.minDimension / 2,
                    center = center,
                    style = Stroke(width = 6.dp.toPx())
                )

                currentLocation?.let { userLocation ->
                    val geofenceLocation = Location("").apply {
                        latitude = geofenceLatitude
                        longitude = geofenceLongitude
                    }

                    val bearing = geofenceLocation.bearingTo(userLocation)
                    val distance = geofenceLocation.distanceTo(userLocation)

                    val distanceRatio = (distance / GEOFENCE_RADIUS).coerceAtMost(1.2f)
                    val angleRad = Math.toRadians(bearing.toDouble())

                    val userPositionX = center.x + (distanceRatio * (size.minDimension / 2) * sin(angleRad)).toFloat()
                    val userPositionY = center.y - (distanceRatio * (size.minDimension / 2) * cos(angleRad)).toFloat()

                    drawCircle(
                        color = secondaryColor,
                        radius = 10.dp.toPx(),
                        center = Offset(userPositionX, userPositionY)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val statusText = if (ongoingSession != null) {
            "You are inside the geofence."
        } else {
            "You are outside the geofence."
        }
        Text(text = statusText, style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        distanceToCenter?.let {
            Text(
                text = "Distance to center: %.1f meters".format(it),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        ongoingSession?.let { session ->
            session.timeIn?.let { timeIn ->
                var sessionDuration by remember { mutableStateOf(0L) }

                LaunchedEffect(timeIn) {
                    while (true) {
                        sessionDuration = System.currentTimeMillis() - timeIn
                        delay(1000)
                    }
                }

                val hours = TimeUnit.MILLISECONDS.toHours(sessionDuration)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(sessionDuration) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(sessionDuration) % 60

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Session timer: %02d:%02d:%02d".format(hours, minutes, seconds),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
