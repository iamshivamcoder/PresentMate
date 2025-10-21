package com.example.presentmate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class LocationPickerActivity : ComponentActivity() {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private lateinit var searchHistoryRepository: SearchHistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        searchHistoryRepository = SearchHistoryRepository(this)

        setContent {
            var initialLocation by remember { mutableStateOf<GeoPoint?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                getCurrentLocation {
                    initialLocation = it
                    isLoading = false
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LocationPickerScreen(
                    searchHistoryRepository = searchHistoryRepository,
                    initialLocation = initialLocation,
                    onLocationConfirmed = { geoPoint ->
                        val intent = Intent().apply {
                            putExtra("latitude", geoPoint.latitude)
                            putExtra("longitude", geoPoint.longitude)
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                )
            }
        }
    }

    private fun getCurrentLocation(onLocationFetched: (GeoPoint?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onLocationFetched(GeoPoint(it.latitude, it.longitude))
                } ?: onLocationFetched(null)
            }.addOnFailureListener {
                Log.e("LocationPicker", "Failed to get location", it)
                onLocationFetched(null)
            }
        } else {
            onLocationFetched(null)
        }
    }
}
