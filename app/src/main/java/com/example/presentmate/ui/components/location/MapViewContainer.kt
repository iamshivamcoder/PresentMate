package com.example.presentmate.ui.components.location

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.presentmate.R
import kotlinx.coroutines.delay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val MAP_DEBOUNCE_DELAY = 300L
private const val MAP_ANIMATION_DELAY = 100L // New constant for animation delay

/**
 * A container for the osmdroid MapView.
 *
 * @param modifier The modifier to be applied to the MapView.
 * @param selectedLocation The currently selected location.
 * @param onMapMove Called when the map is moved.
 * @param onMapMoveFinished Called when the map movement is finished.
 * @param zoomLevel The zoom level to apply to the map.
 * @param mapMoveDebounce The debounce delay for map movements.
 */
@Composable
internal fun MapViewContainer(
    modifier: Modifier = Modifier,
    selectedLocation: GeoPoint?,
    onMapMove: (GeoPoint) -> Unit,
    onMapMoveFinished: () -> Unit,
    zoomLevel: Double = 15.0,
    mapMoveDebounce: Long = MAP_DEBOUNCE_DELAY
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnMapMove by rememberUpdatedState(onMapMove)
    val currentOnMapMoveFinished by rememberUpdatedState(onMapMoveFinished)

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setTileSource(TileSourceFactory.MAPNIK)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(zoomLevel)
            selectedLocation?.let { controller.setCenter(it) }
        }
    }

    LaunchedEffect(selectedLocation) {
        selectedLocation?.let {
            delay(MAP_ANIMATION_DELAY) // Added delay
            mapView.controller.animateTo(it)
        }
    }

    LaunchedEffect(zoomLevel) {
        mapView.controller.setZoom(zoomLevel)
    }

    var mapMoveCounter by remember { mutableStateOf(0) }
    LaunchedEffect(mapMoveCounter) {
        if (mapMoveCounter > 0) {
            delay(mapMoveDebounce)
            currentOnMapMoveFinished()
        }
    }

    val mapListener = remember {
        object : MapListener {
            private fun onMapMoved(): Boolean {
                val center = mapView.mapCenter as? GeoPoint ?: return false
                currentOnMapMove(center)
                mapMoveCounter++
                return true
            }

            override fun onScroll(event: ScrollEvent?): Boolean = onMapMoved()

            override fun onZoom(event: ZoomEvent?): Boolean = onMapMoved()
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.addMapListener(mapListener)

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
        modifier = modifier.semantics {
            contentDescription = context.getString(R.string.map_content_description)
        }
    )
}

@Composable
internal fun LocationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Location Access Needed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This app needs access to your device's location to provide accurate location-based features. Your location data is used only for selecting locations on the map and is not shared with third parties.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
