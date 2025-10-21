package com.example.presentmate.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    savedLocation: GeoPoint? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableFloatStateOf(200f) }
    var isTrackingEnabled by remember { mutableStateOf(true) }
    var centerPoint by remember { mutableStateOf(savedLocation ?: GeoPoint(20.5937, 78.9629)) } // Default to India or saved location

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setTileSource(TileSourceFactory.MAPNIK)
            controller.setZoom(13.0)
            controller.setCenter(centerPoint)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }

    val circleOverlay = remember { Polygon() }

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

    // Update map center when the savedLocation changes
    DisposableEffect(savedLocation) {
        savedLocation?.let {
            centerPoint = it
            mapView.controller.animateTo(it)
        }
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
                            .background(Color.Gray)
                            .align(Alignment.Center)
                    ) {
                        Text("Map Preview", color = Color.White)
                    }
                } else {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Current Location FAB
                FloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        // Navigate to current location
                        mapView.controller.animateTo(centerPoint)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .semantics { contentDescription = "Go to current location" },
                    containerColor = colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = colorScheme.onPrimary
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
                    text = "Select a location and set your geofence.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search for a location",
                            color = colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Search location input" },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.outline,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            // Implement search logic here
                        }
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Location Name Field
                Text(
                    text = "Location Name",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    placeholder = {
                        Text(
                            "e.g., Work, University",
                            color = colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Location name input" },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.outline,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Radius Slider
                Text(
                    text = "Radius",
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
                        onCheckedChange = { isTrackingEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorScheme.primary,
                            checkedTrackColor = colorScheme.primaryContainer,
                            uncheckedThumbColor = colorScheme.outline,
                            uncheckedTrackColor = colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        // Implement save logic here
                        navController.navigateUp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics { contentDescription = "Save geofence" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Save Geofence",
                        style = typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Saved Geofences Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Saved Geofences",
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Placeholder for a list of saved geofences
                    Text(
                        text = "List of saved geofences will go here.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    // TODO: Implement actual list of saved geofences
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeofenceScreenPreview() {
    GeofenceScreen(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun GeofenceScreenWithLocationPreview() {
    GeofenceScreen(
        navController = rememberNavController(),
        savedLocation = GeoPoint(40.7128, -74.0060) // New York City
    )
}
