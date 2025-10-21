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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentmate.LocationPickerViewModel
import com.example.presentmate.SearchHistoryRepository
import com.example.presentmate.data.AppDatabase
import com.example.presentmate.data.SavedPlacesRepository
import com.example.presentmate.ui.components.InitialLoadingContent
import com.example.presentmate.ui.components.LocationPermissionRationaleDialog
import com.example.presentmate.ui.components.LocationPickerContent
import com.example.presentmate.ui.components.PermissionDeniedContent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.Locale
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationPickerScreen(
    searchHistoryRepository: SearchHistoryRepository,
    savedPlacesRepository: SavedPlacesRepository, // Add this
    onLocationConfirmed: (GeoPoint) -> Unit,
    onNavigateBack: () -> Unit = {},
    initialLocation: GeoPoint? = null
) {
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

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
            initialLocation
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
                        onLocationConfirmed = onLocationConfirmed
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
        onNavigateBack = {},
        initialLocation = GeoPoint(40.7128, -74.0060) // New York City
    )
}
