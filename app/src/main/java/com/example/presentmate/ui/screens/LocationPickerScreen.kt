package com.example.presentmate.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.presentmate.LocationPickerViewModel
import com.example.presentmate.SearchHistoryRepository
import com.example.presentmate.data.AppDatabase
import com.example.presentmate.data.SavedPlacesRepository
import com.example.presentmate.geofence.GeofenceManager
import com.example.presentmate.geofence.GeofenceUtils
import com.example.presentmate.ui.components.PermissionDeniedContent
import com.example.presentmate.ui.components.common.InitialLoadingContent
import com.example.presentmate.ui.components.location.LocationPermissionRationaleDialog
import com.example.presentmate.ui.components.location.LocationPickerContent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import org.osmdroid.util.GeoPoint
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationPickerScreen(
    searchHistoryRepository: SearchHistoryRepository,
    savedPlacesRepository: SavedPlacesRepository,
    navController: NavController? = null,
    onLocationConfirmed: (GeoPoint) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)

            // Add background location permission for Android 10+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            // Add notification permission for Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    val viewModel: LocationPickerViewModel = viewModel(
        factory = LocationPickerViewModel.Companion.provideFactory(
            geocoder,
            searchHistoryRepository,
            savedPlacesRepository,
            fusedLocationProviderClient
        )
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    // Handle errors with Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.onPerformSearch()
                }
                SnackbarResult.Dismissed -> {
                    viewModel.clearError()
                }
            }
        }
    }

    // Check permissions on initial launch
    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted && !hasRequestedPermission) {
            if (locationPermissionsState.shouldShowRationale) {
                showPermissionRationale = true
            } else {
                locationPermissionsState.launchMultiplePermissionRequest()
                hasRequestedPermission = true
            }
        }
    }

    // Show permission rationale dialog
    if (showPermissionRationale) {
        LocationPermissionRationaleDialog(
            onDismiss = {
                showPermissionRationale = false
                onNavigateBack()
            },
            onConfirm = {
                showPermissionRationale = false
                locationPermissionsState.launchMultiplePermissionRequest()
                hasRequestedPermission = true
            }
        )
    }

    if (showSaveDialog) {
        var locationName by remember { mutableStateOf("") }
        var geofenceRadius by remember { mutableFloatStateOf(200f) }
        var enableGeofencing by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Location & Geofence") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Enter a name for this location and configure geofencing.")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Location Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Geofence Radius: ${geofenceRadius.toInt()} meters",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Slider(
                        value = geofenceRadius,
                        onValueChange = { geofenceRadius = it },
                        valueRange = 50f..1000f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Enable Geofencing",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = enableGeofencing,
                            onCheckedChange = { enableGeofencing = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedLocation = uiState.selectedLocation
                        if (locationName.isNotBlank() && selectedLocation != null) {
                            // Save the location
                            viewModel.saveSelectedLocation(locationName)
                            showSaveDialog = false

                            // Save geofencing preferences
                            val prefs = context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE)
                            with(prefs.edit()) {
                                putFloat("geofence_latitude", selectedLocation.latitude.toFloat())
                                putFloat("geofence_longitude", selectedLocation.longitude.toFloat())
                                putFloat("geofence_radius", geofenceRadius)
                                putBoolean("geofence_enabled", enableGeofencing)
                                apply()
                            }

                            val geofenceManager = GeofenceManager(context)
                            val pendingIntent = GeofenceUtils.createGeofencePendingIntent(context)

                            if (enableGeofencing) {
                                geofenceManager.addGeofence(
                                    locationName,
                                    selectedLocation.latitude,
                                    selectedLocation.longitude,
                                    geofenceRadius,
                                    pendingIntent
                                )
                            } else {
                                geofenceManager.removeGeofence(pendingIntent)
                            }

                            onLocationConfirmed(selectedLocation)
                        }
                    },
                    enabled = locationName.isNotBlank() && uiState.selectedLocation != null
                ) {
                    Text("Save & Activate Geofence")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show appropriate content based on permissions and loading state
            when {
                !locationPermissionsState.allPermissionsGranted && hasRequestedPermission -> {
                    PermissionDeniedContent(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        onDismiss = onNavigateBack
                    )
                }
                uiState.isInitializing -> {
                    InitialLoadingContent()
                }
                else -> {
                    LocationPickerContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onLocationConfirmed = { showSaveDialog = true },
                        onGoToCurrentLocation = {
                            if (locationPermissionsState.allPermissionsGranted) {
                                viewModel.getCurrentLocation()
                            } else {
                                if (locationPermissionsState.shouldShowRationale) {
                                    showPermissionRationale = true
                                } else {
                                    locationPermissionsState.launchMultiplePermissionRequest()
                                    hasRequestedPermission = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun LocationPickerScreenPreview() {
    val context = LocalContext.current
    val searchHistoryRepository = remember { SearchHistoryRepository(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val savedPlacesRepository = remember { SavedPlacesRepository(database.savedPlaceDao()) }
    LocationPickerScreen(
        searchHistoryRepository = searchHistoryRepository,
        savedPlacesRepository = savedPlacesRepository,
        onLocationConfirmed = {},
        onNavigateBack = {}
    )
}
