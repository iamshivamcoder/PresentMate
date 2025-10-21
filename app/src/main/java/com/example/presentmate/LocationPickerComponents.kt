package com.example.presentmate

import android.content.Context
import android.location.Address
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
internal fun LocationSearchBar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onPerformSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onGoToCurrentLocation: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search for a location") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
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
                    Icon(Icons.Default.MyLocation, contentDescription = "Go to my location")
                }
            }
        }
    }
}

internal fun createMapView(context: Context): MapView {
    return MapView(context).apply {
        setMultiTouchControls(true)
        setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
    }
}

@Composable
internal fun MapViewContainer(
    modifier: Modifier = Modifier,
    initialLocation: GeoPoint?,
    onMapMove: (GeoPoint) -> Unit,
    onMapMoveFinished: () -> Unit,
) {
    val context = LocalContext.current
    val mapView = remember {
        createMapView(context).apply {
            controller.setZoom(15.0)
            initialLocation?.let { controller.setCenter(it) }
        }
    }

    val mapListener = remember {
        object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                onMapMove(mapView.mapCenter as GeoPoint)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                onMapMove(mapView.mapCenter as GeoPoint)
                return true
            }
        }
    }
    
    LaunchedEffect(mapView) {
        mapView.addMapListener(mapListener)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
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
            mapView.removeMapListener(mapListener)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@Composable
internal fun CenterPin(isMapMoving: Boolean) {
    val elevation by animateDpAsState(if (isMapMoving) 8.dp else 2.dp, label = "pin_elevation")
    val size by animateDpAsState(if (isMapMoving) 40.dp else 32.dp, label = "pin_size")
    
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.GpsFixed,
            contentDescription = "Map Pin",
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation, CircleShape),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
internal fun ConfirmLocationButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Text("Confirm Location", modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
internal fun SearchOverlay(
    modifier: Modifier = Modifier,
    suggestions: List<Address>,
    history: List<String>,
    onSuggestionClicked: (Address) -> Unit,
    onHistoryItemClicked: (String) -> Unit,
    onRemoveFromHistory: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Saved Places Section
        Text("Saved Places", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SavedPlaceItem(label = "Home", onClick = { /* TODO */ })
            SavedPlaceItem(label = "Work", onClick = { /* TODO */ })
        }

        // History or Suggestions
        if (suggestions.isNotEmpty()) {
            SuggestionList(suggestions, onSuggestionClicked)
        } else {
            HistoryList(history, onHistoryItemClicked, onRemoveFromHistory)
        }
    }
}

@Composable
private fun SavedPlaceItem(label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(Icons.Outlined.Star, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<Address>,
    onSuggestionClicked: (Address) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { address ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryList(
    history: List<String>,
    onHistoryItemClicked: (String) -> Unit,
    onRemoveFromHistory: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    if (history.isNotEmpty()) {
        Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                else -> Color.Red
                            }, label = "background color"
                        )
                        val alignment = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.CenterEnd
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = alignment
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
}

@Composable
internal fun SelectedAddressCard(
    modifier: Modifier = Modifier,
    addressText: String,
    isVisible: Boolean
) {
    if (isVisible) {
        Card(
            modifier = modifier,
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
