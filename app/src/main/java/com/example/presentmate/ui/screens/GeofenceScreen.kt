package com.example.presentmate.ui.screens

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.presentmate.data.AppDatabase
import com.example.presentmate.data.SavedPlace
import com.example.presentmate.data.SavedPlacesRepository
import com.example.presentmate.geofence.GeofenceBroadcastReceiver
import com.example.presentmate.geofence.GeofenceManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val db = remember { AppDatabase.getDatabase(context) }
    val savedPlacesRepository = remember { SavedPlacesRepository(db.savedPlaceDao()) }
    val geofenceManager = remember { GeofenceManager(context) }
    val prefs = remember { context.getSharedPreferences("geofence_prefs", Context.MODE_PRIVATE) }

    val savedPlaces by savedPlacesRepository.getAll().collectAsState(initial = emptyList())

    var selectedSavedPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var radiusMeters by remember { mutableFloatStateOf(prefs.getFloat("geofence_radius", 200f)) }
    var isTrackingEnabled by remember { mutableStateOf(prefs.getBoolean("geofence_enabled", false)) }

    // Initialize selectedSavedPlace with the first item once savedPlaces is loaded
    LaunchedEffect(savedPlaces) {
        if (selectedSavedPlace == null && savedPlaces.isNotEmpty()) {
            val savedLat = prefs.getFloat("geofence_latitude", 0f)
            val savedLon = prefs.getFloat("geofence_longitude", 0f)
            val foundPlace = savedPlaces.find { it.latitude.toFloat() == savedLat && it.longitude.toFloat() == savedLon }
            selectedSavedPlace = foundPlace ?: savedPlaces.first()
        }
    }

    val centerPoint = selectedSavedPlace?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(20.5937, 78.9629) // Default to India or selected location

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

    // Update map center when selectedSavedPlace changes
    LaunchedEffect(selectedSavedPlace) {
        selectedSavedPlace?.let {
            val newGeoPoint = GeoPoint(it.latitude, it.longitude)
            mapView.controller.animateTo(newGeoPoint)
            mapView.controller.setCenter(newGeoPoint)
        }
    }

    // Update geofence when enabled and params change
    LaunchedEffect(selectedSavedPlace, radiusMeters, isTrackingEnabled) {
        if (isTrackingEnabled) {
            selectedSavedPlace?.let {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, GeofenceBroadcastReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                geofenceManager.addGeofence(
                    it.id.toString(),
                    it.latitude,
                    it.longitude,
                    radiusMeters,
                    pendingIntent
                )
            }
        }
    }

    // Update circle overlay when radius or center changes
    DisposableEffect(radiusMeters, centerPoint) {
        circleOverlay.points = Polygon.pointsAsCircle(centerPoint, radiusMeters.toDouble())
        circleOverlay.fillPaint.color = android.graphics.Color.parseColor("#4D2196F3") // Semi-transparent blue
        circleOverlay.outlinePaint.color = android.graphics.Color.parseColor("#2196F3") // Blue border
        circleOverlay.outlinePaint.strokeWidth = 4f

        if (!mapView.overlays.contains(circleOverlay)) {
            mapView.overlays.add(circleOverlay)
        }
        mapView.invalidate()

        onDispose { }
    }

    // Map lifecycle management
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Make the entire column scrollable
        ) {
            // Map View Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Maintain a fixed height for the map for preview
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

            // Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(colorScheme.surface)
                    .padding(20.dp)
            ) {
                // Subtitle
                Text(
                    text = "Select a saved location and set your geofence.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Saved Locations List
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
                                supportingContent = { Text(place.address, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSavedPlace = place
                                        with(prefs.edit()) {
                                            putFloat("geofence_latitude", place.latitude.toFloat())
                                            putFloat("geofence_longitude", place.longitude.toFloat())
                                            apply()
                                        }
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    },
                                trailingContent = {
                                    if (selectedSavedPlace == place) {
                                        Icon(Icons.Default.Save, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Location Preview Section
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

                // Radius Slider
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
                        with(prefs.edit()) {
                            putFloat("geofence_radius", radiusMeters)
                            apply()
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

                // Enable/Disable Switch
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
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                0,
                                Intent(context, GeofenceBroadcastReceiver::class.java),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            )
                            if (checked) {
                                with(prefs.edit()) {
                                    putBoolean("geofence_enabled", true)
                                    apply()
                                }
                                selectedSavedPlace?.let {
                                    geofenceManager.addGeofence(
                                        it.id.toString(),
                                        it.latitude,
                                        it.longitude,
                                        radiusMeters,
                                        pendingIntent
                                    )
                                }
                            } else {
                                with(prefs.edit()) {
                                    putBoolean("geofence_enabled", false)
                                    apply()
                                }
                                geofenceManager.removeGeofence(pendingIntent)
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeofenceScreenPreview() {
    GeofenceScreen(navController = rememberNavController())
}
