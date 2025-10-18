@file:Suppress("DEPRECATION")
package com.example.presentmate

import android.location.Address
import android.location.Geocoder
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
// Import this for the .collectAsStateWithLifecycle() optimization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLocation: MutableState<GeoPoint?>,
    searchHistoryRepository: SearchHistoryRepository,
    onLocationConfirmed: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    // Use remember for dependencies that shouldn't change
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val viewModel: LocationPickerViewModel = viewModel(
        factory = LocationPickerViewModel.provideFactory(geocoder, searchHistoryRepository, initialLocation.value)
    )

    // Use collectAsStateWithLifecycle for better performance
    // It stops collecting when the app is in the background.
    // You may need to add: implementation "androidx.lifecycle:lifecycle-runtime-compose:2.8.3"
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- Main Layout Change ---
    // Use Box as the root. Everything floats on top of the map.
    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 1: The Map (fills the whole screen)
        MapViewContainer(
            modifier = Modifier.fillMaxSize(),
            selectedLocation = uiState.selectedLocation,
            initialLocation = initialLocation.value,
            onMapTap = viewModel::onLocationSelected
        )

        // Layer 2: UI Elements floating on top

        // Search bar and lists, aligned to the top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            LocationSearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onPerformSearch = viewModel::onPerformSearch,
                onClearSearch = viewModel::onClearSearch,
                onLocationConfirmed = {
                    uiState.selectedLocation?.let(onLocationConfirmed)
                    viewModel.addSearchToHistory()
                },
                isLocationSelected = uiState.selectedLocation != null,
                onGoToCurrentLocation = {
                    initialLocation.value?.let { viewModel.onLocationSelected(it) }
                }
            )

            // Lists appear below the search bar, overlaying the map
            if (uiState.showSuggestions) {
                SuggestionList(
                    suggestions = uiState.suggestions,
                    onSuggestionClicked = viewModel::onSuggestionClicked
                )
            } else if (uiState.searchQuery.isEmpty() && uiState.history.isNotEmpty()) {
                // Show history only if query is empty and history is not
                HistoryList(
                    history = uiState.history,
                    onHistoryItemClicked = viewModel::onHistoryItemClicked,
                    onRemoveFromHistory = viewModel::onRemoveFromHistory
                )
            }
        }

        // Selected Address Card, aligned to the bottom
        SelectedAddressCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            addressText = uiState.addressText,
            isVisible = uiState.selectedLocation != null
        )

        // Loading spinner, aligned to the center
        if (uiState.isSearching) {
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
}

// (LocationSearchBar is unchanged, no need to copy it)
@Composable
private fun LocationSearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onPerformSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onLocationConfirmed: () -> Unit,
    isLocationSelected: Boolean,
    onGoToCurrentLocation: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

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
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search for a location") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onPerformSearch()
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true
                )
                IconButton(onClick = onGoToCurrentLocation) {
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
                    onClick = onGoToCurrentLocation,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text("Current Location")
                }
                Button(
                    onClick = onLocationConfirmed,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    enabled = isLocationSelected
                ) {
                    Text("Select Location")
                }
            }
        }
    }
}


// (MapViewContainer is unchanged)
@Composable
private fun MapViewContainer(
    modifier: Modifier = Modifier,
    selectedLocation: GeoPoint?,
    initialLocation: GeoPoint?,
    onMapTap: (GeoPoint) -> Unit
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(initialLocation ?: GeoPoint(20.5937, 78.9629))
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        }
    }
    val marker = remember { Marker(mapView) }
    val circle = remember { Polygon() }
    val mapEventsOverlay = remember {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTap(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        MapEventsOverlay(mapEventsReceiver)
    }

    LaunchedEffect(selectedLocation) {
        mapView.overlays.remove(marker)
        mapView.overlays.remove(circle)

        selectedLocation?.let { geoPoint ->
            marker.apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Selected Location"
            }
            circle.apply {
                points = Polygon.pointsAsCircle(geoPoint, 100.0)
                fillPaint.color = Color.Red.copy(alpha = 0.19f).toArgb()
                outlinePaint.color = Color.Red.toArgb()
                outlinePaint.strokeWidth = 2f
            }
            mapView.overlays.add(marker)
            mapView.overlays.add(circle)
            mapView.controller.animateTo(geoPoint)
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.overlays.add(0, mapEventsOverlay)
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

// (SuggestionList is unchanged)
@Composable
private fun SuggestionList(
    suggestions: List<Address>,
    onSuggestionClicked: (Address) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(MaterialTheme.colorScheme.surface)
            // Add padding so the list doesn't touch the search card
            .padding(horizontal = 16.dp)
    ) {
        items(suggestions) { address ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // Remove horizontal padding from here, it's on the LazyColumn now
                    .padding(vertical = 2.dp)
                    .clickable { onSuggestionClicked(address) }
            ) {
                Text(
                    text = address.getAddressLine(0) ?: "Unknown Location",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// (HistoryList is unchanged)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    history: List<String>,
    onHistoryItemClicked: (String) -> Unit,
    onRemoveFromHistory: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .background(MaterialTheme.colorScheme.surface)
            // Add padding so the list doesn't touch the search card
            .padding(horizontal = 16.dp)
    ) {
        items(history, key = { it }) { query ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                        coroutineScope.launch { onRemoveFromHistory(query) }
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
                        // Remove horizontal padding from here
                        .padding(vertical = 2.dp)
                        .clickable { onHistoryItemClicked(query) }
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

// --- Modified Composable ---
// Added a Modifier parameter so it can be aligned
@Composable
private fun SelectedAddressCard(
    modifier: Modifier = Modifier, // Accept a modifier
    addressText: String,
    isVisible: Boolean
) {
    if (isVisible) {
        Card(
            // Apply the passed-in modifier here
            modifier = modifier
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
}