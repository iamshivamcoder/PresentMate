package com.example.presentmate.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.presentmate.db.PresentMateDatabase
import com.example.presentmate.data.SavedPlace
import com.example.presentmate.data.SavedPlacesRepository
import com.example.presentmate.geofence.GeofenceManager
import com.example.presentmate.geofence.GeofenceUtils
import com.example.presentmate.ui.components.PermissionDeniedContent
import com.example.presentmate.data.GeofencePreferencesRepository
import com.example.presentmate.utils.LocationUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GeofenceScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val db = remember { PresentMateDatabase.getDatabase(context) }
    val savedPlacesRepository = remember { SavedPlacesRepository(db.savedPlaceDao()) }
    val geofenceManager = remember { GeofenceManager(context) }
    val prefs = remember { GeofencePreferencesRepository.getPreferences(context) }

    val savedPlaces by savedPlacesRepository.getAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedSavedPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var radiusMeters by remember { mutableFloatStateOf(GeofencePreferencesRepository.getGeofenceRadius(context)) }
    var isTrackingEnabled by remember { mutableStateOf(GeofencePreferencesRepository.isGeofenceEnabled(context)) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var showLocationServicesDialog by rememberSaveable { mutableStateOf(false) }

    val isLocationEnabled = LocationUtils.isLocationEnabled(context)

    val geofencePendingIntent = remember {
        GeofenceUtils.createGeofencePendingIntent(context)
    }

    LaunchedEffect(savedPlaces) {
        if (selectedSavedPlace == null && savedPlaces.isNotEmpty()) {
            val savedPlaceId = prefs.getInt("geofence_place_id", -1)
            val foundPlace = if (savedPlaceId != -1) {
                savedPlaces.find { it.id == savedPlaceId }
            } else {
                // Fallback: try to find by coordinates with epsilon comparison
                val savedLat = prefs.getFloat("geofence_latitude", 0f).toDouble()
                val savedLon = prefs.getFloat("geofence_longitude", 0f).toDouble()
                savedPlaces.find { 
                    kotlin.math.abs(it.latitude - savedLat) < 0.0001 && 
                    kotlin.math.abs(it.longitude - savedLon) < 0.0001 
                }
            }
            selectedSavedPlace = foundPlace ?: savedPlaces.firstOrNull()
        }
    }

    val centerPoint = selectedSavedPlace?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(
        20.5937,
        78.9629
    )

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(13.0)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    val circleOverlay = remember { Polygon() }

    LaunchedEffect(selectedSavedPlace) {
        selectedSavedPlace?.let {
            val newGeoPoint = GeoPoint(it.latitude, it.longitude)
            mapView.controller.animateTo(newGeoPoint)
            mapView.controller.setCenter(newGeoPoint)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            Triple(
                selectedSavedPlace?.id,
                radiusMeters,
                isTrackingEnabled
            )
        }
            .drop(1) // Skip the initial emission
            .distinctUntilChanged() // Only emit when values actually change
            .collectLatest { (_, radius, enabled) ->
                if (permissionsState.allPermissionsGranted && isLocationEnabled && enabled && selectedSavedPlace != null) {
                    selectedSavedPlace?.let {
                        geofenceManager.addGeofence(
                            it.id.toString(),
                            it.latitude,
                            it.longitude,
                            radius,
                            geofencePendingIntent
                        )
                    }
                } else if (enabled && !isLocationEnabled) {
                    showLocationServicesDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted && !hasRequestedPermission) {
            if (permissionsState.shouldShowRationale) {
                showPermissionRationale = true
            } else {
                permissionsState.launchMultiplePermissionRequest()
                hasRequestedPermission = true
            }
        }
    }

    if (showPermissionRationale) {
        val rationaleDialog = @Composable {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showPermissionRationale = false
                    navController.popBackStack()
                },
                title = { Text("Permissions Required") },
                text = { Text("Location and notification permissions are required for geofence tracking to work properly.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionRationale = false
                            permissionsState.launchMultiplePermissionRequest()
                            hasRequestedPermission = true
                        }
                    ) {
                        Text("Grant Permissions")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { navController.popBackStack() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        rationaleDialog()
    }

    if (showLocationServicesDialog) {
        LocationServicesDisabledDialog(
            onDismiss = { showLocationServicesDialog = false },
            onConfirm = {
                showLocationServicesDialog = false
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            }
        )
    }

    DisposableEffect(radiusMeters, centerPoint) {
        circleOverlay.points = Polygon.pointsAsCircle(centerPoint, radiusMeters.toDouble())
        circleOverlay.fillPaint.color = "#4D2196F3".toColorInt()
        circleOverlay.outlinePaint.color = "#2196F3".toColorInt()
        circleOverlay.outlinePaint.strokeWidth = 4f

        if (!mapView.overlays.contains(circleOverlay)) {
            mapView.overlays.add(circleOverlay)
        }
        mapView.invalidate()

        onDispose { }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onDetach()
        }
    }

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !permissionsState.allPermissionsGranted && hasRequestedPermission -> {
                    PermissionDeniedContent(
                        onOpenSettings = {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        },
                        onDismiss = { navController.popBackStack() }
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            if (LocalInspectionMode.current) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colorScheme.surfaceVariant)
                                        .align(Alignment.Center)
                                ) {
                                    Text("Map Preview", color = colorScheme.onSurfaceVariant)
                                }
                            } else {
                                AndroidView(
                                    factory = { mapView },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(colorScheme.surface)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Select a saved location and set your geofence.",
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = "Choose a Saved Location",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (savedPlaces.isEmpty()) {
                                Text(
                                    text = "No saved locations yet. Save some from the Location picker!",
                                    style = typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column {
                                    savedPlaces.forEach { place ->
                                        ListItem(
                                            headlineContent = { Text(place.name) },
                                            supportingContent = {
                                                Text(
                                                    place.address,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            leadingContent = {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSavedPlace = place
                                                    prefs.edit {
                                                        putInt("geofence_place_id", place.id)
                                                        putFloat(
                                                            "geofence_latitude",
                                                            place.latitude.toFloat()
                                                        )
                                                        putFloat(
                                                            "geofence_longitude",
                                                            place.longitude.toFloat()
                                                        )
                                                        putString("geofence_place_name", place.name)
                                                    }
                                                    view.performHapticFeedback(
                                                        HapticFeedbackConstants.CLOCK_TICK
                                                    )
                                                },
                                            trailingContent = {
                                                if (selectedSavedPlace?.id == place.id) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = colorScheme.primary
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            selectedSavedPlace?.let { place ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = place.name,
                                                    style = typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = place.address,
                                                    style = typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            } ?: run {
                                Text(
                                    text = "No location selected. Please select a location from the list above.",
                                    style = typography.bodyMedium,
                                    color = colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Radius: ${radiusMeters.toInt()} meters",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Slider(
                                value = radiusMeters,
                                onValueChange = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    radiusMeters = it
                                    prefs.edit {
                                        putFloat("geofence_radius", radiusMeters)
                                    }
                                },
                                valueRange = 50f..1000f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Geofence radius slider" },
                                colors = SliderDefaults.colors(
                                    thumbColor = colorScheme.primary,
                                    activeTrackColor = colorScheme.primary,
                                    inactiveTrackColor = colorScheme.surfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable Geofence",
                                    style = typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = isTrackingEnabled,
                                    onCheckedChange = { checked ->
                                        isTrackingEnabled = checked
                                        if (checked) {
                                            prefs.edit {
                                                putBoolean("geofence_enabled", true)
                                            }
                                            if (permissionsState.allPermissionsGranted && isLocationEnabled) {
                                                selectedSavedPlace?.let {
                                                    geofenceManager.addGeofence(
                                                        it.id.toString(),
                                                        it.latitude,
                                                        it.longitude,
                                                        radiusMeters,
                                                        geofencePendingIntent
                                                    )
                                                }
                                            } else if (!isLocationEnabled) {
                                                showLocationServicesDialog = true
                                            }
                                        } else {
                                            prefs.edit {
                                                putBoolean("geofence_enabled", false)
                                            }
                                            geofenceManager.removeGeofence(geofencePendingIntent)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colorScheme.primary,
                                        checkedTrackColor = colorScheme.primaryContainer,
                                        uncheckedThumbColor = colorScheme.outline,
                                        uncheckedTrackColor = colorScheme.surfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { navController.navigate("locationPickerScreen") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add New Location")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationServicesDisabledDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Services Disabled") },
        text = { Text("Please enable location services for automatic session tracking.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Enable")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

