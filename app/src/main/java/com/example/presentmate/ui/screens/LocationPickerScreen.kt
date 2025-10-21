package com.example.presentmate.ui.screens

import android.Manifest
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.presentmate.LocationPickerViewModel
import com.example.presentmate.SearchHistoryRepository
import com.example.presentmate.data.AppDatabase
import com.example.presentmate.data.SavedPlacesRepository
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
    onLocationConfirmed: (GeoPoint) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
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
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Location") },
            text = {
                Column {
                    Text("Enter a name for this location.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text("Name")},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (locationName.isNotBlank()) {
                            viewModel.saveSelectedLocation(locationName)
                            showSaveDialog = false
                        }
                    },
                    enabled = locationName.isNotBlank()
                ) {
                    Text("Save")
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
        topBar = {
            // Top app bar with a save button
        },
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
                        onLocationConfirmed = onLocationConfirmed,
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

            // Save button
            IconButton(
                onClick = { showSaveDialog = true },
                enabled = uiState.selectedLocation != null && !uiState.isMapMoving,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save location")
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
