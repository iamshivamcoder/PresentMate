package com.example.presentmate.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val MAP_DEBOUNCE_DELAY = 500L

private fun createMapView(context: Context): MapView {
    return MapView(context).apply {
        setMultiTouchControls(true)
        setTileSource(TileSourceFactory.MAPNIK)
        isTilesScaledToDpi = true
        minZoomLevel = 3.0
        maxZoomLevel = 19.0
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
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        createMapView(context).apply {
            controller.setZoom(15.0)
            initialLocation?.let { controller.setCenter(it) }
        }
    }

    var isMapMoving by remember { mutableStateOf(false) }

    LaunchedEffect(isMapMoving) {
        if (isMapMoving) {
            delay(MAP_DEBOUNCE_DELAY)
            isMapMoving = false
            onMapMoveFinished()
        }
    }

    val mapListener = remember {
        object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                val center = mapView.mapCenter as? GeoPoint ?: return false
                onMapMove(center)
                isMapMoving = true
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                val center = mapView.mapCenter as? GeoPoint ?: return false
                onMapMove(center)
                isMapMoving = true
                return true
            }
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
            contentDescription = "Interactive map for location selection"
        }
    )
}