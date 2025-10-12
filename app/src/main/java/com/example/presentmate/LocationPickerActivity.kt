package com.example.presentmate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

class LocationPickerActivity : ComponentActivity() {
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val initialLocation = mutableStateOf<GeoPoint?>(null)
    private lateinit var searchHistoryRepository: SearchHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchHistoryRepository = SearchHistoryRepository(this)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getCurrentLocation()
            }
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        setContent {
            LocationPickerScreen(initialLocation.value, searchHistoryRepository) { 
                val intent = Intent().apply {
                    putExtra("latitude", it.latitude)
                    putExtra("longitude", it.longitude)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    initialLocation.value = GeoPoint(it.latitude, it.longitude)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLocation: GeoPoint?,
    searchHistoryRepository: SearchHistoryRepository,
    onLocationSelected: (GeoPoint) -> Unit
) {
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var addressText by remember { mutableStateOf("Tap to select a location") }
    var searchQuery by remember { mutableStateOf("") }
    var searchHistory by remember { mutableStateOf(searchHistoryRepository.getSearchHistory()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val mapView = remember { MapView(context) }

    Scaffold(
        topBar = {
            Column {
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        if (it.length > 2) {
                            coroutineScope.launch {
                                val addresses = geocoder.getFromLocationName(it, 5)
                                if (addresses != null && addresses.isNotEmpty()) {
                                    val newLocation = GeoPoint(addresses[0].latitude, addresses[0].longitude)
                                    selectedLocation = newLocation
                                    mapView.controller.setCenter(newLocation)
                                }
                            }
                        }
                    },
                    placeholder = { Text("Search for a location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = addressText, modifier = Modifier.padding(16.dp))
            }
        },
        bottomBar = {
            Button(
                onClick = { 
                    selectedLocation?.let { onLocationSelected(it) } 
                    searchHistoryRepository.addToSearchHistory(searchQuery)
                },
                modifier = Modifier.padding(16.dp),
                enabled = selectedLocation != null
            ) {
                Text("Select Location")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(searchHistory, key = { it }) { query ->
                    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                coroutineScope.launch {
                                    searchHistoryRepository.removeFromSearchHistory(query)
                                    searchHistory = searchHistoryRepository.getSearchHistory()
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = swipeToDismissBoxState,
                        backgroundContent = {
                            val direction = swipeToDismissBoxState.dismissDirection
                            val color = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> ComposeColor.Red
                                SwipeToDismissBoxValue.EndToStart -> ComposeColor.Red
                                else -> ComposeColor.Transparent
                            }
                            val alignment = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = ComposeColor.White
                                )
                            }
                        }
                    ) {
                        Text(
                            text = query,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    searchQuery = query
                                    coroutineScope.launch {
                                        val addresses = geocoder.getFromLocationName(query, 1)
                                        if (addresses != null && addresses.isNotEmpty()) {
                                            val newLocation = GeoPoint(addresses[0].latitude, addresses[0].longitude)
                                            selectedLocation = newLocation
                                            mapView.controller.setCenter(newLocation)
                                        }
                                    }
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(2f)) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize(),
                    update = {
                        it.controller.setZoom(15.0)
                        it.setMultiTouchControls(true)
                        it.controller.setCenter(initialLocation ?: GeoPoint(20.5937, 78.9629)) // Default center

                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                selectedLocation = p
                                it.overlays.clear()

                                // Add marker
                                val marker = Marker(it)
                                marker.position = p
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                it.overlays.add(marker)

                                // Add circle for radius
                                val circle = Polygon().apply {
                                    points = Polygon.pointsAsCircle(p, 100.0)
                                    fillColor = 0x30FF0000
                                    strokeColor = Color.RED
                                    strokeWidth = 2f
                                }
                                it.overlays.add(circle)

                                it.invalidate() // Redraw map

                                // Update address
                                try {
                                    val addresses = geocoder.getFromLocation(p.latitude, p.longitude, 1)
                                    addressText = addresses?.firstOrNull()?.getAddressLine(0) ?: "No address found"
                                } catch (e: Exception) {
                                    addressText = "Could not get address"
                                }

                                return true
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean {
                                return false
                            }
                        }
                        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                        it.overlays.add(0, mapEventsOverlay)
                    }
                )
            }
        }
    }
}
