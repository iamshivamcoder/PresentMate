package com.example.presentmate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

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

        // Permissions are now handled in SettingsScreen before launching this activity.
        // We can directly try to get the current location.
        getCurrentLocation()

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
