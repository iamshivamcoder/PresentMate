@file:Suppress("DEPRECATION")
package com.example.presentmate

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    val searchCache = remember { mutableMapOf<String, List<Address>>() }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val keyboardController = LocalSoftwareKeyboardController.current

    suspend fun getAddressText(geoPoint: GeoPoint): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                            ?: String.format(Locale.US, "Lat: %.6f, Lng: %.6f", geoPoint.latitude, geoPoint.longitude)
                        continuation.resume(address)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                        ?: String.format(Locale.US, "Lat: %.6f, Lng: %.6f", geoPoint.latitude, geoPoint.longitude)
                    continuation.resume(address)
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }


    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(20.5937, 78.9629)) // Default to India center
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(initialLocation.value) {
        initialLocation.value?.let {
            mapView.controller.setCenter(it)
        }
    }

    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { geoPoint ->
            coroutineScope.launch {
                addressText = getAddressText(geoPoint)
            }
        }
    }

    fun performSearch(query: String) {
        if (query.trim().isEmpty()) return

        if (searchCache.containsKey(query)) {
            searchSuggestions = searchCache.getValue(query)
            showSuggestions = true
            return
        }

        coroutineScope.launch {
            try {
                isSearching = true
                val addresses = suspendCancellableCoroutine<List<Address>?> { continuation ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        geocoder.getFromLocationName(query, 5) { addressList ->
                            continuation.resume(addressList)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        continuation.resume(geocoder.getFromLocationName(query, 5))
                    }
                }

                if (addresses?.isNotEmpty() == true) {
                    searchSuggestions = addresses
                    searchCache[query] = addresses
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

    fun goToCurrentLocation() {
        initialLocation.value?.let { currentLoc ->
            selectedLocation = currentLoc
            mapView.controller.animateTo(currentLoc)
        }
    }

    Column {
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
                            if (it.isEmpty()) {
                                showSuggestions = false
                                searchHistory = searchHistoryRepository.getSearchHistory()
                            }
                        },
                        placeholder = { Text("Search for a location") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
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
                    IconButton(onClick = { goToCurrentLocation() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                    }
                }
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
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    map.overlays.clear()

                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            selectedLocation = p
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    }

                    val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                    map.overlays.add(0, mapEventsOverlay)

                    selectedLocation?.let { geoPoint ->
                        val marker = Marker(map).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Selected Location"
                        }
                        val circle = Polygon().apply {
                            points = Polygon.pointsAsCircle(geoPoint, 100.0)
                            // Use Paint objects instead of deprecated convenience properties
                            fillPaint.color = Color.Red.copy(alpha = 0.19f).toArgb()
                            outlinePaint.color = Color.Red.toArgb()
                            outlinePaint.strokeWidth = 2f
                        }
                        map.overlays.add(marker)
                        map.overlays.add(circle)
                    }

                    map.invalidate()
                }
            )

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

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

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
                                    .background(Color.Red)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
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
