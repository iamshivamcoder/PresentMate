package com.example.presentmate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor

class LocationPickerActivity : ComponentActivity() {
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val initialLocation = mutableStateOf<GeoPoint?>(null)
    private lateinit var searchHistoryRepository: SearchHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid BEFORE creating MapView
        Configuration.getInstance().load(this, this.getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        searchHistoryRepository = SearchHistoryRepository(this)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                getCurrentLocation()
            }
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        setContent {
            LocationPickerScreen(
                initialLocation = initialLocation,
                searchHistoryRepository = searchHistoryRepository
            ) { geoPoint ->
                val intent = Intent().apply {
                    putExtra("latitude", geoPoint.latitude)
                    putExtra("longitude", geoPoint.longitude)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    initialLocation.value = GeoPoint(it.latitude, it.longitude)
                }
            }.addOnFailureListener {
                Log.e("LocationPicker", "Failed to get location", it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLocation: MutableState<GeoPoint?>,
    searchHistoryRepository: SearchHistoryRepository,
    onLocationSelected: (GeoPoint) -> Unit
) {
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var addressText by remember { mutableStateOf("Tap on map to select location") }
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var searchHistory by remember { mutableStateOf(searchHistoryRepository.getSearchHistory()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Create MapView with proper configuration
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            // Set default location (India center)
            controller.setCenter(GeoPoint(20.5937, 78.9629))

            // Configure tile source for better loading
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        }
    }

    // Handle MapView lifecycle to prevent memory leaks
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Search function that only triggers on submit or suggestion tap
    fun performSearch(query: String) {
        if (query.trim().isEmpty()) return

        coroutineScope.launch {
            try {
                isSearching = true
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(query, 5)
                }

                if (addresses != null && addresses.isNotEmpty()) {
                    searchSuggestions = addresses
                    showSuggestions = true
                } else {
                    searchSuggestions = emptyList()
                    showSuggestions = false
                }
            } catch (e: Exception) {
                Log.e("LocationPicker", "Search failed", e)
                searchSuggestions = emptyList()
                showSuggestions = false
            } finally {
                isSearching = false
            }
        }
    }

    // Get current location function
    fun goToCurrentLocation() {
        initialLocation.value?.let { currentLoc ->
            selectedLocation = currentLoc
            mapView.controller.animateTo(currentLoc)

            // Update address for current location
            coroutineScope.launch {
                try {
                    val addresses = withContext(Dispatchers.IO) {
                        geocoder.getFromLocation(currentLoc.latitude, currentLoc.longitude, 1)
                    }
                    addressText = addresses?.firstOrNull()?.getAddressLine(0) ?: "Current Location"
                } catch (e: Exception) {
                    addressText = "Current Location"
                }
            }
        }
    }

    Column {
        // Search Bar with improved UX
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            // Only show history when field is focused but empty
                            if (it.isEmpty()) {
                                showSuggestions = false
                                searchHistory = searchHistoryRepository.getSearchHistory()
                            }
                        },
                        placeholder = { Text("Search for a location") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    showSuggestions = false
                                    searchSuggestions = emptyList()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                performSearch(searchQuery)
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true
                    )

                    // Current Location Button
                    IconButton(onClick = { goToCurrentLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                    }
                }

                // Quick Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { goToCurrentLocation() },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Text("Current Location")
                    }

                    Button(
                        onClick = {
                            selectedLocation?.let { onLocationSelected(it) }
                            if (searchQuery.isNotEmpty()) {
                                searchHistoryRepository.addToSearchHistory(searchQuery)
                            }
                        },
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        enabled = selectedLocation != null
                    ) {
                        Text("Select Location")
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            // Map View
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    // Set initial location if available
                    initialLocation.value?.let {
                        map.controller.setCenter(it)
                    }

                    // Clear existing overlays
                    map.overlays.clear()

                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            selectedLocation = p

                            // Add marker at selected location
                            val marker = Marker(map).apply {
                                position = p
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Selected Location"
                            }
                            map.overlays.add(marker)

                            // Add geofence circle
                            val circle = Polygon().apply {
                                points = Polygon.pointsAsCircle(p, 100.0)
                                fillColor = ComposeColor.Red.copy(alpha = 0.19f).value.toInt() // 0x30FF0000 equivalent
                                strokeColor = ComposeColor.Red.value.toInt()
                                strokeWidth = 2f
                            }
                            map.overlays.add(circle)

                            map.invalidate()

                            // Update address
                            coroutineScope.launch {
                                try {
                                    val addresses = withContext(Dispatchers.IO) {
                                        geocoder.getFromLocation(p.latitude, p.longitude, 1)
                                    }
                                    addressText = addresses?.firstOrNull()?.getAddressLine(0)
                                        ?: "Lat: ${String.format("%.6f", p.latitude)}, Lng: ${String.format("%.6f", p.longitude)}"
                                } catch (e: java.io.IOException) {
                                    addressText = "Network error"
                                } catch (e: Exception) {
                                    addressText = "Lat: ${String.format("%.6f", p.latitude)}, Lng: ${String.format("%.6f", p.longitude)}"
                                }
                            }

                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    }

                    val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                    map.overlays.add(0, mapEventsOverlay)
                }
            )

            // Floating address card
            if (selectedLocation != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = addressText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Loading indicator
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            ComposeColor.Black.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(color = ComposeColor.White)
                }
            }
        }

        // Search Suggestions and History (only show when relevant)
        if (showSuggestions && searchSuggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items(searchSuggestions) { address ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clickable {
                                val newLocation = GeoPoint(address.latitude, address.longitude)
                                selectedLocation = newLocation
                                mapView.controller.animateTo(newLocation)
                                searchQuery = address.getAddressLine(0) ?: ""
                                addressText = address.getAddressLine(0) ?: "Selected Location"
                                showSuggestions = false
                                searchSuggestions = emptyList()
                            }
                    ) {
                        Text(
                            text = address.getAddressLine(0) ?: "Unknown Location",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
            // Show search history when search field is empty
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
            ) {
                items(searchHistory, key = { it }) { query ->
                    val dismissState = rememberSwipeToDismissBoxState(
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
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ComposeColor.Red)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = ComposeColor.White
                                )
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .clickable {
                                    searchQuery = query
                                    performSearch(query)
                                }
                        ) {
                            Text(
                                text = query,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
