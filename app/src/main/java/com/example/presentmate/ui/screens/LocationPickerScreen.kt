package com.example.presentmate.ui.screens

import android.Manifest
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.presentmate.viewmodel.LocationPickerViewModel
import com.example.presentmate.SearchHistoryRepository
import com.example.presentmate.data.SavedPlacesRepository
import com.example.presentmate.ui.components.PermissionDeniedContent
import com.example.presentmate.ui.components.common.InitialLoadingContent
import com.example.presentmate.ui.components.location.LocationPermissionRationaleDialog
import com.example.presentmate.ui.components.location.LocationPickerContent
import com.example.presentmate.ui.components.location.SaveLocationDialog
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

    var locationName by remember(uiState.locationName) { mutableStateOf(uiState.locationName) }
    var geofenceRadius by remember { mutableStateOf(uiState.geofenceRadius) }
    var enableGeofencing by remember { mutableStateOf(uiState.enableGeofencing) }

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
        SaveLocationDialog(
            onDismissRequest = { showSaveDialog = false },
            onConfirm = { newLocationName, newGeofenceRadius, newEnableGeofencing ->
                viewModel.saveLocationAndGeofence(
                    context = context,
                    locationName = newLocationName,
                    geofenceRadius = newGeofenceRadius,
                    enableGeofencing = newEnableGeofencing
                )
                showSaveDialog = false
                uiState.selectedLocation?.let { onLocationConfirmed(it) }
            },
            isConfirmEnabled = uiState.selectedLocation != null,
            locationName = locationName,
            onLocationNameChange = { locationName = it },
            geofenceRadius = geofenceRadius,
            onGeofenceRadiusChange = { geofenceRadius = it },
            enableGeofencing = enableGeofencing,
            onEnableGeofencingChange = { enableGeofencing = it }
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